# Graylog Neo4jOutput Plugin (Experimental)

**Required Graylog version:** 2.0 and later

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
