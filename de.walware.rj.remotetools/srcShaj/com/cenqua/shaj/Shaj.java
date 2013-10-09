/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
package com.cenqua.shaj;

import com.cenqua.shaj.log.Log;

/**
 * A simple way of verifying username/passwords (authentication) and
 * checking group membership. Passwords and group membership is checked with
 * the underlying operating system (see below for a discussion on platform-specific behavior).
 *
 * <p>
 * The {@link #checkPassword(String, String, String)} and
 * {@link #checkGroupMembership(String, String, String)}
 * methods both require a "domain" parameter. The exact meaning of this parameter is
 * platform-specific. These static methods simply call through to the corresponding methods
 * in the default {@link Authenticator}.
 * </p>
 *
 * <p>
 * This class is multi-thread safe.
 * </p>
 *
 * <p>
 * <b>Note</b>: Shaj needs to load the "shaj" native library (e.g. "libshaj.so" on Linux, "shaj.dll" on win32, etc.).
 * If this process fails for any reason, an error is logged and most of the methods will throw <code>IllegalStateException</code>.
 * The {@link #init()} method can be called to determine if Shaj was sucessfully initialized.
 * </p>
 *
 * <p>
 * When necessary, Shaj performs logging using the {@link com.cenqua.shaj.log.Log} class.
 * </p>
 *
 * <p>
 * <b>Platform specific notes:</b>
 * For further information, see the specific implementation classes
 * {@link Win32Authenticator} and {@link PAMAuthenticator}.
 * </p>
 *
 */
public class Shaj {

    static boolean sInitOkay = false;

    static
    {
        try {
            System.loadLibrary("shaj");
            sInitOkay = initlibrary(Log.Factory.getInstance());
        } catch (Throwable e) {
            Log.Factory.getInstance().error("could not load native library, host-auth disabled", e);
        }
    }

    /**
     * Forces Shaj to load its required resources (native libraries, etc).
     * It is never necessary to call this method (Shaj will call this method itself when needed),
     * but calling this method early in your program gives you a chance to determine Shaj's status
     * at a convenient time.
     * This method may be called multiple times.
     *
     * @return true if Shaj was able to successfully initialize its platform-specific components.
     */
    public static boolean init() {
        return sInitOkay;
    }

    private static native boolean initlibrary(Log log);

    /**
     * Checks a user's password.
     * @param domain the (platform-specific) domain/service to used to perform the check.
     *   May be <code>null</code> (which has a platform-specific meaning).
     * @param username the username
     * @param password the password to verify
     * @return true if the password matches the username
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>password</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkPassword(String domain, String username, String password) {
        return Authenticator.getDefault().checkPassword(domain, username, password, Log.Factory.getInstance());
    }

    /**
     * Tests if a user is a member of a specific group.
     *
     * @param domain the (platform-specific) domain/service to used to perform the check.
     *   May be <code>null</code> (which has a platform-specific meaning).
     * @param username the username to test for membership
     * @param group the group to look in
     * @return true if the user is a member of the group
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>group</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkGroupMembership(String domain, String username, String group) {
        return Authenticator.getDefault().checkGroupMembership(domain, username, group, Log.Factory.getInstance());
    }

}
