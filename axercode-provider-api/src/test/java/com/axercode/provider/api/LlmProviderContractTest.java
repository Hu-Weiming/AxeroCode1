package com.axercode.provider.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.session.ConversationMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmProviderContractTest {

    @Test
    void supportsStreamingDefaultsToFalse() {
        LlmProvider provider = new LlmProvider() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public ProviderResponse generate(ProviderRequest request) {
                return ProviderResponse.complete(request.messages().getFirst().content());
            }
        };

        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                false
        );

        assertFalse(provider.supportsStreaming());
        provider.generate(request);
    }
}
