#!/bin/bash

# Move all shell scripts to scripts/ folder

echo "Moving shell scripts to scripts/ folder..."
echo ""

# List of script files to move
scripts=(
    "backfill_history.sh"
    "init.sh"
    "run.sh"
    "run_all_tests.sh"
    "smoke_test.sh"
    "test_concurrency.sh"
    "test_websocket_reconnect.sh"
    "cleanup_old_files.sh"
)

# Move each script
for script in "${scripts[@]}"; do
    if [ -f "$script" ]; then
        mv "$script" scripts/
        echo "✓ Moved $script"
    fi
done

echo ""
echo "✓ All scripts moved to scripts/ folder"
echo ""
echo "Update your commands:"
echo "  Old: ./run.sh"
echo "  New: ./scripts/run.sh"
