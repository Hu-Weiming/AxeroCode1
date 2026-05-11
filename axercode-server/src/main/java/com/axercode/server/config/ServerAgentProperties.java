package com.axercode.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.agent")
public class ServerAgentProperties {

    private int maxToolRounds = 3;
    private int maxRecentMessages = 12;
    private int maxReflectionRounds = 1;

    public int getMaxToolRounds() {
        return maxToolRounds;
    }

    public void setMaxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
    }

    public int getMaxRecentMessages() {
        return maxRecentMessages;
    }

    public void setMaxRecentMessages(int maxRecentMessages) {
        this.maxRecentMessages = maxRecentMessages;
    }

    public int getMaxReflectionRounds() {
        return maxReflectionRounds;
    }

    public void setMaxReflectionRounds(int maxReflectionRounds) {
        this.maxReflectionRounds = maxReflectionRounds;
    }
}
