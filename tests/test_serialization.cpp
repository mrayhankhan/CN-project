#include "../src/common/serialization.hpp"
#include <iostream>
#include <cassert>

int main() {
  Tick t;
  t.timestamp_ms = 1630000000000ULL;
  t.feed_id = 5;
  t.seq_id = 42;
  t.price = 123.456;
  t.size = 1000;
  t.flags = 0;
  auto p = pack_tick(t);
  Tick out;
  bool ok = unpack_tick(p, out);
  assert(ok);
  assert(out.timestamp_ms == t.timestamp_ms);
  assert(out.feed_id == t.feed_id);
  assert(out.seq_id == t.seq_id);
  assert(out.price == t.price);
  assert(out.size == t.size);
  std::cout << "serialization test ok\n";
  return 0;
}
