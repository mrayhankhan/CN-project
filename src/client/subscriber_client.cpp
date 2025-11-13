#include "../common/serialization.hpp"
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <string>
#include <vector>

int main(int argc, char** argv) {
  const char* host = "127.0.0.1";
  int port = 9100;
  int sock = socket(AF_INET, SOCK_STREAM, 0);
  if (sock < 0) { std::cerr << "socket create failed\n"; return 1; }
  sockaddr_in serv{};
  serv.sin_family = AF_INET;
  serv.sin_port = htons(port);
  inet_pton(AF_INET, host, &serv.sin_addr);
  if (connect(sock, reinterpret_cast<sockaddr*>(&serv), sizeof(serv)) < 0) {
    std::cerr << "connect failed\n"; return 2;
  }
  // send subscribe request as framed json
  std::string req = "{\"client_id\":\"sub1\",\"mode\":\"delta\",\"symbol\":\"TEST\",\"since_ts\":0}";
  std::vector<uint8_t> payload;
  payload.push_back(0x10);
  payload.insert(payload.end(), req.begin(), req.end());
  auto frame = pack_frame(0x10, payload);
  send(sock, frame.data(), frame.size(), 0);
  // read responses line by line
  char buf[1024];
  while (true) {
    ssize_t r = recv(sock, buf, sizeof(buf) - 1, 0);
    if (r <= 0) break;
    buf[r] = 0;
    std::cout << std::string(buf) << std::flush;
  }
  close(sock);
  return 0;
}
