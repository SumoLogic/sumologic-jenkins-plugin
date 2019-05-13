package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.CARBON_CONTENT_TYPE;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GRAPHITE_CONTENT_TYPE;


/**
 * Created by Lukasz on 20/03/2017
 */
public class LogSender {
    private final static Logger LOG = Logger.getLogger(LogSender.class.getName());
    private static LogSender INSTANCE = null;

    private final HttpClient httpClient;

    private LogSender() {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
    }

    protected String getHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warning("Couldn't resolve jenkins host name... Using unknown.");
        }

        return "unkown";
    }

    public static LogSender getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogSender();
        }

        return INSTANCE;
    }

    public void sendLogs(String url, byte[] msg, String sumoName, String sumoCategory, String contentType) {
        PostMethod post = null;

        if (StringUtils.isBlank(url)) {
            LOG.warning("Trying to send logs with blank url. Update config first!");
            return;
        }

        try {
            post = new PostMethod(url);

            createHeaders(post, sumoName, sumoCategory, contentType);

            byte[] compressedData = compress(msg);

            post.setRequestEntity(new ByteArrayRequestEntity(compressedData));
            httpClient.executeMethod(post);
            int statusCode = post.getStatusCode();
            if (statusCode != 200) {
                LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
            }
        } catch (IOException e) {
            LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    public void sendLogs(String url, byte[] msg, String sumoName, String sumoCategory) {
        sendLogs(url, msg, sumoName, sumoCategory, null);
    }

    private byte[] compress(byte[] content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content);
        gzipOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private void createHeaders(final PostMethod post, final String sumoName,
                               final String sumoCategory, final String contentType) {

        post.addRequestHeader("X-Sumo-Host", getHost());

        if (StringUtils.isNotBlank(sumoName)) {
            post.addRequestHeader("X-Sumo-Name", sumoName);
        }

        if (StringUtils.isNotBlank(sumoCategory)) {
            post.addRequestHeader("X-Sumo-Category", sumoCategory);
        }

        post.addRequestHeader("Content-Encoding", "gzip");

        if (isValidContentType(contentType)) {
            post.addRequestHeader("Content-Type", contentType);
        }
    }

    private boolean isValidContentType(final String contentType) {
        if (contentType != null) {
            return GRAPHITE_CONTENT_TYPE.equals(contentType) || CARBON_CONTENT_TYPE.equals(contentType);
        }
        return false;
    }

}
