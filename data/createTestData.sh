#!/usr/bin/env bash

# Creates 20 files with random content and a random size
# count*bs = file size
# Randon number between 1 - 1000
# Smallest size: 1024 * 1 =  1Kb
# Largest size: 1024 * 1000 =  1Mb

OUTPUT=~/tmp/stress-test-data
EXT=.txt

mkdir -p $OUTPUT

echo "Creating small random files for stress test..."
for i in {0..500}; do dd if=/dev/urandom bs=1024 count=$((1 + RANDOM % 1000)) of=$OUTPUT/stress$i$EXT; done

echo "Creating medium random files for stress test..."
for i in {500..600}; do dd if=/dev/urandom bs=1024 count=$((1000 + RANDOM % 1000)) of=$OUTPUT/stress$i$EXT; done

#echo "Creating large random files for stress test..."
#for i in {40..50}; do dd if=/dev/urandom bs=1024 count=$((1000000 + RANDOM % 1000)) of=$OUTPUT/stress$i; done

echo "Created sample data."

echo "Piping sample data files into Logstash."

find $OUTPUT -type f -printf '%f\t./stress-test-data\t1\t%s\n' > $OUTPUT/inventory.txt

~/tools/logstash-6.5.3/bin/logstash --path.settings . -f ciber-inventory.conf
