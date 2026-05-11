package com.axercode.provider.api;

/**
 * Encodes the selected provider together with the provider-local model name into the existing ProviderRequest model field.
 */
public record ProviderModelRef(String providerId, String model) {

    private static final String DELIMITER = "::";

    public ProviderModelRef {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
    }

    public static ProviderModelRef parse(String compositeModel, String defaultProviderId) {
        if (compositeModel == null || compositeModel.isBlank()) {
            throw new IllegalArgumentException("compositeModel must not be blank");
        }
        int delimiterIndex = compositeModel.indexOf(DELIMITER);
        if (delimiterIndex < 0) {
            return new ProviderModelRef(defaultProviderId, compositeModel);
        }
        return new ProviderModelRef(
                compositeModel.substring(0, delimiterIndex),
                compositeModel.substring(delimiterIndex + DELIMITER.length())
        );
    }

    public static String compose(String providerId, String model) {
        return new ProviderModelRef(providerId, model).providerId() + DELIMITER + model;
    }
}
