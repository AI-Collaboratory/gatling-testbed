#!/usr/bin/env bash

OUTPUT=target/stress-test-data

ls -ltr target/stress-test-data/ | awk '{print $9,"./target/stress-test-data",1,$7}'

find target/stress-test-data/ -type f -printf '%f\t./target/stress-test-data\t1\t%s\n' > inventory.txt