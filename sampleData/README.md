# Sample Data Creator Container

This module is used to create sample data that can be used in the test simulations. It is all done inside a Docker 
Container which makes it easy to deploy in any environment.

It creates two types of data RDF and Non RDF (Binary). 

When the sampleData container runs it reads the inventory.txt file and recreates the same folder structure, file name, 
and size if each file specified in it but the content is just random data. These generated files can then used in the 
tests. (See: createTestData.sh script to see how this is done)

This actually give you a good bit of control over the sample data files that I want to run in the simulations without 
the overhead of storing all the files.

Also, the rdf file in the rdf directory are copied over to the container. To add more RDF sample data in the future
it just needs to be added here.

All the file generate are stored under /export/ciber on the container. To share this data with the testbed container
it needs to be mapped to a Volume

After the data is created (and copied) an index in Elasticsearch is created that contains information about the files. 
This index is then used be the simulation tests to file the location of the test data files.


Index name: "ciber-inventory"

## Build and Run

```
docker build --tag=drastic/sample-data sampleData/

docker run -e ELASTICSEARCH_URL="http://elasticsearch:9200" -d -v nfs-ciber:/export/ciber --rm --net performance-net drastic/sample-data:latest
```