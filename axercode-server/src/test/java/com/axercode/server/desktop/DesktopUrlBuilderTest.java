package com.axercode.server.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DesktopUrlBuilderTest {

    private final DesktopUrlBuilder builder = new DesktopUrlBuilder();

    @Test
    void buildLaunchUrlUsesHostPortAndPath() {
        assertEquals(
                "http://127.0.0.1:19090/",
                builder.buildLaunchUrl("127.0.0.1", 19090, "/").toString()
        );
    }

    @Test
    void buildLaunchUrlNormalizesMissingLeadingSlash() {
        assertEquals(
                "http://127.0.0.1:19090/chat",
                builder.buildLaunchUrl("127.0.0.1", 19090, "chat").toString()
        );
    }
}
