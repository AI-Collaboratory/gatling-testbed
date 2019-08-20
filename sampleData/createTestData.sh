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
INDEX_URL=$ELASTICSEARCH_URL/$CIBER_INDEX

mkdir -p $BINARIES

echo "Creating small random files for stress test..."
for i in {0..2000}; do dd if=/dev/urandom bs=128 count=$((1 + RANDOM % 10)) of=$BINARIES/stress$i$EXT; done

for i in {2000..3000}; do dd if=/dev/urandom bs=256 count=$((1 + RANDOM % 100)) of=$BINARIES/stress$i$EXT; done

echo "Creating medium random files for stress test..."
for i in {3000..3500}; do dd if=/dev/urandom bs=512 count=$((1 + RANDOM % 1000)) of=$BINARIES/stress$i$EXT; done

echo "Creating large random files for stress test..."
for i in {3500..4000}; do dd if=/dev/urandom bs=1024 count=$((1 + RANDOM % 10000)) of=$BINARIES/stress$i$EXT; done

echo "Created sample data into: $BINARIES"

find $BINARIES -type f -printf '%f\t./stress/binaries\t1\t%s\n' > $OUTPUT/inventory.txt

echo "Created inventory file at $OUTPUT/inventory.txt"

echo "Deleting the index from Elasticsearh [$INDEX_URL]"
curl -X DELETE $INDEX_URL

echo "Piping Gatling logs into Logstash."
/usr/share/logstash/bin/logstash --path.settings . -f /ciber-inventory.conf
echo "Inventory has been piped to Elasticsearh via logstash."

DATE=$(date '+%Y%m%d-%H%M%S')
echo "${DATE}" > ${OUTPUT}/sample-data.done

#START_TIME=`date '+%s'`
#CONT=1
#while [ "${CONT}" == "1" ] ; do
#  sleep 60
#  CUR_TIME=`date '+%s'`
#  ELAPSED_TIME=$(expr $CUR_TIME - $START_TIME)
#  echo "All data loaded.  Uptime since completion: ${ELAPSED_TIME} seconds"
#done

