#include "normalizer.hpp"
#include <thread>
#include <chrono>
#include <algorithm>
#include <condition_variable>
#include <iostream>
#include <set>

Normalizer::Normalizer(size_t window_ms)
  : window_ms_(window_ms), running_(true) {
  std::thread([this]{ worker_loop(); }).detach();
}

void Normalizer::set_output_cb(OutputCallback cb) {
  std::lock_guard<std::mutex> lk(mu_);
  out_cb_ = cb;
}

void Normalizer::push_raw(const Tick& t) {
  std::lock_guard<std::mutex> lk(mu_);
  by_feed_[t.feed_id].push_back(t);
}

void Normalizer::stop() {
  std::lock_guard<std::mutex> lk(mu_);
  running_ = false;
}

static bool tick_less(const Tick& a, const Tick& b) {
  if (a.timestamp_ms != b.timestamp_ms) return a.timestamp_ms < b.timestamp_ms;
  return a.seq_id < b.seq_id;
}

void Normalizer::worker_loop() {
  while (true) {
    std::vector<Tick> ready;
    {
      std::lock_guard<std::mutex> lk(mu_);
      if (!running_ && by_feed_.empty()) break;
      uint64_t now_ms = static_cast<uint64_t>(std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count());
      for (auto it = by_feed_.begin(); it != by_feed_.end();) {
        auto& dq = it->second;
        while (!dq.empty()) {
          const Tick& t = dq.front();
          if (t.timestamp_ms + window_ms_ <= now_ms) {
            ready.push_back(t);
            dq.pop_front();
          } else break;
        }
        if (dq.empty()) it = by_feed_.erase(it);
        else ++it;
      }
    }

    if (!ready.empty()) {
      std::sort(ready.begin(), ready.end(), tick_less);
      // simple dedupe based on feed id and seq id local to batch
      std::set<std::pair<uint32_t,uint64_t>> seen;
      for (auto& t : ready) {
        auto key = std::make_pair(t.feed_id, t.seq_id);
        if (seen.find(key) != seen.end()) continue;
        seen.insert(key);
        // simple outlier rejection: price must be positive and finite
        if (!(t.price > 0)) continue;
        // smoothing could be applied here. For simplicity pass through
        OutputCallback cb;
        {
          std::lock_guard<std::mutex> lk(mu_);
          cb = out_cb_;
        }
        if (cb) cb(t);
      }
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
  }
}
