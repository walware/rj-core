/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */

package com.cenqua.shaj;

import com.cenqua.shaj.log.Log;


/**
 * Base-class for platform-specific authentication.
 * 
 * <p>This class is multi-thread safe.</p>
 */
public abstract class Authenticator {
    private static Authenticator INSTANCE = null;

    /**
     * A singleton <code>Authenticator</code> for the underlying operating system.
     * @return a singleton instance
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static Authenticator getDefault() {
        if (INSTANCE == null) {
            if (!Shaj.init()) {
                throw new IllegalStateException("Shaj did not initialize correctly.");
            }
            if (Win32Authenticator.isSupported()) {
                INSTANCE = new Win32Authenticator();
            } else if (PAMAuthenticator.isSupported()) {
                INSTANCE = new PAMAuthenticator();
            } else {
                throw new IllegalStateException("Could not find any platform-specific support (tried win32 and PAM).");
            }
        }
        return INSTANCE;
    }

    /**
     * Checks a user's password.
     * @param domain the (platform-specific) domain/service to used to perform the check.
     *   May be <code>null</code> (which has a platform-specific meaning).
     * @param username the username
     * @param password the password to verify
     * @param log where to log errors/debugging
     * @return true if the password matches the username
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>password</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public abstract boolean checkPassword(String domain, String username, String password, Log log);

    /**
     * Tests if a user is a member of a specific group.
     *
     * @param domain the (platform-specific) domain/service to used to perform the check.
     *   May be <code>null</code> (which has a platform-specific meaning).
     * @param username the username to test for membership
     * @param group the group to look in
     * @param log where to log errors/debugging
     * @return true if the user is a member of the group
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>group</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public abstract boolean checkGroupMembership(String domain, String username, String group, Log log);
}
