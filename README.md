[![SumoLogic ICON](/src/main/webapp/SumoLogic_Logo.ico)](http://sumologic.com)

# sumologic-publisher

## Prerequisite

* `Sumologic-publisher 2.0.0` need [Pipeline Rest API(2.8)](https://plugins.jenkins.io/pipeline-rest-api), [GIT (3.3.2)](https://plugins.jenkins.io/git), [metrics (3.0.0)](https://plugins.jenkins.io/metrics), [subversion (2.9)](https://plugins.jenkins.io/subversion) and [junit (1.8)](https://plugins.jenkins.io/junit). If not present, `Sumologic-publisher` will install these plugins as a part of internal dependency.
* Hosted Collector and HTTP source on SumoLogic server.

## Installation

In `manage plugins`, search for `sumologic-publisher` version `2.0` and install the plugin.

Tested with Jenkins version `2.100 - 2.178`. Later version will be supported. In case of any issue, please raise a [issue](https://github.com/SumoLogic/sumologic-jenkins-plugin/issues)

## Configuration

![configuration](/src/main/webapp/Configuration.png)

* **SumoLogic Portal Name** - Eg- service.sumologic.com (where hosted collector resides).
* **Metric Data Prefix** - Can be the name of the Jenkins Master on which plugin is installed or name with you can distinguish Jenkins Master.
* **Http Source URL** - Source configured on the sumologic server.
* **Source Category** - Source Category defined for the source provided in the **Http Source URL**.
* **Keep Old Configuration for Jobs**
	* Enable to keep old configuration for jobs.

* Types of Logs
	* **Metric Data** - To send metric information.
	* **Audit Logs** - To send audit information like login, Logout, Login Failure, configuration changes to jobs, changes to jenkins.
	* **Periodic Logs** - To send periodic information like Node information, Master information, Shutdown events, Jenkins system logs.
	* **SCM Logs** - To send Source control Management logs related to builds.
* **Enable Job Status for All Jobs**
	* Select to send status for all jobs
* **Enable Console Logs for All Jobs**
	* Select to send status for all jobs.
	
**_`In case of specific Jobs`_**

* For Freestyle and maven Project
	* Go To Job Configuration
		* In the Post build Actions, select `Sumo Logic Build Logger`
		
		![freestyle](/src/main/webapp/FreeStyle.png)
		
* For Pipeline Jobs
	* Go To Job Configuration
		* In the pipeline configuration, for normal script make below as the top level.
		
			`SumoPipelineLogCollection {
				// your script
		 	}`
		 
         ![pipeline_Normal](/src/main/webapp/Pipeline_Normal.png)
		
		* In the pipeline configuration, for declarative pipeline script update the option.
		
			`options {
				SumoPipelineLogCollection()
				}`

		![pipeline_Dec](/src/main/webapp/Pipeline_Dec.png)

## Developer Version

- ### Environment

	The following build environment is required to build the plugin

	* `Java 1.8` and `Maven 3.6.x`.

- ### Build

	Run `mvn clean install` or `mvn package`.

	Commands will create a `sumologic-publisher.hpi` in **_target_** folder.

- ### Deploy

	Upload the `sumologic-publisher.hpi` in advanced section of **_manage plugin_** in Jenkins.

	![uploadPlugin.png](/src/main/webapp/uploadPlugin.png)

## License

The sumologic-publisher is licensed under the apache v2.0 license.

## Contributing

* Fork the project on [Github](https://github.com/SumoLogic/sumologic-jenkins-plugin).
* Make your feature addition or fix bug, write tests and commit.
* Create a pull request with one of maintainer's as Reviewers.
