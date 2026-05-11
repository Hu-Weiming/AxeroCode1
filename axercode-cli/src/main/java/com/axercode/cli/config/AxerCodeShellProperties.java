package com.axercode.cli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.shell")
public class AxerCodeShellProperties {

    private String historyFile = "${user.home}/.axercode/cli.history";

    public String getHistoryFile() {
        return historyFile;
    }

    public void setHistoryFile(String historyFile) {
        this.historyFile = historyFile;
    }
}
