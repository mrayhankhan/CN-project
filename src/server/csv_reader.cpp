#include "csv_reader.hpp"
#include <fstream>
#include <sstream>
#include <iostream>

CsvReader::CsvReader(const std::string& filepath) : header_read_(false) {
  file_.open(filepath);
  if (!file_.is_open()) {
    std::cerr << "Failed to open CSV file: " << filepath << "\n";
  }
}

CsvReader::~CsvReader() {
  if (file_.is_open()) {
    file_.close();
  }
}

bool CsvReader::is_open() const {
  return file_.is_open();
}

void CsvReader::reset() {
  file_.clear();
  file_.seekg(0);
  header_read_ = false;
}

bool CsvReader::read_next(Tick& tick) {
  if (!file_.is_open()) return false;
  
  // Skip header line on first read
  if (!header_read_) {
    std::string header;
    if (!std::getline(file_, header)) return false;
    header_read_ = true;
  }
  
  std::string line;
  if (!std::getline(file_, line)) return false;
  if (line.empty()) return false;
  
  // Parse CSV: timestamp_ms,feed_id,seq_id,price,size,flags
  std::stringstream ss(line);
  std::string token;
  
  try {
    // timestamp_ms
    if (!std::getline(ss, token, ',')) return false;
    tick.timestamp_ms = std::stoull(token);
    
    // feed_id
    if (!std::getline(ss, token, ',')) return false;
    tick.feed_id = std::stoul(token);
    
    // seq_id
    if (!std::getline(ss, token, ',')) return false;
    tick.seq_id = std::stoull(token);
    
    // price
    if (!std::getline(ss, token, ',')) return false;
    tick.price = std::stod(token);
    
    // size
    if (!std::getline(ss, token, ',')) return false;
    tick.size = std::stoull(token);
    
    // flags
    if (!std::getline(ss, token, ',')) return false;
    tick.flags = static_cast<uint8_t>(std::stoi(token));
    
    return true;
  } catch (const std::exception& e) {
    std::cerr << "CSV parse error: " << e.what() << "\n";
    return false;
  }
}
