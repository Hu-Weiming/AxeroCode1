package com.axercode.server.desktop;

import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class DesktopUrlBuilder {

    public URI buildLaunchUrl(String host, int port, String path) {
        String normalizedPath = normalizePath(path);
        return URI.create("http://" + host + ":" + port + normalizedPath);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
