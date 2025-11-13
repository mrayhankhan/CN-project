#include "broadcaster.hpp"
#include "../common/serialization.hpp"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <sstream>

Broadcaster::Broadcaster(int port)
  : port_(port), listen_fd_(-1), running_(false) {}

Broadcaster::~Broadcaster() {
  stop();
}

void Broadcaster::start() {
  listen_fd_ = socket(AF_INET, SOCK_STREAM, 0);
  if (listen_fd_ < 0) {
    std::cerr << "listen socket create failed\n";
    return;
  }
  int opt = 1;
  setsockopt(listen_fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(port_);
  if (bind(listen_fd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
    std::cerr << "bind failed\n";
    close(listen_fd_);
    listen_fd_ = -1;
    return;
  }
  if (listen(listen_fd_, 10) < 0) {
    std::cerr << "listen failed\n";
    close(listen_fd_);
    listen_fd_ = -1;
    return;
  }
  running_ = true;
  listener_thread_ = std::thread(&Broadcaster::listener_loop, this);
}

void Broadcaster::stop() {
  running_ = false;
  if (listen_fd_ >= 0) close(listen_fd_);
  if (listener_thread_.joinable()) listener_thread_.join();
  std::lock_guard<std::mutex> lk(mu_);
  for (auto& p : clients_) close(p.first);
  clients_.clear();
}

void Broadcaster::listener_loop() {
  while (running_) {
    sockaddr_in cli{}; socklen_t len = sizeof(cli);
    int fd = accept(listen_fd_, reinterpret_cast<sockaddr*>(&cli), &len);
    if (fd < 0) {
      if (!running_) break;
      std::cerr << "accept failed\n";
      continue;
    }
    // simple client registration
    {
      std::lock_guard<std::mutex> lk(mu_);
      clients_[fd] = "unknown";
    }
    // spawn per client reader to consume subscribe request and then run writer thread
    std::thread([this,fd]{
      // read frame length
      uint8_t lenbuf[4];
      if (!read_n_from_fd(fd, lenbuf, 4)) { close(fd); return; }
      uint32_t len = ntohl(*reinterpret_cast<uint32_t*>(lenbuf));
      std::vector<uint8_t> payload(len);
      if (!read_n_from_fd(fd, payload.data(), len)) { close(fd); return; }
      uint8_t type = payload[0];
      // ignore content for now
      // send ack snapshot
      std::string welcome = "{\"type\":\"snapshot\",\"note\":\"welcome\"}\n";
      send(fd, welcome.c_str(), static_cast<size_t>(welcome.size()), 0);
      // keep socket open for writes
      // writer loop will write updates when push_normalized is called
      // simple approach: nothing more in reader
      // block to keep thread alive
      while (true) {
        char tmp;
        ssize_t r = recv(fd, &tmp, 1, MSG_PEEK | MSG_DONTWAIT);
        if (r == 0) break;
        if (r < 0) {
          // no data available
          std::this_thread::sleep_for(std::chrono::milliseconds(100));
          continue;
        }
        // if client sends something break
        break;
      }
      // cleanup
      std::lock_guard<std::mutex> lk(mu_);
      if (clients_.find(fd) != clients_.end()) {
        close(fd);
        clients_.erase(fd);
      }
    }).detach();
  }
}

void Broadcaster::push_normalized(const Tick& t) {
  std::lock_guard<std::mutex> lk(mu_);
  std::ostringstream oss;
  oss << "{\"type\":\"delta\",\"tick\":{\"timestamp_ms\":" << t.timestamp_ms
      << ",\"feed_id\":" << t.feed_id
      << ",\"seq_id\":" << t.seq_id
      << ",\"price\":" << t.price
      << ",\"size\":" << t.size
      << ",\"flags\":" << static_cast<int>(t.flags)
      << "}}\n";
  std::string s = oss.str();
  for (auto it = clients_.begin(); it != clients_.end();) {
    int fd = it->first;
    ssize_t sent = send(fd, s.c_str(), s.size(), 0);
    if (sent < 0) {
      close(fd);
      it = clients_.erase(it);
    } else ++it;
  }
}

int Broadcaster::subscriber_count() {
  std::lock_guard<std::mutex> lk(mu_);
  return static_cast<int>(clients_.size());
}
