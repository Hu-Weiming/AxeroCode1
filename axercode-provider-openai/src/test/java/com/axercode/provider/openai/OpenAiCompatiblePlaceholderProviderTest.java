package com.axercode.provider.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.session.ConversationMessage;
import com.axercode.provider.api.ProviderException;
import org.junit.jupiter.api.Test;

class OpenAiCompatiblePlaceholderProviderTest {

    @Test
    void generateReturnsFriendlyComingSoonError() {
        OpenAiCompatiblePlaceholderProvider provider = new OpenAiCompatiblePlaceholderProvider();

        ProviderException exception = assertThrows(
                ProviderException.class,
                () -> provider.generate(ProviderRequest.create(
                        "gpt-4o-mini",
                        java.util.List.of(ConversationMessage.user("hello")),
                        java.util.List.of(),
                        false
                ))
        );

        assertEquals("当前功能开发难度较大，目前正在加急修复中。", exception.getMessage());
    }
}
