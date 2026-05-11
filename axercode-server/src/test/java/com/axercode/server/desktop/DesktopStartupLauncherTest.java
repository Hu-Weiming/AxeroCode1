package com.axercode.server.desktop;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.URI;
import org.junit.jupiter.api.Test;

class DesktopStartupLauncherTest {

    @Test
    void launchWhenEnabledOpensBrowserAtDesktopUrl() {
        DesktopApplicationProperties properties = new DesktopApplicationProperties();
        properties.setLaunchOnStartup(true);
        properties.setHost("127.0.0.1");
        properties.setPath("/");

        BrowserLauncher browserLauncher = mock(BrowserLauncher.class);
        DesktopStartupLauncher startupLauncher = new DesktopStartupLauncher(
                properties,
                new DesktopUrlBuilder(),
                browserLauncher
        );

        startupLauncher.openAtPort(19090);

        verify(browserLauncher).open(URI.create("http://127.0.0.1:19090/"));
    }

    @Test
    void launchWhenDisabledDoesNothing() {
        DesktopApplicationProperties properties = new DesktopApplicationProperties();
        properties.setLaunchOnStartup(false);
        properties.setHost("127.0.0.1");
        properties.setPath("/");

        BrowserLauncher browserLauncher = mock(BrowserLauncher.class);
        DesktopStartupLauncher startupLauncher = new DesktopStartupLauncher(
                properties,
                new DesktopUrlBuilder(),
                browserLauncher
        );

        startupLauncher.openAtPort(19090);

        verify(browserLauncher, never()).open(org.mockito.ArgumentMatchers.any());
    }
}
