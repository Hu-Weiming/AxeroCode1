package com.axercode.storage.sqlite.shell;

import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionContextBrancher;
import com.axercode.core.session.SessionId;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite repository for persisted interactive shell workspace state.
 */
public class SqliteShellStateRepository {

    private static final String FOCUS_KEY = "current";
    private static final String ACTIVE_CHECKPOINT_KEY = "current";
    private static final String ACTIVE_BRANCH_KEY = "current";
    private static final String PLAN_MODE_KEY = "current";

    private final SqliteSessionRepository sessionRepository;
    private final Path databaseFile;

    public SqliteShellStateRepository(SqliteSessionRepository sessionRepository) {
        if (sessionRepository == null) {
            throw new IllegalArgumentException("sessionRepository must not be null");
        }
        this.sessionRepository = sessionRepository;
        this.databaseFile = sessionRepository.databaseFile();
        initializeSchema();
    }

    public Optional<Path> loadFocusPath() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select focus_path
                     from shell_focus
                     where singleton_key = ?
                     """)) {
            statement.setString(1, FOCUS_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(Path.of(resultSet.getString("focus_path")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load shell focus from SQLite", exception);
        }
    }

    public void saveFocusPath(Path focusPath) {
        if (focusPath == null) {
            throw new IllegalArgumentException("focusPath must not be null");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_focus (singleton_key, focus_path, saved_at)
                     values (?, ?, ?)
                     on conflict(singleton_key) do update set
                         focus_path = excluded.focus_path,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, FOCUS_KEY);
            statement.setString(2, focusPath.toAbsolutePath().toString());
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save shell focus to SQLite", exception);
        }
    }

    public void clearFocusPath() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     delete from shell_focus
                     where singleton_key = ?
                     """)) {
            statement.setString(1, FOCUS_KEY);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear shell focus from SQLite", exception);
        }
    }

    public Optional<String> loadActiveCheckpointName() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select checkpoint_name
                     from shell_active_checkpoint
                     where singleton_key = ?
                     """)) {
            statement.setString(1, ACTIVE_CHECKPOINT_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getString("checkpoint_name"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load active shell checkpoint from SQLite", exception);
        }
    }

    public void saveActiveCheckpointName(String checkpointName) {
        if (checkpointName == null || checkpointName.isBlank()) {
            throw new IllegalArgumentException("checkpointName must not be blank");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_active_checkpoint (singleton_key, checkpoint_name, saved_at)
                     values (?, ?, ?)
                     on conflict(singleton_key) do update set
                         checkpoint_name = excluded.checkpoint_name,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, ACTIVE_CHECKPOINT_KEY);
            statement.setString(2, checkpointName);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save active shell checkpoint to SQLite", exception);
        }
    }

    public void clearActiveCheckpointName() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     delete from shell_active_checkpoint
                     where singleton_key = ?
                     """)) {
            statement.setString(1, ACTIVE_CHECKPOINT_KEY);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear active shell checkpoint from SQLite", exception);
        }
    }

    public boolean loadPlanModeEnabled() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select enabled
                     from shell_plan_mode
                     where singleton_key = ?
                     """)) {
            statement.setString(1, PLAN_MODE_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return resultSet.getInt("enabled") != 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load shell plan mode from SQLite", exception);
        }
    }

    public void savePlanModeEnabled(boolean enabled) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_plan_mode (singleton_key, enabled, saved_at)
                     values (?, ?, ?)
                     on conflict(singleton_key) do update set
                         enabled = excluded.enabled,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, PLAN_MODE_KEY);
            statement.setInt(2, enabled ? 1 : 0);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save shell plan mode to SQLite", exception);
        }
    }

    public void saveCheckpoint(String name, SessionContext sessionContext) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }

        SessionContext checkpointSnapshot = SessionContextBrancher.branch(sessionContext);
        sessionRepository.saveSession(checkpointSnapshot);

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_checkpoints (checkpoint_name, session_id, saved_at)
                     values (?, ?, ?)
                     on conflict(checkpoint_name) do update set
                         session_id = excluded.session_id,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, name);
            statement.setString(2, checkpointSnapshot.sessionId().toString());
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save shell checkpoint to SQLite", exception);
        }
    }

    public Optional<SessionContext> loadCheckpoint(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select session_id
                     from shell_checkpoints
                     where checkpoint_name = ?
                     """)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return sessionRepository.loadSession(SessionId.from(resultSet.getString("session_id")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load shell checkpoint from SQLite", exception);
        }
    }

    public List<String> listCheckpointNames() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select checkpoint_name
                     from shell_checkpoints
                     order by saved_at asc
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (resultSet.next()) {
                names.add(resultSet.getString("checkpoint_name"));
            }
            return List.copyOf(names);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list shell checkpoints from SQLite", exception);
        }
    }

    public Optional<String> loadActiveBranchName() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select branch_name
                     from shell_active_branch
                     where singleton_key = ?
                     """)) {
            statement.setString(1, ACTIVE_BRANCH_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getString("branch_name"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load active shell branch from SQLite", exception);
        }
    }

    public void saveActiveBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("branchName must not be blank");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_active_branch (singleton_key, branch_name, saved_at)
                     values (?, ?, ?)
                     on conflict(singleton_key) do update set
                         branch_name = excluded.branch_name,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, ACTIVE_BRANCH_KEY);
            statement.setString(2, branchName);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save active shell branch to SQLite", exception);
        }
    }

    public void clearActiveBranchName() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     delete from shell_active_branch
                     where singleton_key = ?
                     """)) {
            statement.setString(1, ACTIVE_BRANCH_KEY);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear active shell branch from SQLite", exception);
        }
    }

    public void saveBranch(String name, SessionContext sessionContext) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }

        sessionRepository.saveSession(sessionContext);

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shell_branches (branch_name, session_id, saved_at)
                     values (?, ?, ?)
                     on conflict(branch_name) do update set
                         session_id = excluded.session_id,
                         saved_at = excluded.saved_at
                     """)) {
            statement.setString(1, name);
            statement.setString(2, sessionContext.sessionId().toString());
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save shell branch to SQLite", exception);
        }
    }

    public Optional<SessionContext> loadBranch(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select session_id
                     from shell_branches
                     where branch_name = ?
                     """)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return sessionRepository.loadSession(SessionId.from(resultSet.getString("session_id")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load shell branch from SQLite", exception);
        }
    }

    public List<String> listBranchNames() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select branch_name
                     from shell_branches
                     order by saved_at asc
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (resultSet.next()) {
                names.add(resultSet.getString("branch_name"));
            }
            return List.copyOf(names);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list shell branches from SQLite", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
    }

    private void initializeSchema() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists shell_focus (
                        singleton_key text primary key,
                        focus_path text not null,
                        saved_at text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists shell_checkpoints (
                        checkpoint_name text primary key,
                        session_id text not null,
                        saved_at text not null,
                        foreign key (session_id) references sessions(session_id)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists shell_active_checkpoint (
                        singleton_key text primary key,
                        checkpoint_name text not null,
                        saved_at text not null,
                        foreign key (checkpoint_name) references shell_checkpoints(checkpoint_name)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists shell_plan_mode (
                        singleton_key text primary key,
                        enabled integer not null,
                        saved_at text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists shell_branches (
                        branch_name text primary key,
                        session_id text not null,
                        saved_at text not null,
                        foreign key (session_id) references sessions(session_id)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists shell_active_branch (
                        singleton_key text primary key,
                        branch_name text not null,
                        saved_at text not null,
                        foreign key (branch_name) references shell_branches(branch_name)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite shell state schema", exception);
        }
    }
}
