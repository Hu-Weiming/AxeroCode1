package com.axercode.cli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.storage")
public class AxerCodeStorageProperties {

    private String databaseFile = "${user.home}/.axercode/data/axercode.db";

    public String getDatabaseFile() {
        return databaseFile;
    }

    public void setDatabaseFile(String databaseFile) {
        this.databaseFile = databaseFile;
    }
}
