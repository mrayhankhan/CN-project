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

# compile java sources
javac src/*.java

# run server using port from environment or 8080
if [ -z "${PORT:-}" ]; then
  java -cp src MainServer
else
  export PORT
  java -cp src MainServer
fi
