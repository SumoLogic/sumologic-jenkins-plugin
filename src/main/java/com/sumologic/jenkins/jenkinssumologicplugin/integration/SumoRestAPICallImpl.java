package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.google.gson.Gson;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.FOLDER_URL;

public class SumoRestAPICallImpl {

    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private final HttpClient httpClient;
    private String credential;
    private Gson gson;

    private SumoRestAPICallImpl() {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
        httpClient.getParams().setParameter("http.protocol.max-redirects", 10);

        credential = "Basic " + Base64.getEncoder().encodeToString((PluginDescriptorImpl.getInstance().getAccessID()
                + ":" + PluginDescriptorImpl.getInstance().getAccessKey()).getBytes());

        gson = new Gson();
    }

    private static class SumoRestAPICallImplHolder {
        static SumoRestAPICallImpl sumoRestAPICall = new SumoRestAPICallImpl();
    }

    public static SumoRestAPICallImpl getInstance() {
        return SumoRestAPICallImpl.SumoRestAPICallImplHolder.sumoRestAPICall;
    }

    private String getDeploymentURL() {
        String deployment = StringUtils.substringBetween(PluginDescriptorImpl.getInstance().getQueryPortal(), "service.", ".sumologic");
        if (StringUtils.isNotEmpty(deployment)) {
            return "https://api." + deployment + ".sumologic.com";
        } else {
            return "https://api.sumologic.com";
        }
    }

    private String makeRestAPICall(final HttpMethod httpMethod) {
        try {
            createHeaders(httpMethod);

            httpClient.executeMethod(httpMethod);
            int statusCode = httpMethod.getStatusCode();
            if (statusCode != 200) {
                LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
                return null;
            }
            return responseAsString(httpMethod);
        } catch (Exception e) {
            LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
            return null;
        } finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }

    private String responseAsString(HttpMethod httpMethod) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(httpMethod.getResponseBodyAsStream()); BufferedReader br = new BufferedReader(inputStreamReader)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }

    private void createHeaders(final HttpMethod httpMethod) {
        httpMethod.addRequestHeader("Authorization", this.credential);
    }

    String getIdOfExistingFolderOrCreateFolderIfDoesNotExists(String folderName, String folderId, Map<String, Object> request) {
        String response = makeRestAPICall(new GetMethod(getDeploymentURL() + FOLDER_URL + folderId));
        if (StringUtils.isNotEmpty(response)) {
            try {
                Map objectValue = gson.fromJson(response, Map.class);
                if (response.contains(folderName)) {
                    if (objectValue.containsKey("children")) {
                        for (Object children : ((List) objectValue.get("children"))) {
                            if (children instanceof Map) {
                                Map<String, Object> valueOfChildren = (Map<String, Object>) children;
                                if (folderName.equals(valueOfChildren.get("name"))) {
                                    return (String) valueOfChildren.get("id");
                                }
                            }
                        }
                    }
                } else {
                    request.put("parentId", objectValue.get("id"));
                    return postRequestToSumoLogic(gson.toJson(request), getDeploymentURL() + FOLDER_URL);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return null;
    }

    private String postRequestToSumoLogic(String request, String postURL) throws IOException {
        PostMethod postMethod = new PostMethod(postURL);

        postMethod.setRequestEntity(new StringRequestEntity(request, ContentType.APPLICATION_JSON.toString(), "UTF-8"));

        String response = makeRestAPICall(postMethod);

        if (StringUtils.isNotEmpty(response) && response.contains("id")) {
            Map<String, Object> value = gson.fromJson(response, Map.class);
            return (String) value.get("id");
        }
        return null;
    }

    String createDashboardInSumoLogic(String request, String parentFolderId) {
        try {
            return postRequestToSumoLogic(request, getDeploymentURL() + FOLDER_URL + parentFolderId + "/import");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}
