package com.axercode.cli.config;

import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    public SqliteSessionRepository sqliteSessionRepository(AxerCodeStorageProperties properties) {
        String resolvedPath = properties.getDatabaseFile().replace("${user.home}", System.getProperty("user.home"));
        return new SqliteSessionRepository(Path.of(resolvedPath));
    }

    @Bean
    public SqliteShellStateRepository sqliteShellStateRepository(SqliteSessionRepository sessionRepository) {
        return new SqliteShellStateRepository(sessionRepository);
    }
}
