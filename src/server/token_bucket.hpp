#pragma once
#include <chrono>
#include <mutex>

class TokenBucket {
public:
  TokenBucket(size_t capacity, size_t refill_rate);
  bool try_consume(size_t tokens = 1);
  void refill();
private:
  size_t capacity_;
  size_t refill_rate_;
  double tokens_;
  std::chrono::steady_clock::time_point last_refill_;
  std::mutex mu_;
};
