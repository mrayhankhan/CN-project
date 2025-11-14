#!/bin/bash

# Backfill history for existing pastes
# This script creates history entries for pastes that existed before history tracking

echo "Backfilling history for existing pastes..."

DATA_DIR="data"
HISTORY_FILE="$DATA_DIR/history.log"

# Count existing pastes
PASTE_COUNT=$(ls -1 $DATA_DIR/*.json 2>/dev/null | wc -l)

if [ $PASTE_COUNT -eq 0 ]; then
    echo "No existing pastes found"
    exit 0
fi

echo "Found $PASTE_COUNT existing paste(s)"

# Create backup of history file if it exists
if [ -f "$HISTORY_FILE" ]; then
    cp "$HISTORY_FILE" "$HISTORY_FILE.backup"
    echo "Backed up existing history to $HISTORY_FILE.backup"
fi

# Process each paste file
for paste_file in $DATA_DIR/*.json; do
    # Extract ID from filename
    filename=$(basename "$paste_file")
    id="${filename%.json}"
    
    # Skip if not a valid 5-digit ID
    if [[ ! $id =~ ^[0-9]{5}$ ]]; then
        continue
    fi
    
    # Check if history entry already exists for this ID
    if [ -f "$HISTORY_FILE" ]; then
        if grep -q "\"id\":\"$id\"" "$HISTORY_FILE"; then
            echo "History already exists for $id, skipping"
            continue
        fi
    fi
    
    # Get file modification time
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        timestamp=$(stat -f "%Sm" -t "%Y-%m-%dT%H:%M:%SZ" "$paste_file" 2>/dev/null)
    else
        # Linux
        timestamp=$(date -r "$paste_file" -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null)
    fi
    
    # Fallback to current time if stat fails
    if [ -z "$timestamp" ]; then
        timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    fi
    
    # Create history entry
    history_entry="{\"id\":\"$id\",\"timestamp\":\"$timestamp\",\"creator_ip\":\"system\",\"version\":1,\"action\":\"create\",\"deleted\":false}"
    
    # Append to history file
    echo "$history_entry" >> "$HISTORY_FILE"
    echo "Added history for $id"
done

echo ""
echo "Backfill complete!"
echo "Total history entries: $(wc -l < $HISTORY_FILE)"
