package com.axercode.cli.bootstrap;

import com.axercode.cli.config.AxerCodeAgentProperties;
import com.axercode.cli.config.AxerCodeProviderProperties;
import com.axercode.cli.config.AxerCodeShellProperties;
import com.axercode.cli.config.AxerCodeStorageProperties;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.ConfigurableApplicationContext;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.axercode")
@EnableConfigurationProperties({
        AxerCodeAgentProperties.class,
        AxerCodeProviderProperties.class,
        AxerCodeShellProperties.class,
        AxerCodeStorageProperties.class
})
@ImportRuntimeHints(CliNativeHints.class)
public class AxerCodeCliApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(AxerCodeCliApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run(args);
        System.exit(org.springframework.boot.SpringApplication.exit(context));
    }
}
