#pragma once
#include "../common/types.hpp"
#include <vector>
#include <mutex>
#include <thread>
#include <map>

class Broadcaster {
public:
  Broadcaster(int port);
  ~Broadcaster();
  void start();
  void stop();
  void push_normalized(const Tick& t);
  int subscriber_count();
private:
  void listener_loop();
  int port_;
  int listen_fd_;
  std::mutex mu_;
  std::map<int,std::string> clients_;
  bool running_;
  std::thread listener_thread_;
};
