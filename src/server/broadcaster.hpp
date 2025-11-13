#pragma once
#include "../common/types.hpp"
#include "token_bucket.hpp"
#include <vector>
#include <mutex>
#include <thread>
#include <map>
#include <memory>

class Broadcaster {
public:
  Broadcaster(int port, size_t rate_limit = 100, size_t burst_size = 200);
  ~Broadcaster();
  void start();
  void stop();
  void push_normalized(const Tick& t);
  int subscriber_count();
private:
  void listener_loop();
  int port_;
  int listen_fd_;
  size_t rate_limit_;
  size_t burst_size_;
  std::mutex mu_;
  std::map<int,std::string> clients_;
  std::map<int,std::shared_ptr<TokenBucket>> rate_limiters_;
  bool running_;
  std::thread listener_thread_;
};
