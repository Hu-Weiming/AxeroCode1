package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;

/**
 * Keeps the current interactive CLI session in memory for the lifetime of the process.
 */
public class InMemorySessionStore implements SessionStore {

    private SessionContext currentSession = SessionContext.start();

    @Override
    public SessionContext currentSession() {
        return currentSession;
    }

    @Override
    public SessionContext replace(SessionContext sessionContext) {
        this.currentSession = sessionContext;
        return currentSession;
    }

    @Override
    public SessionContext reset() {
        this.currentSession = SessionContext.start();
        return currentSession;
    }
}
