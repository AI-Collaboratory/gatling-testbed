#!/usr/bin/env bash

# Recreate the files from the inventory.txt. The file sizes will be the same but the contents of the files
# will be random.

OUTPUT=/export/ciber
SRC_DIR=/local/ciber
RDF_FILES=$OUTPUT/rdf
EXT=.txt
CIBER_INDEX=ciber-inventory
INDEX_URL=$ELASTICSEARCH_URL/$CIBER_INDEX

echo "Deleting the index from Elasticsearh [$INDEX_URL]"
curl -X DELETE $INDEX_URL

echo "Deleting all existing files in the inventory..."
rm -r $OUTPUT/*

mkdir -p $RDF_FILES
cp -r $SRC_DIR/stress/rdf $OUTPUT/
cp $SRC_DIR/inventory.txt $OUTPUT/inventory.txt

cd $OUTPUT

echo "Recreating all the files in the inventory for stress test..."

file="$OUTPUT/inventory.txt"
while IFS=$'\t' read -r f1 f2 f3 f4
do
  # display fields using f1, f2,..,f7
  printf 'file: %s, dir: %s, size: %s\n' "$f1" "$f2" "$f4"
  mkdir -p $f2
  dd if=/dev/urandom bs=$f4 count=1 of=$f2/$f1;
done <"$file"

echo "Finished recreating all the files in the inventory to: $OUTPUT"

# Append the RDF files to the inventory
find $RDF_FILES -type f -printf '%f\t./rdf\t1\t%s\n' >> $OUTPUT/inventory.txt

echo "Updated the inventory file at $OUTPUT/inventory.txt with RDF files"


echo "Piping Gatling logs into Logstash."
/usr/share/logstash/bin/logstash --path.settings . -f /ciber-inventory.conf
echo "Inventory has been piped to Elasticsearh via logstash."


DATE=$(date '+%Y%m%d-%H%M%S')
echo "${DATE}" > ${OUTPUT}/sample-data.done

START_TIME=`date '+%s'`
CONT=1
while [ "${CONT}" == "1" ] ; do
  sleep 60
  CUR_TIME=`date '+%s'`
  ELAPSED_TIME=$(expr $CUR_TIME - $START_TIME)
  echo "All data loaded.  Uptime since completion: ${ELAPSED_TIME} seconds"
done

