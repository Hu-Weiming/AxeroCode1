package com.axercode.server.desktop;

import java.awt.Desktop;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class SystemBrowserLauncher implements BrowserLauncher {

    @Override
    public void open(URI uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }

            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", uri.toString()).start();
                return;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to open browser for " + uri, exception);
        }

        throw new IllegalStateException(
                "No supported browser launcher available for OS: " + System.getProperty("os.name")
        );
    }
}
