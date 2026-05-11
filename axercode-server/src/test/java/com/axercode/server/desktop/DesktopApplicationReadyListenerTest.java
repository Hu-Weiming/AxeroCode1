package com.axercode.server.desktop;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;

class DesktopApplicationReadyListenerTest {

    @Test
    void applicationReadyForwardsRuntimePortToStartupLauncher() {
        DesktopStartupLauncher startupLauncher = mock(DesktopStartupLauncher.class);
        DesktopApplicationReadyListener listener = new DesktopApplicationReadyListener(startupLauncher);
        ServletWebServerApplicationContext context = mock(ServletWebServerApplicationContext.class);
        WebServer webServer = mock(WebServer.class);
        when(context.getWebServer()).thenReturn(webServer);
        when(webServer.getPort()).thenReturn(19191);

        listener.onApplicationReady(new ApplicationReadyEvent(
                new SpringApplication(),
                new String[0],
                context,
                Duration.ofMillis(1)
        ));

        verify(startupLauncher).openAtPort(19191);
    }
}
