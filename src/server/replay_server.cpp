#include "csv_reader.hpp"
#include "broadcaster.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <cstring>
#include <getopt.h>

void print_usage(const char* prog) {
  std::cout << "Usage: " << prog << " [OPTIONS]\n"
            << "Options:\n"
            << "  --log <path>     Path to normalized_log.csv (required)\n"
            << "  --speed <float>  Replay speed multiplier (default: 1.0, 0 = max speed)\n"
            << "  --port <int>     Subscriber port (default: 9100)\n"
            << "  --help           Show this help\n";
}

int main(int argc, char** argv) {
  std::string log_path;
  double speed = 1.0;
  int port = 9100;
  
  // Parse command line arguments
  static struct option long_options[] = {
    {"log", required_argument, 0, 'l'},
    {"speed", required_argument, 0, 's'},
    {"port", required_argument, 0, 'p'},
    {"help", no_argument, 0, 'h'},
    {0, 0, 0, 0}
  };
  
  int opt;
  int option_index = 0;
  while ((opt = getopt_long(argc, argv, "l:s:p:h", long_options, &option_index)) != -1) {
    switch (opt) {
      case 'l':
        log_path = optarg;
        break;
      case 's':
        speed = std::stod(optarg);
        if (speed < 0) {
          std::cerr << "Speed must be >= 0\n";
          return 1;
        }
        break;
      case 'p':
        port = std::stoi(optarg);
        break;
      case 'h':
        print_usage(argv[0]);
        return 0;
      default:
        print_usage(argv[0]);
        return 1;
    }
  }
  
  if (log_path.empty()) {
    std::cerr << "Error: --log is required\n";
    print_usage(argv[0]);
    return 1;
  }
  
  std::cout << "Replay server starting:\n"
            << "  Log file: " << log_path << "\n"
            << "  Speed: " << (speed == 0 ? "max" : std::to_string(speed) + "x") << "\n"
            << "  Port: " << port << "\n";
  
  // Initialize broadcaster for subscribers
  Broadcaster broadcaster(port, 100, 200);
  broadcaster.start();
  std::cout << "Listening for subscribers on port " << port << "\n";
  
  // Open CSV file
  CsvReader reader(log_path);
  if (!reader.is_open()) {
    std::cerr << "Failed to open log file\n";
    return 1;
  }
  
  // Read and replay ticks
  Tick tick;
  uint64_t prev_timestamp = 0;
  bool first_tick = true;
  size_t tick_count = 0;
  
  auto start_time = std::chrono::steady_clock::now();
  
  while (reader.read_next(tick)) {
    tick_count++;
    
    // Calculate delay based on timestamp difference
    if (!first_tick && speed > 0) {
      uint64_t delta_ms = tick.timestamp_ms - prev_timestamp;
      uint64_t delay_ms = static_cast<uint64_t>(delta_ms / speed);
      
      if (delay_ms > 0) {
        std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
      }
    }
    
    // Broadcast tick to all subscribers
    broadcaster.push_normalized(tick);
    
    prev_timestamp = tick.timestamp_ms;
    first_tick = false;
    
    // Progress indicator every 1000 ticks
    if (tick_count % 1000 == 0) {
      std::cout << "Replayed " << tick_count << " ticks, subscribers: " 
                << broadcaster.subscriber_count() << "\r" << std::flush;
    }
  }
  
  auto end_time = std::chrono::steady_clock::now();
  auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();
  
  std::cout << "\nReplay complete:\n"
            << "  Total ticks: " << tick_count << "\n"
            << "  Elapsed time: " << elapsed << " ms\n"
            << "  Final subscriber count: " << broadcaster.subscriber_count() << "\n";
  
  // Keep server running to allow subscribers to finish receiving
  std::cout << "Press Ctrl+C to exit...\n";
  while (true) {
    std::this_thread::sleep_for(std::chrono::seconds(1));
  }
  
  return 0;
}
