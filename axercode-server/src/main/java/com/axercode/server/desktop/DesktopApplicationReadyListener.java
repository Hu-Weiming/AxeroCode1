package com.axercode.server.desktop;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopApplicationReadyListener {

    private final DesktopStartupLauncher startupLauncher;

    public DesktopApplicationReadyListener(DesktopStartupLauncher startupLauncher) {
        this.startupLauncher = startupLauncher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (event.getApplicationContext() instanceof WebServerApplicationContext webServerApplicationContext
                && webServerApplicationContext.getWebServer() != null) {
            startupLauncher.openAtPort(webServerApplicationContext.getWebServer().getPort());
        }
    }
}
