# Graylog Neo4jOutput Plugin (Experimental)

**Required Graylog version:** 2.0 and later
**Required Neo4j version:** 3.5 and later

Breaking changes between version 2.3 and 4.0
--------------------------------------------

* 4.0 is only compatible with Neo4j 3.5+
* The HTTP url format changed (they are different between Neo4j 3.5 and Neo4j 4.x)
* The query parameters now uses Neo4j standard format `$paramName` 

Limitations: 
* does not support neo4j native types in query parameters (number, dates, booleans). 
The parameters have to be converted into the correct type if needed using cypher functions.
* no multiple database support

Installation
------------

[Download the plugin](https://github.com/mariussturm/graylog-plugin-output-neo4j/releases) and place the .jar file in
your Graylog plugin directory. The plugin directory is the plugins/ folder relative from your graylog-server directory
by default and can be configured in your graylog.conf file.

Restart graylog-server and you are done.

Getting started
---------------

This project is using Maven and requires Java 8 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.

Testing in a graylog instance
-----------------------------

* Build the plugin jar using `mvn package -DskipTests`
* Start the graylog and databases with `docker-compose up`
* Configure the plugin. See `test.http` or do it manually:
    - connect to http://localhost:9000/system/outputs
    - create a neo4j output
    - connect to http://localhost:9000/streams/000000000000000000000001/outputs
    - assign the neo4j output to the All message stream
    - create a new GELF HTTP input here http://localhost:9000/system/inputs
* Send some test data
  
    `curl -XPOST http://localhost:12201/gelf -p0 -d '{"short_message":"Hello there", "host":"example.org", "facility":"test", "source":"1.2.3.4", "user_id":"foo"}'`

* Check the nodes are created with correct properties in Neo4j
