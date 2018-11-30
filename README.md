# sumologic-jenkins-plugin

Building
========

Just run `mvn`.

Install
=======

Tested with Jenkins 1.596.2

* Upload `target/sumologic-publisher.hpi` to your instance of Jenkins via
./pluginManager/advanced

### TLS 1.2 Requirement
Sumo Logic only accepts connections from clients using TLS version 1.2 or greater. To utilize the content of this repo, ensure that it's running in an execution environment that is configured to use TLS 1.2 or greater.
