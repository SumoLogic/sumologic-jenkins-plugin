[![SumoLogic ICON](/src/main/webapp/SumoLogic_Logo.ico)](http://sumologic.com)

# sumologic-publisher
[![Build Status][jenkins-status]][jenkins-builds]
[![Jenkins Plugin][plugin-version-badge]][plugin]
[![GitHub release][github-release-badge]][github-release]
[![Jenkins Plugin Installs][plugin-install-badge]][plugin]

## Features
Plugin can be installed and used with global configuration. Below are set of features that can be used individually.

1. [SumoUpload](#SumoUpload) - The function provides an ability to upload data from Files directly to Sumo Logic. Data is sent to the HTTP Source URL mentioned in the Global Configuration.

## Prerequisite

* `Sumologic-publisher` need [Pipeline Rest API](https://plugins.jenkins.io/pipeline-rest-api), [GIT](https://plugins.jenkins.io/git), [metrics](https://plugins.jenkins.io/metrics), [subversion](https://plugins.jenkins.io/subversion) and [junit](https://plugins.jenkins.io/junit). If not present, `Sumologic-publisher` will install these plugins as a part of internal dependency.
* Hosted Collector and HTTP source on SumoLogic server.

## Installation

In `manage plugins`, search for `sumologic-publisher` version `2.0` and install the plugin.

Tested with Jenkins version `2.100 - 2.178`. Later version will be supported. In case of any issue, please raise a [issue](https://github.com/jenkinsci/sumologic-publisher-plugin/issues)

## Configuration

![configuration](/src/main/webapp/Configuration.png)

* **SumoLogic Portal Name** - Eg- service.sumologic.com (where hosted collector resides).
* **Enable proxy settings** - Check to enable proxy settings
* **Proxy Host** - Input the proxy server URL. e.g proxy.example.com
* **Proxy Port** - Input the port for the proxy server.
* **Enable Proxy Authentication** - Check to enable authentication for the proxy.
* **Username** - Input the username to use for the proxy.
* **Password** - Input the password to use for the proxy.
* **Metric Data Prefix** - Can be the name of the Jenkins Master on which plugin is installed or name with you can distinguish Jenkins Master.
* **HTTP Source URL** - URL of the HTTP Logs and Metrics Sumo Logic source.
* **Source Category** - Source Category defined for the source provided in the **Http Source URL**.
* **Keep Old Configuration for Jobs**
	* Enable to send old configuration for jobs.

* Types of Logs
	* **Metric Data** - To send metric information.
	* **Audit Logs** - To send audit information like login, Logout, Login Failure, configuration changes to jobs, changes to jenkins.
	* **Periodic Logs** - To send periodic information like Node information, Master information, Shutdown events, Jenkins system logs.
	* **SCM Logs** - To send Source control Management logs related to builds.
* **Enable Job Status for All Jobs**
	* Select to send status for all jobs
* **Enable Console Logs for All Jobs**
	* Select to send console logs for all jobs.
* **Enable Console Logs for All Jobs**
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

## Groovy script to configure the Plugin

Groovy configuration script for Jenkins post-initialisation

The purpose of this script is to automate the global configuration of plugin when Jenkins starts, so that no manual intervention is required via UI afterwards.
This example of Groovy script file should have `.groovy` extension and be placed in the directory $JENKINS_HOME/init.groovy.d/.

Download the [SumoLogicPublisherConfiguration.groovy](https://github.com/jenkinsci/sumologic-publisher-plugin/tree/master/src/main/groovy/com/sumologic/jenkins/jenkinssumologicplugin/SumoLogicPublisherConfiguration.groovy) file.
- Make sure you are adjusting the values for fields according to your need.

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

- ### Debug locally
	After success building the plugin through `mvn clean install` run :
$ export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
$ mvn hpi:run

refer : https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial#Plugintutorial-DebuggingaPlugin

#### SumoUpload
The Function can be used in Jenkins Pipelines to send files data to Sumo Logic. Function allow below properties:-
1. file - Provide a file path or a directory path. If the value is a directory then data from all files within the directory will be sent to Sumo Logic.
2. includePathPattern - Provide a pattern to include file names or extension. For eg:- *.json will include all json files only.
3. excludePathPattern - Provide a pattern to exclude file names or extension. For eg:- *.json will exclude all json files only.
4. text - Provide any string that will be sent to Sumo Logic.
5. keyValueMap - Provide a key value map that will be converted to JSON and sent to Sumo Logic.
6. workingDir - Provides the path of the directory where files are present. The path will be searched in the node where the Pipeline Step is executed.
7. fields - Provide a key value map that will be sent as [X-Sumo-Fields](https://help.sumologic.com/Manage/Fields#X-Sumo-Fields_HTTP_header) to Sumo Logic.

Below are some example uses of the Step Function :-
- Upload a file/folder from the workspace (or a String) to Sumo Logic.

```groovy
SumoUpload(file:'file.txt')
SumoUpload(file:'someFolder')
``` 

- Upload with include/exclude patterns. The option accepts a comma-separated list of patterns.

```groovy
SumoUpload(includePathPattern:'**/*', excludePathPattern:'**/*.log,**/*.json')
```

- Upload file from master directory when Pipeline Stage is running on a agent. Below will send File.txt file present in Archive Folder of the job pipeline on master system.
```groovy
node('master')
{
  SumoUpload(file: "File.txt", workingDir: "$JENKINS_HOME/jobs/$JOB_NAME/builds/$BUILD_NUMBER/archive")
}
```

- Upload a text to Sumo Logic with Fields.
```groovy
    script{
      fields = [
        jenkins_master: "test_master",
        result: currentBuild.currentResult
        ]
    }
    SumoUpload(text: "This is test String", fields: fields)
```

- Upload a Key Value map as JSON to Sumo Logic.
```groovy
    script{
      deploy_event = [
        event_name: STAGE_NAME,
        result: currentBuild.currentResult
        ]
    }
    SumoUpload(keyValueMap: deploy_event)
```

## Change Log

For Full Change Log, [Visit](./CHANGELOG.md).

## License

The sumologic-publisher is licensed under the apache v2.0 license.

## Contributing

* Fork the project on [Github](https://github.com/jenkinsci/sumologic-publisher-plugin).
* Make your feature addition or fix bug, write tests and commit.
* Create a pull request with one of maintainer's as Reviewers.

[jenkins-builds]: https://ci.jenkins.io/job/Plugins/job/sumologic-publisher-plugin/job/master

[jenkins-status]: https://ci.jenkins.io/buildStatus/icon?job=Plugins/sumologic-publisher-plugin/master

[plugin-version-badge]: https://img.shields.io/jenkins/plugin/v/sumologic-publisher.svg

[plugin]: https://plugins.jenkins.io/sumologic-publisher

[github-release-badge]: https://img.shields.io/github/release/jenkinsci/sumologic-publisher-plugin.svg?label=release

[github-release]: https://github.com/jenkinsci/sumologic-publisher-plugin/releases/latest

[plugin-install-badge]: https://img.shields.io/jenkins/plugin/i/sumologic-publisher.svg?color=blue
