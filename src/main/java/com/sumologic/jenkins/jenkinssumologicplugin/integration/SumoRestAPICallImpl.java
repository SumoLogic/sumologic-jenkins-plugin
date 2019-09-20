package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.*;

public class SumoRestAPICallImpl {

    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private final HttpClient httpClient;
    private ObjectMapper mapper;
    private PluginDescriptorImpl pluginDescriptor;

    private SumoRestAPICallImpl() {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
        httpClient.getParams().setParameter("http.protocol.max-redirects", 10);

        this.pluginDescriptor = PluginDescriptorImpl.getInstance();
        mapper = new ObjectMapper();
    }

    private static class SumoRestAPICallImplHolder {
        static SumoRestAPICallImpl sumoRestAPICall = new SumoRestAPICallImpl();
    }

    public static SumoRestAPICallImpl getInstance() {
        return SumoRestAPICallImpl.SumoRestAPICallImplHolder.sumoRestAPICall;
    }

    private String getDeploymentURL() {
        String deployment = StringUtils.substringBetween(pluginDescriptor.getQueryPortal(), "service.", ".sumologic");
        deployment = StringUtils.isNotEmpty(deployment) ? "." + deployment : "";
        return "https://service" + deployment + ".sumologic.com";
    }

    private void createHeaders(final HttpMethod httpMethod, String sessionId) {
        if (StringUtils.isNotEmpty(sessionId)) {
            httpMethod.addRequestHeader("apisession", sessionId);
        }
        httpMethod.addRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
        httpMethod.addRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        httpMethod.addRequestHeader("Content-Type", "application/json");
    }

    private Map makeRestAPICall(final HttpMethod httpMethod, final String sessionId) {
        try {
            createHeaders(httpMethod, sessionId);
            httpClient.executeMethod(httpMethod);
            int statusCode = httpMethod.getStatusCode();
            if (statusCode != 200) {
                LOG.warning("Received error for " + httpMethod.getPath());
                return null;
            }
            return mapper.readValue(httpMethod.getResponseBodyAsStream(), Map.class);
        } catch (Exception e) {
            LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
            return null;
        } finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }

    private Object postRequestToSumoLogic(String request, String postURL, String key, String sessionId) throws IOException {
        PostMethod postMethod = new PostMethod(postURL);

        postMethod.setRequestEntity(new StringRequestEntity(request, ContentType.APPLICATION_JSON.toString(), "UTF-8"));

        Map response = makeRestAPICall(postMethod, sessionId);
        if (response != null && response.containsKey(key)) {
            return response.get(key);
        }
        return null;
    }

    public String getSessionForUserAfterLoginWithCredentials(String email, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        String sessionKey = "";
        try {
            Object reponse = postRequestToSumoLogic(mapper.writeValueAsString(request),
                    getDeploymentURL() + CREDENTIALS_URL, "apiSessionId", null);
            sessionKey = Objects.nonNull(reponse) ? String.valueOf(reponse) : "";
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return sessionKey;
    }

    private String getPersonalFolderIdFromSumoLogic(Map response) {
        if (response != null) {
            try {
                if (response.containsKey("folder")) {
                    Map folder = (Map) response.get("folder");
                    if (folder.containsKey("id")) {
                        return String.valueOf(folder.get("id"));
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return null;
    }

    private void importAppInSumoLogic(String personalFolderId, String sessionId) {
        Map<String, String> replaceDetails = new HashMap<>();
        try {
            replaceDetails.put("\\$\\$NAME", pluginDescriptor.getJenkinsMasterName());
            replaceDetails.put("\\$\\$jenkinslogsrc", "_sourceCategory = " + pluginDescriptor.getSourceCategory());
            String appFolderID = importDashboardInAFolder(personalFolderId, sessionId, "json/JenkinsApp.json",
                    replaceDetails, "id");

            if (StringUtils.isNotEmpty(appFolderID)) {
                String jenkinsJobInformationFolderID = getChildrenFolderId(sessionId,
                        appFolderID, Collections.singletonList("Job Monitoring"), "id", "folder");
                if (StringUtils.isNotEmpty(jenkinsJobInformationFolderID)) {
                    //Import Build Dashboard and get access Key
                    replaceDetails.clear();
                    replaceDetails.put("\\$\\$jenkinslogsrc", "_sourceCategory = " + pluginDescriptor.getSourceCategory());
                    String buildDashboardAccessKey = importDashboardInAFolder(jenkinsJobInformationFolderID, sessionId, "json/BuildDashboard.json",
                            replaceDetails, "accessKey");
                    pluginDescriptor.setBuildDashboardId(buildDashboardAccessKey);

                    //Import Job Dashboard and get access Key
                    replaceDetails.put("\\$\\$BUILDDASHBOARDID", buildDashboardAccessKey);
                    String jobDashboardAccessKey = importDashboardInAFolder(jenkinsJobInformationFolderID, sessionId, "json/JobDashboard.json",
                            replaceDetails, "accessKey");
                    pluginDescriptor.setJobDashboardId(jobDashboardAccessKey);

                    //Import Job Overview dashboard and done.
                    replaceDetails.put("\\$\\$JOBDASHBOARDID", jobDashboardAccessKey);
                    importDashboardInAFolder(jenkinsJobInformationFolderID, sessionId, "json/OverviewDashboard.json",
                            replaceDetails, "accessKey");
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private String importDashboardInAFolder(String folderID, String sessionId, String fileName,
                                            Map<String, String> replaceDetails, String key) throws IOException {
        String request = fileAsString(fileName);
        if (StringUtils.isNotEmpty(request)) {
            for (Map.Entry<String, String> entry : replaceDetails.entrySet()) {
                request = request.replaceAll(entry.getKey(), entry.getValue());
            }

            Map<String, Object> map = new HashMap<>();
            map.put("content", request);

            Map response = (Map) postRequestToSumoLogic(mapper.writeValueAsString(map),
                    getDeploymentURL() + String.format(IMPORT_URL, Integer.parseInt(folderID)),
                    "folderable", sessionId);

            if (response != null && response.containsKey(key)) {
                return String.valueOf(response.get(key));
            }
        }
        return null;
    }

    private String fileAsString(String fileName) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName))); BufferedReader br = new BufferedReader(inputStreamReader)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }

    private String getChildrenFolderId(String sessionId, String parentId,
                                       List<String> childFolderName, String keyToFetch,
                                       String typeOfContent) {

        Map response = makeRestAPICall(new GetMethod(getDeploymentURL() + FOLDER_URL + parentId), sessionId);
        if (response != null) {
            try {
                return getIdFromFolder(response, childFolderName, keyToFetch, typeOfContent);
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return null;
    }

    private String getIdFromFolder(Map response, List<String> childFolderName,
                                   String keyToFetch, String typeOfContent) {
        if (response.containsKey("folder")) {
            Map parentFolder = (Map) response.get("folder");
            if (parentFolder.containsKey("children")) {
                List childrens = (List) parentFolder.get("children");
                for (Object children : childrens) {
                    Map childrenValue = (Map) children;
                    if (childrenValue.containsKey(typeOfContent)) {
                        Map folder = (Map) childrenValue.get(typeOfContent);
                        if (folder.containsKey("name")
                                && childFolderName.contains((String) folder.get("name"))) {
                            return String.valueOf(folder.get(keyToFetch));
                        }

                    }
                }
            }
        }
        return null;
    }

    public void installJenkinsApp() {
        String sessionId = getSessionForUserAfterLoginWithCredentials(pluginDescriptor.getEmail(), pluginDescriptor.getPassword());
        if (StringUtils.isNotEmpty(sessionId)) {
            Map response = makeRestAPICall(new GetMethod(getDeploymentURL() + FOLDER_URL + "personal"), sessionId);
            String personalFolderId = getPersonalFolderIdFromSumoLogic(response);

            if (StringUtils.isNotEmpty(personalFolderId)) {
                String appID = getIdFromFolder(response,
                        Collections.singletonList(pluginDescriptor.getJenkinsMasterName()), "id", "folder");
                if (StringUtils.isEmpty(appID)) {
                    importAppInSumoLogic(personalFolderId, sessionId);
                    LOG.info("App installed in SumoLogic");
                }
            }
        }
    }
}
