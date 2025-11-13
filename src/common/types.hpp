#pragma once
#include <cstdint>
#include <string>

struct Tick {
  uint64_t timestamp_ms;
  uint32_t feed_id;
  uint64_t seq_id;
  double price;
  uint64_t size;
  uint8_t flags;
};
