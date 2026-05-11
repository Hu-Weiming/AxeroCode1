package com.axercode.server.config;

import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerStorageConfiguration {

    @Bean
    public SqliteSessionRepository sqliteSessionRepository(ServerStorageProperties properties) {
        String resolvedPath = properties.getDatabaseFile().replace("${user.home}", System.getProperty("user.home"));
        return new SqliteSessionRepository(Path.of(resolvedPath));
    }
}
