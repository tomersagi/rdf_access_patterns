# Annotating RDF Access Patterns

This repository contains code acompanying a paper submitted to VLDB journal for review on RDF data representations. The code allows the annoation of RDF queries according to the access pattern features described in the paper. 

## Usage

* Build using gradle
* The main entry point is the dk.aau.cs.learnRDF.BenchmarkRunner file main function
* The main function requires to parameters: 1. A path to the folder containing the queries to run. 2. A path in which the output file will be generated. 
* The result is a csv file counting the number of queries containing each access pattern feature for each subfolder of the supplied query folder. 


## Details Instructions using  Docker

### Build once

```
docker run --rm -u gradle -v "${PWD}":/home/gradle/project -w /home/gradle/project gradle:jdk11 gradle shadowJar

java -jar ./build/libs/project-all.jar

```


### Continuous building

```
docker run --rm -it -u gradle -v "${PWD}":/home/gradle/project -w /home/gradle/project --entrypoint=/bin/bash  gradle:jdk11

gradle shadowJar

```


### Running

Query set need to be activated in the `configuration.properties` file


```

java -jar ./build/libs/project-all.jar queries/ ./output

```
