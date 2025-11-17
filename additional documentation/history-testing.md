# Paste History Feature Testing

## Overview

This guide covers testing the paste history tracking functionality, which logs all paste creation events with timestamps and metadata.

---

## What is Paste History?

The history feature maintains a log of all paste creation events in `data/history.log`. Each entry includes:
- Paste ID
- Timestamp (ISO 8601 format)
- Creator IP address
- Version number
- Action type (create)
- Deletion status

---

## Automated History Backfill

**Script**: `backfill_history.sh`

### Purpose
Creates history entries for pastes that existed before history tracking was implemented.

### What It Does
- Scans all existing paste files in `data/` directory
- Creates history entries for pastes without existing entries
- Backs up existing history.log before modifications
- Uses file modification time as timestamp
- Marks entries as system-generated

### Running the Backfill

```bash
chmod +x backfill_history.sh
./backfill_history.sh
```

### Expected Result
```
Backfilling history for existing pastes...
Backed up existing history to data/history.log.backup
Processing paste 00001...
Created history entry for 00001
Processing paste 00002...
History already exists for 00002, skipping
...
Backfill complete! Processed X pastes.
```

---

## Manual History Testing

### Test 1: History Entry on Create

#### Procedure
1. Start the server: `./run.sh`
2. Create a new paste via web interface
3. Check history log:
```bash
tail -1 data/history.log | jq
```

#### Expected Output
```json
{
  "id": "00001",
  "timestamp": "2025-11-14T10:30:45.123Z",
  "creator_ip": "127.0.0.1",
  "version": 1,
  "action": "create",
  "deleted": false
}
```

**Result**: ✅ PASS / ❌ FAIL

---

### Test 2: History Entry via curl

#### Procedure
```bash
# Create paste via API
curl -X POST http://localhost:8080/create -d "Test paste for history"

# Check latest history entry
tail -1 data/history.log | jq
```

#### Verify
- ID matches created paste
- Timestamp is current
- Creator IP is logged
- Action is "create"
- Deleted is false

**Result**: ✅ PASS / ❌ FAIL

---

### Test 3: Multiple Paste History

#### Procedure
```bash
# Create multiple pastes
for i in {1..5}; do
  curl -X POST http://localhost:8080/create -d "Test paste $i"
  sleep 1
done

# Check history count
wc -l data/history.log
```

#### Verify
- Each paste has a history entry
- Entries are in chronological order
- No duplicate entries

**Result**: ✅ PASS / ❌ FAIL

---

### Test 4: History Persistence

#### Procedure
1. Create a paste
2. Note the history entry count: `wc -l data/history.log`
3. Restart the server: `Ctrl+C` then `./run.sh`
4. Create another paste
5. Check history: `wc -l data/history.log`

#### Verify
- History count increased by 1
- Previous entries still exist
- No data loss on restart

**Result**: ✅ PASS / ❌ FAIL

---

### Test 5: History Format Validation

#### Procedure
```bash
# Validate all history entries are valid JSON
while IFS= read -r line; do
  echo "$line" | jq empty || echo "Invalid JSON: $line"
done < data/history.log
```

#### Expected
- No errors (all entries are valid JSON)
- No "Invalid JSON" messages

**Result**: ✅ PASS / ❌ FAIL

---

### Test 6: Concurrent History Writes

#### Procedure
```bash
# Create multiple pastes simultaneously
for i in {1..10}; do
  curl -X POST http://localhost:8080/create -d "Concurrent paste $i" &
done
wait

# Count history entries
PASTE_COUNT=$(ls data/*.json | wc -l)
HISTORY_COUNT=$(wc -l < data/history.log)

echo "Pastes: $PASTE_COUNT"
echo "History entries: $HISTORY_COUNT"
```

#### Verify
- History count matches paste count
- No missing entries
- No corrupted JSON

**Result**: ✅ PASS / ❌ FAIL

---

## History API Testing

### View History (if implemented)

If the server has a history API endpoint:

```bash
# Get paste history
curl http://localhost:8080/api/history

# Get specific paste history
curl http://localhost:8080/api/history/00001
```

---

## Backfill Testing

### Test 1: Backfill Existing Pastes

#### Setup
```bash
# Create pastes without history
rm -f data/history.log
curl -X POST http://localhost:8080/create -d "Paste 1"
curl -X POST http://localhost:8080/create -d "Paste 2"
curl -X POST http://localhost:8080/create -d "Paste 3"
```

#### Procedure
```bash
# Run backfill
./backfill_history.sh

# Check history
cat data/history.log | jq
```

#### Verify
- All pastes have history entries
- Timestamps are reasonable
- Creator IP is "system"
- Backup file created

**Result**: ✅ PASS / ❌ FAIL

---

### Test 2: Backfill Idempotency

#### Procedure
```bash
# Run backfill twice
./backfill_history.sh
FIRST_COUNT=$(wc -l < data/history.log)

./backfill_history.sh
SECOND_COUNT=$(wc -l < data/history.log)

echo "First: $FIRST_COUNT, Second: $SECOND_COUNT"
```

#### Verify
- Counts are equal (no duplicate entries)
- Script skips existing entries

**Result**: ✅ PASS / ❌ FAIL

---

## History File Management

### Viewing History

#### All history entries:
```bash
cat data/history.log | jq
```

#### Recent entries:
```bash
tail -10 data/history.log | jq
```

#### Count total entries:
```bash
wc -l data/history.log
```

#### Search for specific paste:
```bash
grep '"id":"00001"' data/history.log | jq
```

---

### Backup and Restore

#### Create backup:
```bash
cp data/history.log data/history.log.backup
```

#### Restore from backup:
```bash
cp data/history.log.backup data/history.log
```

---

## Troubleshooting

### Missing History Entries

**Symptoms**: Pastes exist but no history.log entries

**Fix**:
```bash
# Run backfill script
./backfill_history.sh
```

---

### Corrupted History File

**Symptoms**: Invalid JSON in history.log

**Investigation**:
```bash
# Find invalid lines
while IFS= read -r line; do
  echo "$line" | jq empty 2>&1 | grep -q "parse error" && echo "$line"
done < data/history.log
```

**Fix**:
```bash
# Restore from backup if available
cp data/history.log.backup data/history.log

# Or rebuild from pastes
rm data/history.log
./backfill_history.sh
```

---

### Duplicate Entries

**Symptoms**: Same paste has multiple create entries

**Investigation**:
```bash
# Find duplicates
cut -d'"' -f4 data/history.log | sort | uniq -d
```

**Fix**:
```bash
# Remove duplicates (keep first occurrence)
awk '!seen[$0]++' data/history.log > data/history.log.tmp
mv data/history.log.tmp data/history.log
```

---

## Testing Checklist

- [ ] New paste creates history entry
- [ ] History entry has correct format
- [ ] Multiple pastes create multiple entries
- [ ] History persists after server restart
- [ ] All entries are valid JSON
- [ ] Concurrent creates don't lose entries
- [ ] Backfill script creates missing entries
- [ ] Backfill is idempotent (no duplicates)
- [ ] History backup is created
- [ ] No corrupted entries after stress test

---

## History Entry Schema

```json
{
  "id": "string (5 digits)",
  "timestamp": "string (ISO 8601)",
  "creator_ip": "string (IPv4 or 'system')",
  "version": "number (always 1 for now)",
  "action": "string ('create')",
  "deleted": "boolean (always false for now)"
}
```

### Field Descriptions

- **id**: Paste identifier (00001, 00002, etc.)
- **timestamp**: Creation time in ISO 8601 format
- **creator_ip**: IP address of creator or "system" for backfilled entries
- **version**: Version number (future use for tracking edits)
- **action**: Type of event (currently only "create")
- **deleted**: Deletion flag (future use for soft deletes)

---

## Future Enhancements

Potential additions to history tracking:

1. **Edit History**: Track paste modifications
2. **Deletion Tracking**: Mark pastes as deleted
3. **View History**: Log paste access events
4. **History API**: Endpoint to query history
5. **History Cleanup**: Archive old entries
6. **User Identification**: Track users beyond IP
