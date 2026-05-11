package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import org.springframework.stereotype.Component;

/**
 * Production CLI session store that restores and persists the current conversation through SQLite.
 */
@Component
public class SqliteBackedSessionStore implements SessionStore {

    private final SqliteSessionRepository repository;
    private SessionContext currentSession;

    public SqliteBackedSessionStore(SqliteSessionRepository repository) {
        this.repository = repository;
        this.currentSession = repository.loadCurrentSession()
                .orElseGet(() -> {
                    SessionContext createdSession = SessionContext.start();
                    repository.saveCurrentSession(createdSession);
                    return createdSession;
                });
    }

    @Override
    public synchronized SessionContext currentSession() {
        return currentSession;
    }

    @Override
    public synchronized SessionContext replace(SessionContext sessionContext) {
        repository.saveCurrentSession(sessionContext);
        currentSession = sessionContext;
        return currentSession;
    }

    @Override
    public synchronized SessionContext reset() {
        SessionContext resetSession = SessionContext.start();
        repository.saveCurrentSession(resetSession);
        currentSession = resetSession;
        return currentSession;
    }
}
