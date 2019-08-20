#!/bin/bash

while getopts ":e:i:s:" opt; do
  case $opt in
    e) ELASTICSEARCH_URL="$OPTARG"
    ;;
    i) INDEX_NAME="$OPTARG"
    ;;
    s) SERVER_UNDER_TEST_URL="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

# If the values are not set then set them to defaults
if [ -z $ELASTICSEARCH_URL ]
  then
    ELASTICSEARCH_URL="http://elasticsearch:9200"
fi

if [ -z $INDEX_NAME ]
  then
    INDEX_NAME="gatling-ldp-$(uuid)"
fi

if [ -z $SERVER_UNDER_TEST_URL ]
  then
    SERVER_UNDER_TEST_URL="http://enterpriseserver_trellis_1:8080"
fi

printf "Elasticsearch is %s\n" "$ELASTICSEARCH_URL"
printf "Index is %s\n" "$INDEX_NAME"
printf "Server under test is %s\n" "$SERVER_UNDER_TEST_URL"


docker run -e SIM_CLASS="ldp.SolidStressTestIngest" -e LDP_URL=$SERVER_UNDER_TEST_URL \
  -e ELASTICSEARCH_URL=$ELASTICSEARCH_URL \
  -e LOCAL_PATH_PREFIX="/srv/ciber" \
  -v nfs-ciber:/srv/ciber \
  -e INDEX_NAME=$INDEX_NAME \
  --name gatling-performance-tests \
  --net performance-net \
  --rm \
  gregjan/gatling-testbed-worker:0.5.0-SNAPSHOT
