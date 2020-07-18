# CHANGELOG

## v2.1.0 
- Added Support to send text, KeyValueMap as JSON and Fields to X-Sumo-Fields using SumoUpload Step Function.
- Converting URL to Secret instead of plain text string.
- Adding MetricDataPrefix as Host Name in case provided in the configuration.

## v2.0.3
- Re-release of version 2.0.2

## v2.0.2
- Added SumoUpload Step Function to Send Files Data to Sumo Logic. [Issue 11](https://github.com/jenkinsci/sumologic-publisher-plugin/issues/11) Solved.
- Added Pre Launch Event and Pre Online event to Computer events.
- Bug Fix related to User Name empty when no user is identified by Jenkins.
- Improving conditions to send Test cases and stages details if global configuration to send logs is not used.

## v2.0.1
- Sumo HTTP URL encryption.
- Added a button to test the Sumo HTTP URL.
- Added groovy script for Jenkins Plugin configuration during Jenkins restart.
- Added filter condition for sumo log handler.

## V2.0.0
- Contains Pipeline and Multi-branch pipeline support.
- Added Audit information.
- Added Metric Information.
- Added Health related information for every Slave.
- Added SumoLogic Search Logs button on every build.
- Added support for sending logs for every job as well as specific jobs.
- Added support for customisation of type of logs that can be sent to SumoLogic.
- Added support for pipeline console logs identified by stage name.
- Added support for Source Control Management.
- Added support for configuration change.