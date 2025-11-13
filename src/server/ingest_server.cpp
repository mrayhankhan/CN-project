#include "../common/serialization.hpp"
#include "normalizer.hpp"
#include "broadcaster.hpp"
#include "persistence.hpp"
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <iostream>
#include <thread>
#include <sstream>
#include <atomic>
#include <functional>

extern int start_admin_http(int port, std::function<std::string()> status_cb);

int main(int argc, char** argv) {
  const int feed_port = 9000;
  const int sub_port = 9100;
  const int admin_port = 9200;

  Broadcaster broadcaster(sub_port, 100, 200);
  broadcaster.start();

  Persistence persist("normalized_log.csv");

  Normalizer normalizer(200, 5);
  normalizer.set_output_cb([&](const Tick& t){
    broadcaster.push_normalized(t);
    persist.append(t);
  });

  // admin http
  auto status_cb = [&]()->std::string {
    std::ostringstream ss;
    ss << "{\"feeds\":0,\"subscribers\":" << broadcaster.subscriber_count() << "}";
    return ss.str();
  };
  int admin_sock = start_admin_http(admin_port, status_cb);
  if (admin_sock < 0) {
    std::cerr << "admin http start failed\n";
  } else {
    std::cout << "admin http running on port " << admin_port << "\n";
  }

  int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
  if (listen_fd < 0) { std::cerr << "socket create failed\n"; return 1; }
  int opt = 1;
  setsockopt(listen_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(feed_port);
  if (bind(listen_fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
    std::cerr << "bind failed\n"; return 2;
  }
  if (listen(listen_fd, 10) < 0) { std::cerr << "listen failed\n"; return 3; }
  std::cout << "feed ingest listening on port " << feed_port << "\n";

  std::atomic<bool> running(true);

  while (running) {
    sockaddr_in cli{}; socklen_t len = sizeof(cli);
    int fd = accept(listen_fd, reinterpret_cast<sockaddr*>(&cli), &len);
    if (fd < 0) continue;
    std::cout << "feed connected\n";
    // per feed handler
    std::thread([fd, &normalizer]{
      while (true) {
        uint8_t lenbuf[4];
        if (!read_n_from_fd(fd, lenbuf, 4)) break;
        uint32_t plen = ntohl(*reinterpret_cast<uint32_t*>(lenbuf));
        if (plen == 0) break;
        std::vector<uint8_t> payload(plen);
        if (!read_n_from_fd(fd, payload.data(), plen)) break;
        uint8_t type = payload[0];
        if (type == 0x01) {
          std::vector<uint8_t> tickbuf(payload.begin() + 1, payload.end());
          Tick t;
          if (unpack_tick(tickbuf, t)) {
            normalizer.push_raw(t);
          }
        }
      }
      close(fd);
      std::cout << "feed disconnected\n";
    }).detach();
  }

  close(listen_fd);
  return 0;
}
