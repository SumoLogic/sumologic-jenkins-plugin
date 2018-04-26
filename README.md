# sumologic-jenkins-plugin

| TLS Deprecation Notice |
| --- |
| In keeping with industry standard security best practices, as of May 31, 2018, the Sumo Logic service will only support TLS version 1.2 going forward. Verify that all connections to Sumo Logic endpoints are made from software that supports TLS 1.2. |

Building
========

Just run `mvn`.

Install
=======

Tested with Jenkins 1.596.2

* Upload `target/sumologic-publisher.hpi` to your instance of Jenkins via
./pluginManager/advanced
