#include "../src/server/normalizer.hpp"
#include <cassert>
#include <iostream>
#include <vector>
#include <thread>
#include <chrono>

void test_moving_average() {
  std::vector<Tick> output;
  Normalizer norm(200, 3);
  
  norm.set_output_cb([&output](const Tick& t) {
    output.push_back(t);
  });
  
  for (int i = 0; i < 5; i++) {
    Tick t{};
    t.timestamp_ms = 1000 + i * 50;
    t.feed_id = 1;
    t.seq_id = i;
    t.price = 100.0 + i;
    t.size = 100;
    t.flags = 0;
    norm.push_raw(t);
  }
  
  std::this_thread::sleep_for(std::chrono::milliseconds(500));
  norm.stop();
  
  assert(output.size() > 0);
  std::cout << "Smoothing test passed: " << output.size() << " ticks processed\n";
}

int main() {
  test_moving_average();
  std::cout << "All smoothing tests passed!\n";
  return 0;
}
