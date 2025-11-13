#pragma once
#include "types.hpp"
#include <vector>
#include <cstdint>

std::vector<uint8_t> pack_tick(const Tick& t);
bool unpack_tick(const std::vector<uint8_t>& buf, Tick& out);
std::vector<uint8_t> pack_frame(uint8_t type, const std::vector<uint8_t>& payload);
bool read_n_from_fd(int fd, uint8_t* buf, size_t n);
uint64_t htonll_u64(uint64_t v);
uint64_t ntohll_u64(uint64_t v);
