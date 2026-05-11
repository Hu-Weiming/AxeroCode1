package com.axercode.server.bootstrap;

import com.axercode.server.config.ServerAgentProperties;
import com.axercode.server.config.ServerProviderProperties;
import com.axercode.server.config.ServerStorageProperties;
import com.axercode.server.desktop.DesktopApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.axercode.server")
@EnableConfigurationProperties({
        ServerProviderProperties.class,
        ServerAgentProperties.class,
        ServerStorageProperties.class,
        DesktopApplicationProperties.class
})
public class AxerCodeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AxerCodeServerApplication.class, args);
    }
}
