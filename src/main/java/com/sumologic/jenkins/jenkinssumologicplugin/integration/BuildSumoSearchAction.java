package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.model.Action;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.MASTER_FOLDER_DESC;

/**
 * Sourabh Jain
 * <p>
 * This action is added to each build happening in Jenkins. It adds a dashboard for that Build in personal folder of
 * SumoLogic Directory
 */
public class BuildSumoSearchAction implements Action {

    private static final Logger LOG = Logger.getLogger(BuildSumoSearchAction.class.getName());

    private String buildFolderId;

    private final static String URL_TEMPLATE = "https://%s/ui/#/library/folder/%s";

    /**
     * perform all rest api call and add the dashboard to Sumologic for build.
     *
     * @param build
     */
    public BuildSumoSearchAction(Run build) {
        this.buildFolderId = createBuildDashboardInSumoLogic(build);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/sumologic-publisher/sumologic.ico";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Build Dashboard";
    }

    @CheckForNull
    @Override
    /*
     * create just the URL to reach the specific dashboard.
     */
    public String getUrlName() {
        return String.format(URL_TEMPLATE, PluginDescriptorImpl.getInstance().getQueryPortal(), Integer.parseInt(buildFolderId, 16));
    }

    private String createBuildDashboardInSumoLogic(final Run build) {
        String jenkinsMasterName = PluginDescriptorImpl.getInstance().getJenkinsMasterName();

        String masterFolderId = createFolder(jenkinsMasterName, "personal", MASTER_FOLDER_DESC);

        if (StringUtils.isNotEmpty(masterFolderId)) {
            String jobFolderID = createFolder(build.getParent().getFullName(), masterFolderId, build.getParent().getDescription());
            if (StringUtils.isNotEmpty(jobFolderID)) {
                String buildFolderID = createFolder(String.valueOf(build.getNumber()), jobFolderID,
                        "Status is " + build.getResult() + " with duration " + CommonModelFactory.getJobRunDuration(build) / 60 + " Minutes.");

                createBuildInformationDashboard(buildFolderID, build.getParent().getFullName(), String.valueOf(build.getNumber()));
                return buildFolderID;
            }
        }
        return null;
    }

    private String createFolder(String folderName, String parentFolderId, String description) {

        Map<String, Object> request = new HashMap<>();
        request.put("name", folderName);
        request.put("description", description);

        return SumoRestAPICallImpl.getInstance().getIdOfExistingFolderOrCreateFolderIfDoesNotExists(folderName, parentFolderId, request, "id");
    }

    private void createBuildInformationDashboard(String parentFolderID, String jobName, String buildNumber) {
        String request = null;

        try {
            String buildInformationJson = fileAsString("json/BuildInformation.json");
            String replaceString = buildInformationJson.replaceAll("\\{\\{Name}}#\\{\\{Number}}\\*", (jobName + "#" + buildNumber + "*"))
                    .replaceAll("\\{\\{Name}}", "\\\\\"" + jobName + "\\\\\"")
                    .replaceAll("\\{\\{Number}}", "\\\\\"" + buildNumber + "\\\\\"");

            request = replaceString.replaceAll("\\$\\$jenkinslogsrc", "_sourceCategory = " + PluginDescriptorImpl.getInstance().getSourceCategory());
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        SumoRestAPICallImpl.getInstance().createDashboardInSumoLogic(request, parentFolderID);
    }

    private String fileAsString(final String path) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))); BufferedReader br = new BufferedReader(inputStreamReader)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}
