#include "../src/server/token_bucket.hpp"
#include <cassert>
#include <iostream>
#include <thread>
#include <chrono>

void test_rate_limiting() {
  TokenBucket bucket(10, 5);
  
  // Consume all tokens
  for (int i = 0; i < 10; i++) {
    assert(bucket.try_consume(1));
  }
  
  // Should fail
  assert(!bucket.try_consume(1));
  
  // Wait for refill
  std::this_thread::sleep_for(std::chrono::milliseconds(2000));
  
  // Should succeed after refill
  assert(bucket.try_consume(1));
  
  std::cout << "Token bucket test passed\n";
}

int main() {
  test_rate_limiting();
  std::cout << "All token bucket tests passed!\n";
  return 0;
}
