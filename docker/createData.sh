#!/usr/bin/env bash

# Creates 20 files with random content and a random size
# count*bs = file size
# Randon number between 10 - 1000
# Smallest size: 1024 * 10 =  10Kb
# Largest size: 1024 * 1000 =  1Mb
echo "Creating random files for stress test..."
mkdir /tmp/stress-test
for i in {0..20}; do dd if=/dev/urandom bs=1024 count=$((10 + RANDOM % 1000)) of=/tmp/stress-test/stress$i; done
echo "DONE!"
