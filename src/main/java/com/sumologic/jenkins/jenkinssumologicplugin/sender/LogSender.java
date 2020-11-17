package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PluginConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.CARBON_CONTENT_TYPE;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GRAPHITE_CONTENT_TYPE;


/**
 * Created by Lukasz on 20/03/2017
 */
public class LogSender {
    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());
    CloseableHttpClient httpclient;

    private LogSender() {
        LOG.log(Level.INFO, "Initializing Log Sender to Send Logs to Sumo Logic.");
        // Creating the Client Connection Pool Manager by instantiating the PoolingHttpClientConnectionManager class.
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

        //Set the maximum number of connections in the pool
        connManager.setMaxTotal(100);

        //Create a ClientBuilder Object by setting the connection manager
        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(connManager);

        //Build the CloseableHttpClient object using the build() method.
        httpclient = clientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler()).build();
    }

    private String getHost(PluginConfiguration pluginConfiguration) {
        String hostName = "unknown";
        try {
            if (pluginConfiguration.getMetricDataPrefix() != null) {
                hostName = pluginConfiguration.getMetricDataPrefix();
            } else {
                hostName = InetAddress.getLocalHost().getHostName();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't resolve jenkins host name... Using unknown.");
        }
        return hostName;
    }

    private static class LogSenderHolder {
        static LogSender logSender = new LogSender();
    }

    public static LogSender getInstance() {
        return LogSenderHolder.logSender;
    }

    void sendLogs(byte[] msg, String sumoName, String contentType, HashMap<String, String> fields) {
        HttpPost post = null;
        CloseableHttpResponse response = null;
        PluginConfiguration pluginConfiguration = PluginDescriptorImpl.getPluginConfiguration();
        try {
            post = new HttpPost(pluginConfiguration.getSumoLogicEndpoint());

            createHeaders(post, sumoName, contentType, fields, pluginConfiguration);

            byte[] compressedData = compress(msg);
            post.setEntity(new ByteArrayEntity(compressedData));

            response = httpclient.execute(post);

            StatusLine statusLine = response.getStatusLine();
            if (statusLine != null && statusLine.getStatusCode() != 200) {
                LOG.log(Level.WARNING, String.format("Received HTTP error from Sumo Service: %d", statusLine.getStatusCode()));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, String.format("Could not send log to Sumo Logic: %s", e.toString()));
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Unable to Close Response");
                }
            }
        }
    }

    public void sendLogs(byte[] msg) {
        sendLogs(msg, null, null, null);
    }

    public void sendLogs(byte[] msg, String sumoName) {
        sendLogs(msg, sumoName, null, null);
    }

    public void sendLogs(byte[] msg, String sumoName, String contentType) {
        sendLogs(msg, sumoName, contentType, null);
    }

    public void sendLogs(byte[] msg, String sumoName, HashMap<String, String> fields) {
        sendLogs(msg, sumoName, null, fields);
    }

    private byte[] compress(byte[] content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content);
        gzipOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private void createHeaders(final HttpPost post, final String sumoName, final String contentType,
                               HashMap<String, String> fields, PluginConfiguration pluginConfiguration) {
        post.addHeader("X-Sumo-Host", getHost(pluginConfiguration));

        if (StringUtils.isNotBlank(sumoName)) {
            post.addHeader("X-Sumo-Name", sumoName);
        }

        post.addHeader("X-Sumo-Category", pluginConfiguration.getSourceCategory());

        post.addHeader("Content-Encoding", "gzip");

        if (isValidContentType(contentType)) {
            post.addHeader("Content-Type", contentType);
        }

        if (fields != null && !fields.isEmpty()) {
            String field_string = fields.keySet().stream().map(key -> key + "=" + fields.get(key)).collect(Collectors.joining(","));
            post.addHeader("X-Sumo-Fields", field_string);
        }

        post.addHeader("X-Sumo-Client", "sumologic-jenkins-plugin");
    }

    private boolean isValidContentType(final String contentType) {
        if (contentType != null) {
            return GRAPHITE_CONTENT_TYPE.equals(contentType) || CARBON_CONTENT_TYPE.equals(contentType);
        }
        return false;
    }

    public StatusLine testHTTPUrl(String url) throws Exception {
        HttpPost post = null;
        CloseableHttpResponse response = null;

        try {
            post = new HttpPost(url);

            post.setEntity(new StringEntity("This is a Test Message from Jenkins Plugin."));

            response = httpclient.execute(post);

            return response.getStatusLine();
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Unable to Close Response");
                }
            }
        }
    }
}
