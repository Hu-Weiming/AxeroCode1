package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;

/**
 * Abstraction for the current CLI conversation session, regardless of whether it is purely in memory or persisted.
 */
public interface SessionStore {

    SessionContext currentSession();

    SessionContext replace(SessionContext sessionContext);

    SessionContext reset();
}
