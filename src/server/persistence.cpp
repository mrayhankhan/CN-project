#include "persistence.hpp"
#include <cstdio>
#include <ctime>
#include <iostream>

Persistence::Persistence(const std::string& fname)
  : fname_(fname), f_(nullptr) {
  f_ = std::fopen(fname_.c_str(), "a");
  if (!f_) {
    std::cerr << "persistence open failed\n";
  }
}

Persistence::~Persistence() {
  if (f_) std::fclose(f_);
}

void Persistence::append(const Tick& t) {
  if (!f_) return;
  std::lock_guard<std::mutex> lk(mu_);
  std::fprintf(f_, "%llu,%u,%llu,%.6f,%llu,%u\n",
    static_cast<unsigned long long>(t.timestamp_ms),
    static_cast<unsigned int>(t.feed_id),
    static_cast<unsigned long long>(t.seq_id),
    t.price,
    static_cast<unsigned long long>(t.size),
    static_cast<unsigned int>(t.flags));
  std::fflush(f_);
}
