package com.axercode.server.desktop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.desktop")
public class DesktopApplicationProperties {

    private boolean launchOnStartup = true;
    private String host = "127.0.0.1";
    private String path = "/";

    public boolean isLaunchOnStartup() {
        return launchOnStartup;
    }

    public void setLaunchOnStartup(boolean launchOnStartup) {
        this.launchOnStartup = launchOnStartup;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
