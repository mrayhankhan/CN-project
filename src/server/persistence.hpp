#pragma once
#include "../common/types.hpp"
#include <string>
#include <mutex>

class Persistence {
public:
  Persistence(const std::string& fname);
  ~Persistence();
  void append(const Tick& t);
private:
  std::string fname_;
  std::mutex mu_;
  FILE* f_;
};
