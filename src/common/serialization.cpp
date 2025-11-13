#include "serialization.hpp"
#include <cstring>
#include <arpa/inet.h>
#include <unistd.h>

uint64_t htonll_u64(uint64_t v) {
  static const int num = 42;
  if (*(const char*)&num == 42) {
    uint32_t hi = htonl(static_cast<uint32_t>(v >> 32));
    uint32_t lo = htonl(static_cast<uint32_t>(v & 0xffffffffu));
    return (static_cast<uint64_t>(lo) << 32) | hi;
  } else {
    return v;
  }
}

uint64_t ntohll_u64(uint64_t v) {
  return htonll_u64(v);
}

std::vector<uint8_t> pack_tick(const Tick& t) {
  std::vector<uint8_t> buf;
  buf.reserve(8 + 4 + 8 + 8 + 8 + 1);
  uint64_t ts = htonll_u64(t.timestamp_ms);
  buf.insert(buf.end(), reinterpret_cast<uint8_t*>(&ts), reinterpret_cast<uint8_t*>(&ts) + 8);
  uint32_t fid = htonl(t.feed_id);
  buf.insert(buf.end(), reinterpret_cast<uint8_t*>(&fid), reinterpret_cast<uint8_t*>(&fid) + 4);
  uint64_t sid = htonll_u64(t.seq_id);
  buf.insert(buf.end(), reinterpret_cast<uint8_t*>(&sid), reinterpret_cast<uint8_t*>(&sid) + 8);
  uint64_t p;
  static_assert(sizeof(p) == sizeof(t.price));
  std::memcpy(&p, &t.price, sizeof(p));
  p = htonll_u64(p);
  buf.insert(buf.end(), reinterpret_cast<uint8_t*>(&p), reinterpret_cast<uint8_t*>(&p) + 8);
  uint64_t sz = htonll_u64(t.size);
  buf.insert(buf.end(), reinterpret_cast<uint8_t*>(&sz), reinterpret_cast<uint8_t*>(&sz) + 8);
  buf.push_back(t.flags);
  return buf;
}

bool unpack_tick(const std::vector<uint8_t>& buf, Tick& out) {
  size_t need = 8 + 4 + 8 + 8 + 8 + 1;
  if (buf.size() < need) return false;
  size_t idx = 0;
  uint64_t ts;
  std::memcpy(&ts, buf.data() + idx, 8);
  out.timestamp_ms = ntohll_u64(ts);
  idx += 8;
  uint32_t fid;
  std::memcpy(&fid, buf.data() + idx, 4);
  out.feed_id = ntohl(fid);
  idx += 4;
  uint64_t sid;
  std::memcpy(&sid, buf.data() + idx, 8);
  out.seq_id = ntohll_u64(sid);
  idx += 8;
  uint64_t p;
  std::memcpy(&p, buf.data() + idx, 8);
  p = ntohll_u64(p);
  std::memcpy(&out.price, &p, sizeof(p));
  idx += 8;
  uint64_t sz;
  std::memcpy(&sz, buf.data() + idx, 8);
  out.size = ntohll_u64(sz);
  idx += 8;
  out.flags = buf[idx];
  return true;
}

std::vector<uint8_t> pack_frame(uint8_t type, const std::vector<uint8_t>& payload) {
  uint32_t payload_len = static_cast<uint32_t>(payload.size() + 1);
  uint32_t len_be = htonl(payload_len);
  std::vector<uint8_t> frame;
  frame.reserve(4 + payload_len);
  frame.insert(frame.end(), reinterpret_cast<uint8_t*>(&len_be), reinterpret_cast<uint8_t*>(&len_be) + 4);
  frame.push_back(type);
  frame.insert(frame.end(), payload.begin(), payload.end());
  return frame;
}

bool read_n_from_fd(int fd, uint8_t* buf, size_t n) {
  size_t got = 0;
  while (got < n) {
    ssize_t r = read(fd, buf + got, n - got);
    if (r <= 0) return false;
    got += static_cast<size_t>(r);
  }
  return true;
}
