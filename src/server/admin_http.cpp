#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <iostream>
#include <sstream>
#include <thread>
#include <functional>

int start_admin_http(int port, std::function<std::string()> status_cb) {
  int sock = socket(AF_INET, SOCK_STREAM, 0);
  if (sock < 0) return -1;
  int opt = 1;
  setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(port);
  if (bind(sock, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
    close(sock); return -1;
  }
  if (listen(sock, 5) < 0) { close(sock); return -1; }
  std::thread([sock,status_cb]{
    while (true) {
      sockaddr_in cli{}; socklen_t len = sizeof(cli);
      int fd = accept(sock, reinterpret_cast<sockaddr*>(&cli), &len);
      if (fd < 0) break;
      std::string body = status_cb();
      std::ostringstream resp;
      resp << "HTTP/1.1 200 OK\r\n"
           << "Content-Type: application/json\r\n"
           << "Content-Length: " << body.size() << "\r\n"
           << "Connection: close\r\n\r\n"
           << body;
      std::string r = resp.str();
      send(fd, r.c_str(), r.size(), 0);
      close(fd);
    }
  }).detach();
  return sock;
}
