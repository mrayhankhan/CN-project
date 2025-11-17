#!/usr/bin/env bash
set -euo pipefail

# create data folder and initial counter if missing
mkdir -p data
if [ ! -f data/counter.txt ]; then
  printf "00000\n" > data/counter.txt
fi
if [ ! -f data/history.log ]; then
  touch data/history.log
fi

# run server using port from environment or 8080
java -cp src MainServer
