# Quick Start Guide

## Build Everything

```bash
bash scripts/build.sh
```

## Run Live System

### Terminal 1: Start Ingest Server
```bash
./build/src/ingest_server
```

Expected output:
```
admin http running on port 9200
feed ingest listening on port 9000
```

### Terminal 2: Start Feed Generator
```bash
./build/src/feed_generator 1
```

This generates 5000 ticks with feed_id=1, injecting jitter and duplicates.

### Terminal 3: Start Subscriber
```bash
./build/src/subscriber_client
```

You'll see JSON delta updates streaming in real-time.

### Terminal 4: Monitor Status
```bash
curl http://localhost:9200/status
```

Output: `{"feeds":0,"subscribers":1}`

## Run Replay System

First, generate some data with the live system (let it run for ~30 seconds).  
A file `normalized_log.csv` will be created.

Then:

```bash
# Replay at real-time speed
./build/src/replay_server --log normalized_log.csv --speed 1.0

# Replay at 10x speed
./build/src/replay_server --speed 10.0

# Replay as fast as possible
./build/src/replay_server --speed 0
```

In another terminal, connect a subscriber:
```bash
./build/src/subscriber_client
```

## Run Tests

```bash
cd build
make test
```

Or run individual tests:
```bash
./build/tests/test_serialization
./build/tests/test_smoothing
./build/tests/test_token_bucket
./build/tests/test_replay
```

## Configuration

Edit these values in source files:

**Normalization Window** (normalizer.cpp):
```cpp
Normalizer normalizer(200, 5);  // 200ms window, 5-tick smoothing
```

**Rate Limiting** (broadcaster.hpp):
```cpp
Broadcaster broadcaster(port, 100, 200);  // 100 msg/sec, burst 200
```

**Smoothing** (ingest_server.cpp):
```cpp
Normalizer normalizer(200, 0);  // 0 = no smoothing
Normalizer normalizer(200, 10); // 10-tick moving average
```

## Architecture

```
Feed Generator → Port 9000 → Ingest Server → Normalizer
                                                  ↓
                           CSV Log ← Persistence ← 
                                                  ↓
                           Port 9100 ← Broadcaster → Subscribers
                                      (rate limited)
```

Replay flow:
```
CSV Log → Replay Server → Broadcaster → Subscribers
         (paced output)  (port 9100)
```

## Features Enabled

✅ Time-window ordering (200ms)  
✅ Deduplication by sequence ID  
✅ Moving average smoothing (configurable)  
✅ Token bucket rate limiting (100/sec per subscriber)  
✅ CSV persistence  
✅ Historical replay (1x, 10x, fastest)  
✅ Multi-subscriber broadcast  
✅ HTTP admin endpoint  

## Troubleshooting

**Port already in use:**
```bash
lsof -i :9000  # Find process using port
kill <PID>     # Kill it
```

**Subscriber not receiving data:**
- Check server is running: `ps aux | grep ingest_server`
- Check port: `netstat -an | grep 9100`
- Verify admin endpoint: `curl localhost:9200/status`

**Build fails:**
```bash
rm -rf build
bash scripts/build.sh
```

## Performance Tips

1. **Reduce smoothing window** for lower latency
2. **Increase rate limit** for high-throughput subscribers
3. **Use replay speed 0** for benchmark testing
4. **Monitor with admin endpoint** to track subscriber count

Enjoy! 🚀
