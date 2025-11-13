#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <mutex>
#include <thread>
#include <cstring>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sstream>
#include <cstdint>
#include <openssl/sha.h>

// Minimal WebSocket server that bridges TCP JSON stream to WebSocket clients
// Receives JSON deltas from broadcaster, forwards to browsers via WebSocket

const std::string WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

std::string base64_encode(const unsigned char* bytes, size_t len) {
  const char* b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  std::string result;
  for (size_t i = 0; i < len; i += 3) {
    uint32_t val = (bytes[i] << 16) | ((i + 1 < len ? bytes[i + 1] : 0) << 8) | (i + 2 < len ? bytes[i + 2] : 0);
    result += b64[(val >> 18) & 0x3F];
    result += b64[(val >> 12) & 0x3F];
    result += (i + 1 < len ? b64[(val >> 6) & 0x3F] : '=');
    result += (i + 2 < len ? b64[val & 0x3F] : '=');
  }
  return result;
}

bool websocket_handshake(int fd) {
  char buf[4096];
  ssize_t n = recv(fd, buf, sizeof(buf) - 1, 0);
  if (n <= 0) return false;
  buf[n] = '\0';
  
  std::string request(buf);
  size_t key_pos = request.find("Sec-WebSocket-Key: ");
  if (key_pos == std::string::npos) return false;
  
  size_t key_start = key_pos + 19;
  size_t key_end = request.find("\r\n", key_start);
  if (key_end == std::string::npos) return false;
  
  std::string client_key = request.substr(key_start, key_end - key_start);
  std::string accept_str = client_key + WS_MAGIC;
  
  unsigned char hash[SHA_DIGEST_LENGTH];
  SHA1(reinterpret_cast<const unsigned char*>(accept_str.c_str()), accept_str.size(), hash);
  std::string accept_key = base64_encode(hash, SHA_DIGEST_LENGTH);
  
  std::ostringstream response;
  response << "HTTP/1.1 101 Switching Protocols\r\n";
  response << "Upgrade: websocket\r\n";
  response << "Connection: Upgrade\r\n";
  response << "Sec-WebSocket-Accept: " << accept_key << "\r\n\r\n";
  
  std::string resp = response.str();
  return send(fd, resp.c_str(), resp.size(), 0) > 0;
}

void send_websocket_text(int fd, const std::string& msg) {
  size_t len = msg.size();
  std::vector<uint8_t> frame;
  frame.push_back(0x81); // FIN + text frame
  
  if (len < 126) {
    frame.push_back(static_cast<uint8_t>(len));
  } else if (len < 65536) {
    frame.push_back(126);
    frame.push_back(static_cast<uint8_t>((len >> 8) & 0xFF));
    frame.push_back(static_cast<uint8_t>(len & 0xFF));
  } else {
    frame.push_back(127);
    for (int i = 7; i >= 0; --i) {
      frame.push_back(static_cast<uint8_t>((len >> (i * 8)) & 0xFF));
    }
  }
  
  frame.insert(frame.end(), msg.begin(), msg.end());
  send(fd, frame.data(), frame.size(), MSG_DONTWAIT);
}

class WebSocketBridge {
public:
  WebSocketBridge(int ws_port, const std::string& upstream_host, int upstream_port)
    : ws_port_(ws_port), upstream_host_(upstream_host), upstream_port_(upstream_port),
      ws_listen_fd_(-1), upstream_fd_(-1), running_(false) {}
  
  ~WebSocketBridge() { stop(); }
  
  void start() {
    // Start WebSocket listener
    ws_listen_fd_ = socket(AF_INET, SOCK_STREAM, 0);
    if (ws_listen_fd_ < 0) {
      std::cerr << "WebSocket socket create failed\n";
      return;
    }
    int opt = 1;
    setsockopt(ws_listen_fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(ws_port_);
    
    if (bind(ws_listen_fd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
      std::cerr << "WebSocket bind failed\n";
      close(ws_listen_fd_);
      return;
    }
    
    if (listen(ws_listen_fd_, 10) < 0) {
      std::cerr << "WebSocket listen failed\n";
      close(ws_listen_fd_);
      return;
    }
    
    running_ = true;
    std::cout << "WebSocket server listening on port " << ws_port_ << std::endl;
    
    ws_listener_thread_ = std::thread(&WebSocketBridge::ws_listener_loop, this);
    upstream_thread_ = std::thread(&WebSocketBridge::upstream_reader_loop, this);
  }
  
  void stop() {
    running_ = false;
    if (ws_listen_fd_ >= 0) close(ws_listen_fd_);
    if (upstream_fd_ >= 0) close(upstream_fd_);
    if (ws_listener_thread_.joinable()) ws_listener_thread_.join();
    if (upstream_thread_.joinable()) upstream_thread_.join();
    
    std::lock_guard<std::mutex> lk(mu_);
    for (auto& p : ws_clients_) close(p.first);
    ws_clients_.clear();
  }
  
private:
  void ws_listener_loop() {
    while (running_) {
      sockaddr_in cli{};
      socklen_t len = sizeof(cli);
      int fd = accept(ws_listen_fd_, reinterpret_cast<sockaddr*>(&cli), &len);
      if (fd < 0) {
        if (!running_) break;
        continue;
      }
      
      if (!websocket_handshake(fd)) {
        close(fd);
        continue;
      }
      
      {
        std::lock_guard<std::mutex> lk(mu_);
        ws_clients_[fd] = true;
        std::cout << "WebSocket client connected, total: " << ws_clients_.size() << std::endl;
      }
      
      // Monitor for disconnect
      std::thread([this, fd]{
        uint8_t buf[1024];
        while (running_) {
          ssize_t n = recv(fd, buf, sizeof(buf), 0);
          if (n <= 0) break;
          // Handle ping/pong if needed
        }
        std::lock_guard<std::mutex> lk(mu_);
        if (ws_clients_.find(fd) != ws_clients_.end()) {
          close(fd);
          ws_clients_.erase(fd);
          std::cout << "WebSocket client disconnected, remaining: " << ws_clients_.size() << std::endl;
        }
      }).detach();
    }
  }
  
  void connect_upstream() {
    while (running_) {
      upstream_fd_ = socket(AF_INET, SOCK_STREAM, 0);
      if (upstream_fd_ < 0) {
        std::cerr << "Upstream socket create failed\n";
        std::this_thread::sleep_for(std::chrono::seconds(2));
        continue;
      }
      
      sockaddr_in addr{};
      addr.sin_family = AF_INET;
      addr.sin_port = htons(upstream_port_);
      inet_pton(AF_INET, upstream_host_.c_str(), &addr.sin_addr);
      
      if (connect(upstream_fd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
        std::cerr << "Connect to upstream " << upstream_host_ << ":" << upstream_port_ << " failed, retrying...\n";
        close(upstream_fd_);
        upstream_fd_ = -1;
        std::this_thread::sleep_for(std::chrono::seconds(2));
        continue;
      }
      
      std::cout << "Connected to upstream broadcaster at " << upstream_host_ << ":" << upstream_port_ << std::endl;
      
      // Send subscription handshake (4-byte length + payload)
      uint8_t handshake[5] = {0, 0, 0, 1, 0};
      send(upstream_fd_, handshake, sizeof(handshake), 0);
      break;
    }
  }
  
  void upstream_reader_loop() {
    std::string buffer;
    
    while (running_) {
      if (upstream_fd_ < 0) {
        connect_upstream();
        if (upstream_fd_ < 0) continue;
      }
      
      char buf[4096];
      ssize_t n = recv(upstream_fd_, buf, sizeof(buf), 0);
      
      if (n <= 0) {
        std::cerr << "Upstream connection lost, reconnecting...\n";
        close(upstream_fd_);
        upstream_fd_ = -1;
        std::this_thread::sleep_for(std::chrono::seconds(2));
        continue;
      }
      
      buffer.append(buf, n);
      
      // Process line-delimited JSON messages
      size_t pos;
      while ((pos = buffer.find('\n')) != std::string::npos) {
        std::string msg = buffer.substr(0, pos);
        buffer.erase(0, pos + 1);
        
        if (!msg.empty()) {
          broadcast_to_clients(msg);
        }
      }
    }
  }
  
  void broadcast_to_clients(const std::string& msg) {
    std::lock_guard<std::mutex> lk(mu_);
    
    // Keep small buffer to avoid memory issues
    if (message_buffer_.size() > 100) {
      message_buffer_.erase(message_buffer_.begin());
    }
    message_buffer_.push_back(msg);
    
    for (auto it = ws_clients_.begin(); it != ws_clients_.end();) {
      int fd = it->first;
      send_websocket_text(fd, msg);
      ++it;
    }
  }
  
  int ws_port_;
  std::string upstream_host_;
  int upstream_port_;
  int ws_listen_fd_;
  int upstream_fd_;
  bool running_;
  
  std::mutex mu_;
  std::map<int, bool> ws_clients_;
  std::vector<std::string> message_buffer_;
  
  std::thread ws_listener_thread_;
  std::thread upstream_thread_;
};

int main(int argc, char* argv[]) {
  int ws_port = 9200;
  std::string upstream_host = "127.0.0.1";
  int upstream_port = 9100;
  
  for (int i = 1; i < argc; ++i) {
    std::string arg = argv[i];
    if (arg == "--ws-port" && i + 1 < argc) {
      ws_port = std::stoi(argv[++i]);
    } else if (arg == "--upstream-host" && i + 1 < argc) {
      upstream_host = argv[++i];
    } else if (arg == "--upstream-port" && i + 1 < argc) {
      upstream_port = std::stoi(argv[++i]);
    } else if (arg == "--help") {
      std::cout << "Usage: " << argv[0] << " [options]\n";
      std::cout << "  --ws-port <port>         WebSocket listening port (default: 9200)\n";
      std::cout << "  --upstream-host <host>   Broadcaster host (default: 127.0.0.1)\n";
      std::cout << "  --upstream-port <port>   Broadcaster port (default: 9100)\n";
      return 0;
    }
  }
  
  std::cout << "Starting WebSocket Bridge...\n";
  std::cout << "WebSocket port: " << ws_port << "\n";
  std::cout << "Upstream: " << upstream_host << ":" << upstream_port << "\n";
  
  WebSocketBridge bridge(ws_port, upstream_host, upstream_port);
  bridge.start();
  
  std::cout << "Press Ctrl+C to stop\n";
  while (true) {
    std::this_thread::sleep_for(std::chrono::seconds(1));
  }
  
  return 0;
}
