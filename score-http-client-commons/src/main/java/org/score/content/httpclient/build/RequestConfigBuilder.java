package org.score.content.httpclient.build;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.score.content.httpclient.HttpClientInputs;

public class RequestConfigBuilder {
    private String connectionTimeout = "-1";
    private String socketTimeout = "-1";
    private String followRedirects = "true";
    private String proxyHost;
    private String proxyPort = "8080";

    public RequestConfigBuilder setConnectionTimeout(String connectionTimeout) {
        if (!StringUtils.isEmpty(connectionTimeout)) {
            this.connectionTimeout = connectionTimeout;
        }
        return this;
    }

    public RequestConfigBuilder setSocketTimeout(String socketTimeout) {
        if (!StringUtils.isEmpty(socketTimeout)) {
            this.socketTimeout = socketTimeout;
        }
        return this;
    }

    public RequestConfigBuilder setFollowRedirects(String followRedirects) {
        if (!StringUtils.isEmpty(followRedirects)) {
            this.followRedirects = followRedirects;
        }
        return this;
    }

    public RequestConfigBuilder setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public RequestConfigBuilder setProxyPort(String proxyPort) {
        if (!StringUtils.isEmpty(proxyPort)) {
            this.proxyPort = proxyPort;
        }
        return this;
    }

    public RequestConfig buildRequestConfig() {
        HttpHost proxy = null;
        if (proxyHost != null && !proxyHost.isEmpty()) {
            try {
                proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cound not parse '"+ HttpClientInputs.PROXY_PORT
                        +"' input: " + e.getMessage(), e);
            }
        }
        return RequestConfig.custom()
                .setConnectTimeout(Integer.parseInt(connectionTimeout))
                .setSocketTimeout(Integer.parseInt(socketTimeout))
                .setProxy(proxy)
                .setRedirectsEnabled(Boolean.parseBoolean(followRedirects)).build();
    }
}