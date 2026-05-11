package com.axercode.provider.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.session.ConversationMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmProviderStreamingContractTest {

    @Test
    void streamGenerateRejectsUnsupportedProvidersByDefault() {
        LlmProvider provider = new LlmProvider() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public ProviderResponse generate(ProviderRequest request) {
                return ProviderResponse.complete("sync");
            }
        };

        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                true
        );

        ProviderException exception = assertThrows(
                ProviderException.class,
                () -> provider.streamGenerate(request, ignored -> {
                })
        );

        assertFalse(provider.supportsStreaming());
        assertEquals("stub", exception.providerName());
        assertEquals("streamGenerate", exception.operation());
    }
}
