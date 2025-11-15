#!/bin/bash
# Quick restart script

# Kill any running Java processes
pkill -f MainServer 2>/dev/null

# Recompile
cd src
javac *.java

# Run server in background
java MainServer &

echo "Server restarted. Access at http://localhost:8080"
echo "Debug page: http://localhost:8080/debug.html"
echo "History page: http://localhost:8080/history.html"
