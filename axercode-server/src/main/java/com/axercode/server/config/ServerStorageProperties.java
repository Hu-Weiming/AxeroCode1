package com.axercode.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.storage")
public class ServerStorageProperties {

    private String databaseFile = "${user.home}/.axercode/data/axercode.db";

    public String getDatabaseFile() {
        return databaseFile;
    }

    public void setDatabaseFile(String databaseFile) {
        this.databaseFile = databaseFile;
    }
}
