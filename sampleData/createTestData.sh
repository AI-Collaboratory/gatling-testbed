#!/usr/bin/env bash

# Creates 20 files with random content and a random size
# count*bs = file size
# Randon number between 1 - 1000
# Smallest size: 1024 * 1 =  1Kb
# Largest size: 1024 * 1000 =  1Mb

OUTPUT=/export/ciber
BINARIES=$OUTPUT/stress/binaries
EXT=.txt
CIBER_INDEX=ciber-inventory

mkdir -p $BINARIES

echo "Creating small random files for stress test..."
for i in {0..2000}; do dd if=/dev/urandom bs=128 count=$((1 + RANDOM % 10)) of=$BINARIES/stress$i$EXT; done

for i in {2000..3000}; do dd if=/dev/urandom bs=256 count=$((1 + RANDOM % 100)) of=$BINARIES/stress$i$EXT; done

echo "Creating medium random files for stress test..."
for i in {3000..3500}; do dd if=/dev/urandom bs=512 count=$((1 + RANDOM % 1000)) of=$BINARIES/stress$i$EXT; done

echo "Creating large random files for stress test..."
for i in {3500..4000}; do dd if=/dev/urandom bs=1024 count=$((1 + RANDOM % 10000)) of=$BINARIES/stress$i$EXT; done

echo "Created sample data into: $BINARIES"

find $OUTPUT -type f -printf '%f\t./stress/binaries\t1\t%s\n' > $OUTPUT/inventory.txt

echo "Created inventory file at $OUTPUT/inventory.txt"

echo "Deleting the $CIBER_INDEX index from Elasticsearh"
curl -X DELETE http://elasticsearch:9200/$CIBER_INDEX

echo "Piping Gatling logs into Logstash."
/usr/share/logstash/bin/logstash --path.settings . -f /ciber-inventory.conf
echo "Inventory has been piped to Elasticsearh via logstash."