# Protocol Spec

## Transport
All framed messages use a four byte big endian length prefix, then a one byte type code, then payload bytes.

## Type codes
- 0x01: Feed Tick (binary)
- 0x02: Feed Ack (binary)
- 0x10: Subscribe Request (JSON)
- 0x11: Snapshot Response (JSON)
- 0x12: Delta Update (JSON)

## Feed Tick binary payload fields (in network byte order)
- uint64 timestamp_ms
- uint32 feed_id
- uint64 seq_id
- double price (encoded as big endian IEEE 754 64-bit)
- uint64 size
- uint8 flags

## Subscribe Request example (JSON)
```json
{
  "client_id": "sub01",
  "mode": "delta",
  "symbol": "TEST",
  "since_ts": 0
}
```

## Snapshot Response example (JSON)
```json
{
  "type": "snapshot",
  "symbol": "TEST",
  "top": { "bid": 100.1, "ask": 100.2 }
}
```

## Delta Update example (JSON)
```json
{
  "type": "delta",
  "tick": { "timestamp_ms": 1630000000000, "feed_id": 1, "seq_id": 42, "price": 100.15, "size": 100, "flags": 0 }
}
```
