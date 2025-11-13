#include "../src/server/csv_reader.hpp"
#include <cassert>
#include <fstream>
#include <iostream>

void create_test_csv(const std::string& path) {
  std::ofstream f(path);
  f << "timestamp_ms,feed_id,seq_id,price,size,flags\n";
  f << "1000,1,100,99.5,1000,0\n";
  f << "1100,1,101,99.6,1500,0\n";
  f << "1200,2,200,99.7,2000,0\n";
  f.close();
}

int main() {
  std::cout << "Testing CSV reader...\n";
  
  const std::string test_file = "/tmp/test_replay.csv";
  create_test_csv(test_file);
  
  CsvReader reader(test_file);
  assert(reader.is_open());
  
  Tick t1, t2, t3;
  
  // Read first tick
  assert(reader.read_next(t1));
  assert(t1.timestamp_ms == 1000);
  assert(t1.feed_id == 1);
  assert(t1.seq_id == 100);
  assert(t1.price == 99.5);
  assert(t1.size == 1000);
  
  // Read second tick
  assert(reader.read_next(t2));
  assert(t2.timestamp_ms == 1100);
  assert(t2.price == 99.6);
  
  // Read third tick
  assert(reader.read_next(t3));
  assert(t3.feed_id == 2);
  assert(t3.seq_id == 200);
  
  // No more ticks
  Tick t4;
  assert(!reader.read_next(t4));
  
  // Test reset
  reader.reset();
  assert(reader.read_next(t1));
  assert(t1.timestamp_ms == 1000);
  
  std::cout << "All CSV reader tests passed!\n";
  return 0;
}
