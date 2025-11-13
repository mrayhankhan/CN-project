#include "../common/serialization.hpp"
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <vector>
#include <random>

int main(int argc, char** argv) {
  const char* host = "127.0.0.1";
  int port = 9000;
  int feed_id = 1;
  if (argc > 1) feed_id = std::stoi(argv[1]);

  int sock = socket(AF_INET, SOCK_STREAM, 0);
  if (sock < 0) {
    std::cerr << "socket create failed\n"; return 1;
  }
  sockaddr_in serv{};
  serv.sin_family = AF_INET;
  serv.sin_port = htons(port);
  inet_pton(AF_INET, host, &serv.sin_addr);
  if (connect(sock, reinterpret_cast<sockaddr*>(&serv), sizeof(serv)) < 0) {
    std::cerr << "connect failed\n"; return 2;
  }
  std::mt19937 rng(static_cast<unsigned int>(std::chrono::system_clock::now().time_since_epoch().count()));
  std::uniform_int_distribution<int> jitter(0, 40);
  double base = 100.0 + feed_id;
  uint64_t seq = 1;
  for (int i = 0; i < 5000; ++i) {
    Tick t;
    t.timestamp_ms = static_cast<uint64_t>(
      std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count()
    );
    t.feed_id = static_cast<uint32_t>(feed_id);
    t.seq_id = seq++;
    t.price = base + (i % 20) * 0.01;
    t.size = 100;
    t.flags = 0;
    auto payload = pack_tick(t);
    auto frame = pack_frame(0x01, payload);
    // inject jitter and duplicates randomly
    int j = jitter(rng);
    std::this_thread::sleep_for(std::chrono::milliseconds(10 + j));
    ssize_t sent = send(sock, frame.data(), frame.size(), 0);
    if (sent < 0) break;
    // occasionally send duplicate
    if ((i % 50) == 0) {
      send(sock, frame.data(), frame.size(), 0);
    }
  }
  close(sock);
  return 0;
}
