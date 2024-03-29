package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PluginConfiguration;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
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
    RequestConfig proxyConfig;
    boolean enableProxy;
    boolean enableProxyAuth;
    
    private LogSender() {
    	
        LOG.log(Level.INFO, "Initializing Log Sender to Send Logs to Sumo Logic.");
        // Creating the Client Connection Pool Manager by instantiating the PoolingHttpClientConnectionManager class.
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(-1L, TimeUnit.MINUTES);
        // Increase max total connection to 200
        connectionManager.setMaxTotal(200);
        // Increase default max connection per route to 20
        connectionManager.setDefaultMaxPerRoute(20);
        //socket timeout for 2 minutes
        SocketConfig defaultSocketConfig = SocketConfig.custom().setSoTimeout((int) TimeUnit.MINUTES.toMillis(3)).build();
        connectionManager.setDefaultSocketConfig(defaultSocketConfig);

        ConnectionKeepAliveStrategy myStrategy = new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                long keepAliveTime = super.getKeepAliveDuration(httpResponse, httpContext);
                if (keepAliveTime == -1L) {
                    keepAliveTime = TimeUnit.MINUTES.toMillis(2);
                }
                return keepAliveTime;
            }
        };
        
        //proxy enablement
        PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();
        enableProxy = descriptor.getEnableProxy();
		enableProxyAuth = descriptor.getEnableProxyAuth();
		CredentialsProvider credsProvider = null;
		if (this.enableProxy) {
			String proxyHost = descriptor.getProxyHost();
			int proxyPort = descriptor.getProxyPort();
			if (enableProxyAuth) {
				AuthScope authScope = new AuthScope(proxyHost, proxyPort);
				// Setting the credentials
				String proxyAuthUsername = descriptor.getProxyAuthUsername();
				String proxyAuthPassword = descriptor.getProxyAuthPassword();
				org.apache.http.auth.UsernamePasswordCredentials creds = new org.apache.http.auth.UsernamePasswordCredentials(
						proxyAuthUsername, proxyAuthPassword);
				credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(authScope, creds);
			}																								
			HttpHost pHost = new HttpHost(proxyHost, proxyPort, "http");		
			// Setting the proxy
			RequestConfig.Builder reqconfigconbuilder = RequestConfig.custom();
			reqconfigconbuilder = reqconfigconbuilder.setProxy(pHost);
			proxyConfig = reqconfigconbuilder.build();
			
		}

        //Create a ClientBuilder Object by setting the connection manager
        httpclient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(myStrategy)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .useSystemProperties()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
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
             if(this.enableProxy) {
             	post.setConfig(this.proxyConfig);
             }
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
            if(this.enableProxy) {
                post.setConfig(this.proxyConfig);
            }
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
