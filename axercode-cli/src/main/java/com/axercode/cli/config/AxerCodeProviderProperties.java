package com.axercode.cli.config;

import com.axercode.provider.api.ProviderIds;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axercode.provider")
public class AxerCodeProviderProperties {

    private String defaultProvider = ProviderIds.OLLAMA;
    private String defaultModel = "qwen2.5:7b";
    private Ollama ollama = new Ollama();
    private Anthropic anthropic = new Anthropic();
    private OpenAi openai = new OpenAi();

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public Anthropic getAnthropic() {
        return anthropic;
    }

    public void setAnthropic(Anthropic anthropic) {
        this.anthropic = anthropic;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public String resolveDefaultModel(String providerId) {
        return switch (providerId) {
            case ProviderIds.ANTHROPIC -> anthropic.defaultModel != null && !anthropic.defaultModel.isBlank()
                    ? anthropic.defaultModel
                    : defaultModel;
            case ProviderIds.OPENAI -> openai.defaultModel != null && !openai.defaultModel.isBlank()
                    ? openai.defaultModel
                    : defaultModel;
            case ProviderIds.OLLAMA -> ollama.defaultModel != null && !ollama.defaultModel.isBlank()
                    ? ollama.defaultModel
                    : defaultModel;
            default -> defaultModel;
        };
    }

    public static class Ollama {
        private boolean enabled = true;
        private String baseUrl = "http://127.0.0.1:11434";
        private String defaultModel = "qwen2.5:7b";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }

    public static class Anthropic {
        private boolean enabled = true;
        private String baseUrl = "https://api.anthropic.com";
        private String apiKey = "";
        private String version = "2023-06-01";
        private int maxTokens = 2048;
        private String defaultModel = "claude-3-5-sonnet-latest";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }

    public static class OpenAi {
        private boolean enabled = true;
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String defaultModel = "gpt-4o-mini";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }
}
