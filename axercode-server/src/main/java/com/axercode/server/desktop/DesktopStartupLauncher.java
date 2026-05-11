package com.axercode.server.desktop;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopStartupLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DesktopStartupLauncher.class);

    private final DesktopApplicationProperties properties;
    private final DesktopUrlBuilder urlBuilder;
    private final BrowserLauncher browserLauncher;

    public DesktopStartupLauncher(
            DesktopApplicationProperties properties,
            DesktopUrlBuilder urlBuilder,
            BrowserLauncher browserLauncher
    ) {
        this.properties = properties;
        this.urlBuilder = urlBuilder;
        this.browserLauncher = browserLauncher;
    }

    public void openAtPort(int port) {
        if (!properties.isLaunchOnStartup()) {
            logger.info("Desktop launch-on-startup disabled; skipping browser open.");
            return;
        }

        URI launchUri = urlBuilder.buildLaunchUrl(properties.getHost(), port, properties.getPath());
        try {
            browserLauncher.open(launchUri);
            logger.info("Opened AxerCode desktop preview at {}", launchUri);
        } catch (Exception exception) {
            logger.warn("Unable to open desktop preview browser at {}: {}", launchUri, exception.getMessage());
        }
    }
}
