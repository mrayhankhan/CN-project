# Market Data Stream Normalizer

## Overview
Build a server that ingests multiple noisy market feeds and emits a single clean normalized stream.

## Build
Open terminal:
```bash
bash scripts/build.sh
```

## Run
1. Start the ingest server
2. Start one or more feed generators that connect to the ingest server
3. Start a subscriber client to receive normalized ticks

## Ports
- Feed ingest port: 9000
- Subscriber port: 9100
- Admin http port: 9200

## Repository layout
See docs and src folders for code and protocol

## License
MIT