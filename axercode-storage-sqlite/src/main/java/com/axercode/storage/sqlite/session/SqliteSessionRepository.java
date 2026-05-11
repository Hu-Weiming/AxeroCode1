package com.axercode.storage.sqlite.session;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-backed repository that persists CLI conversation sessions to a local SQLite database.
 */
public class SqliteSessionRepository {

    private static final String ACTIVE_SESSION_KEY = "current";

    private final Path databaseFile;

    public SqliteSessionRepository(Path databaseFile) {
        if (databaseFile == null) {
            throw new IllegalArgumentException("databaseFile must not be null");
        }
        this.databaseFile = databaseFile.toAbsolutePath();
        createParentDirectories();
        initializeSchema();
    }

    public Optional<SessionContext> loadCurrentSession() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select session_id
                     from active_session
                     where singleton_key = ?
                     """)) {
            statement.setString(1, ACTIVE_SESSION_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return loadSession(SessionId.from(resultSet.getString("session_id")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load current session from SQLite", exception);
        }
    }

    public Optional<SessionContext> loadSession(SessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        try (Connection connection = openConnection()) {
            if (!sessionExists(connection, sessionId)) {
                return Optional.empty();
            }

            List<ConversationMessage> messages = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    select message_id, role, content, created_at
                    from session_messages
                    where session_id = ?
                    order by sequence_no asc
                    """)) {
                statement.setString(1, sessionId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        messages.add(new ConversationMessage(
                                UUID.fromString(resultSet.getString("message_id")),
                                MessageRole.valueOf(resultSet.getString("role")),
                                resultSet.getString("content"),
                                Instant.parse(resultSet.getString("created_at"))
                        ));
                    }
                }
            }
            return Optional.of(new SessionContext(sessionId, messages));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load session from SQLite", exception);
        }
    }

    public Optional<StoredSessionRuntime> loadSessionRuntime(SessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select provider_id, model_name
                     from session_runtime
                     where session_id = ?
                     """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredSessionRuntime(
                        resultSet.getString("provider_id"),
                        resultSet.getString("model_name")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load session runtime from SQLite", exception);
        }
    }

    public Map<UUID, String> loadSessionMessageModels(SessionId sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        try (Connection connection = openConnection()) {
            return loadSessionMessageModels(connection, sessionId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load session message models from SQLite", exception);
        }
    }

    public void saveSession(SessionContext sessionContext) {
        saveSession(sessionContext, null, null);
    }

    public void saveSession(SessionContext sessionContext, String providerId, String modelName) {
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<UUID, String> assistantMessageModels = resolveAssistantMessageModels(
                        sessionContext,
                        loadSessionMessageModels(connection, sessionContext.sessionId()),
                        modelName
                );
                upsertSession(connection, sessionContext);
                replaceMessages(connection, sessionContext);
                replaceMessageModels(connection, sessionContext.sessionId(), assistantMessageModels);
                if (providerId != null && !providerId.isBlank() && modelName != null && !modelName.isBlank()) {
                    upsertSessionRuntime(connection, sessionContext.sessionId(), providerId, modelName);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save session to SQLite", exception);
        }
    }

    public void saveCurrentSession(SessionContext sessionContext) {
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<UUID, String> assistantMessageModels = resolveAssistantMessageModels(
                        sessionContext,
                        loadSessionMessageModels(connection, sessionContext.sessionId()),
                        null
                );
                upsertSession(connection, sessionContext);
                replaceMessages(connection, sessionContext);
                replaceMessageModels(connection, sessionContext.sessionId(), assistantMessageModels);
                upsertActiveSession(connection, sessionContext.sessionId());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save current session to SQLite", exception);
        }
    }

    public List<StoredSessionSummary> listSessions(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select
                         s.session_id,
                         s.saved_at,
                         (
                             select content
                             from session_messages first_user
                             where first_user.session_id = s.session_id
                               and first_user.role = 'USER'
                             order by first_user.sequence_no asc
                             limit 1
                         ) as title_source,
                         (
                             select content
                             from session_messages latest_message
                             where latest_message.session_id = s.session_id
                             order by latest_message.sequence_no desc
                             limit 1
                         ) as preview_source,
                         (
                             select count(*)
                             from session_messages counted
                             where counted.session_id = s.session_id
                         ) as message_count
                     from sessions s
                     order by s.saved_at desc
                     limit ?
                     """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StoredSessionSummary> sessions = new ArrayList<>();
                while (resultSet.next()) {
                    String preview = summarize(resultSet.getString("preview_source"), 96, "New chat");
                    sessions.add(new StoredSessionSummary(
                            resultSet.getString("session_id"),
                            summarize(resultSet.getString("title_source"), 48, preview),
                            preview,
                            resultSet.getString("saved_at"),
                            resultSet.getInt("message_count")
                    ));
                }
                return List.copyOf(sessions);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list sessions from SQLite", exception);
        }
    }

    private void upsertSession(Connection connection, SessionContext sessionContext) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into sessions (session_id, saved_at)
                values (?, ?)
                on conflict(session_id) do update set saved_at = excluded.saved_at
                """)) {
            statement.setString(1, sessionContext.sessionId().toString());
            statement.setString(2, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private void replaceMessages(Connection connection, SessionContext sessionContext) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("""
                delete from session_messages
                where session_id = ?
                """)) {
            deleteStatement.setString(1, sessionContext.sessionId().toString());
            deleteStatement.executeUpdate();
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                insert into session_messages (
                    message_id,
                    session_id,
                    sequence_no,
                    role,
                    content,
                    created_at
                )
                values (?, ?, ?, ?, ?, ?)
                """)) {
            int sequence = 0;
            for (ConversationMessage message : sessionContext.messages()) {
                insertStatement.setString(1, message.id().toString());
                insertStatement.setString(2, sessionContext.sessionId().toString());
                insertStatement.setInt(3, sequence++);
                insertStatement.setString(4, message.role().name());
                insertStatement.setString(5, message.content());
                insertStatement.setString(6, message.createdAt().toString());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private void upsertActiveSession(Connection connection, SessionId sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into active_session (singleton_key, session_id)
                values (?, ?)
                on conflict(singleton_key) do update set session_id = excluded.session_id
                """)) {
            statement.setString(1, ACTIVE_SESSION_KEY);
            statement.setString(2, sessionId.toString());
            statement.executeUpdate();
        }
    }

    private void upsertSessionRuntime(
            Connection connection,
            SessionId sessionId,
            String providerId,
            String modelName
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into session_runtime (session_id, provider_id, model_name, saved_at)
                values (?, ?, ?, ?)
                on conflict(session_id) do update set
                    provider_id = excluded.provider_id,
                    model_name = excluded.model_name,
                    saved_at = excluded.saved_at
                """)) {
            statement.setString(1, sessionId.toString());
            statement.setString(2, providerId);
            statement.setString(3, modelName);
            statement.setString(4, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private Map<UUID, String> loadSessionMessageModels(Connection connection, SessionId sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select message_id, model_name
                from session_message_models
                where session_id = ?
                """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<UUID, String> messageModels = new LinkedHashMap<>();
                while (resultSet.next()) {
                    messageModels.put(
                            UUID.fromString(resultSet.getString("message_id")),
                            resultSet.getString("model_name")
                    );
                }
                return Map.copyOf(messageModels);
            }
        }
    }

    private Map<UUID, String> resolveAssistantMessageModels(
            SessionContext sessionContext,
            Map<UUID, String> existingMessageModels,
            String currentModelName
    ) {
        Map<UUID, String> resolvedModels = new LinkedHashMap<>();
        for (ConversationMessage message : sessionContext.messages()) {
            if (message.role() != MessageRole.ASSISTANT) {
                continue;
            }

            String preservedModel = existingMessageModels.get(message.id());
            if (preservedModel != null && !preservedModel.isBlank()) {
                resolvedModels.put(message.id(), preservedModel);
                continue;
            }

            if (currentModelName != null && !currentModelName.isBlank()) {
                resolvedModels.put(message.id(), currentModelName);
            }
        }
        return Map.copyOf(resolvedModels);
    }

    private void replaceMessageModels(
            Connection connection,
            SessionId sessionId,
            Map<UUID, String> assistantMessageModels
    ) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("""
                delete from session_message_models
                where session_id = ?
                """)) {
            deleteStatement.setString(1, sessionId.toString());
            deleteStatement.executeUpdate();
        }

        if (assistantMessageModels.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                insert into session_message_models (
                    message_id,
                    session_id,
                    model_name
                )
                values (?, ?, ?)
                """)) {
            for (Map.Entry<UUID, String> entry : assistantMessageModels.entrySet()) {
                insertStatement.setString(1, entry.getKey().toString());
                insertStatement.setString(2, sessionId.toString());
                insertStatement.setString(3, entry.getValue());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private boolean sessionExists(Connection connection, SessionId sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from sessions
                where session_id = ?
                """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
    }

    private String summarize(String content, int maxLength, String fallback) {
        String normalized = content == null
                ? ""
                : content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 1).trim() + "…";
    }

    public Path databaseFile() {
        return databaseFile;
    }

    private void createParentDirectories() {
        try {
            Path parent = databaseFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create SQLite storage directory", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists sessions (
                        session_id text primary key,
                        saved_at text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists session_messages (
                        message_id text primary key,
                        session_id text not null,
                        sequence_no integer not null,
                        role text not null,
                        content text not null,
                        created_at text not null,
                        unique (session_id, sequence_no),
                        foreign key (session_id) references sessions(session_id)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists active_session (
                        singleton_key text primary key,
                        session_id text not null,
                        foreign key (session_id) references sessions(session_id)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists session_runtime (
                        session_id text primary key,
                        provider_id text not null,
                        model_name text not null,
                        saved_at text not null,
                        foreign key (session_id) references sessions(session_id)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists session_message_models (
                        message_id text primary key,
                        session_id text not null,
                        model_name text not null,
                        foreign key (session_id) references sessions(session_id),
                        foreign key (message_id) references session_messages(message_id)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite session schema", exception);
        }
    }
}
