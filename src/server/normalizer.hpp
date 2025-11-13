#pragma once
#include "../common/types.hpp"
#include <vector>
#include <mutex>
#include <map>
#include <deque>
#include <functional>

class Normalizer {
public:
  using OutputCallback = std::function<void(const Tick&)>;
  Normalizer(size_t window_ms = 200);
  void push_raw(const Tick& t);
  void set_output_cb(OutputCallback cb);
  void stop();
private:
  void worker_loop();
  size_t window_ms_;
  std::mutex mu_;
  std::map<uint64_t, std::deque<Tick>> by_feed_;
  bool running_;
  OutputCallback out_cb_;
};
