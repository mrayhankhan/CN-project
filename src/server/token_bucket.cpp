#include "token_bucket.hpp"
#include <algorithm>

TokenBucket::TokenBucket(size_t capacity, size_t refill_rate)
  : capacity_(capacity), refill_rate_(refill_rate), tokens_(capacity),
    last_refill_(std::chrono::steady_clock::now()) {}

void TokenBucket::refill() {
  auto now = std::chrono::steady_clock::now();
  auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_refill_).count();
  double refill_amount = (refill_rate_ * elapsed) / 1000.0;
  tokens_ = std::min(static_cast<double>(capacity_), tokens_ + refill_amount);
  last_refill_ = now;
}

bool TokenBucket::try_consume(size_t tokens) {
  std::lock_guard<std::mutex> lk(mu_);
  refill();
  if (tokens_ >= tokens) {
    tokens_ -= tokens;
    return true;
  }
  return false;
}
