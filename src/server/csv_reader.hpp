#pragma once
#include "../common/types.hpp"
#include <string>
#include <vector>
#include <fstream>

class CsvReader {
public:
  explicit CsvReader(const std::string& filepath);
  ~CsvReader();
  bool read_next(Tick& tick);
  void reset();
  bool is_open() const;
private:
  std::ifstream file_;
  bool header_read_;
};
