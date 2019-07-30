package com.sumologic.jenkins.jenkinssumologicplugin

import jenkins.model.Jenkins

/**

Groovy configuration script for Jenkins post-initialisation

The purpose of this script is to automate the global configuration of plugin when Jenkins starts, so that no manual intervention is required via UI afterwards.
This example of Groovy script file should have `.groovy` extension and be placed in the directory $JENKINS_HOME/init.groovy.d/.

*Note*: Make sure to adjust the values according to your needs.

 **/

def sumoLogic = Jenkins.getInstance().getDescriptor(SumoBuildNotifier.class)

sumoLogic.setQueryPortal('service.eu.sumologic.com')
sumoLogic.setMetricDataPrefix('jenkinsMetricDataPrefix')
sumoLogic.setUrl('https://<get_your_sumologic_http_source_url_here>')
sumoLogic.setSourceCategory('jenkins')
sumoLogic.setKeepOldConfigData(false)
sumoLogic.setMetricDataEnabled(true)
sumoLogic.setAuditLogEnabled(true)
sumoLogic.setPeriodicLogEnabled(true)
sumoLogic.setScmLogEnabled(false)
sumoLogic.setJobStatusLogEnabled(true)
sumoLogic.setJobConsoleLogEnabled(true)
sumoLogic.save()