/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */

package com.cenqua.shaj;

import com.cenqua.shaj.log.Log;


/**
 * Checks user passwords and group-memebership with a Windows domain or computer.
 *
 * <p><b>Note:</b>
 * For the <code>domain</code> argument, it is best to use the full DNS name of the domain.
 * For example, <code>corp.example.com</code>. Using the short version (e.g. <code>corp</code>)
 * may work for {@link #checkWin32Password(String, String, String, com.cenqua.shaj.log.Log)}
 * but not for {@link #checkWin32GroupMembership(String, String, String, com.cenqua.shaj.log.Log)}.
 * </p>
 * 
 * <p>This class contains some static methods that can be used if you
 * wish to call to Windows directly.</p>
 */
public class Win32Authenticator extends Authenticator {

    static {
        Shaj.init();
    }
    
    private static native boolean isSupportedImpl(Log log);
    private static native boolean checkPasswordImpl(String domain, String username, String password, Log log);
    private static native boolean checkGroupMembershipImpl(String domain, String username, String group, Log log);

    /**
     * Determines if this Authenticator can be used on the underlying platform.
     * @return true if this platform supports win32 authentication.
     */
    public static boolean isSupported() {
        return isSupported(Log.Factory.getInstance());
    }

    private static boolean isSupported(final Log log) {
        if (!Shaj.sInitOkay) {
            return false;
        }
        return isSupportedImpl(log);
    }

    @Override
	public boolean checkPassword(final String domain, final String username, final String password, final Log log) {
        return checkWin32Password(domain, username, password, log);
    }

    @Override
	public boolean checkGroupMembership(final String domain, final String username, final String group, final Log log) {
        return checkWin32GroupMembership(domain, username, group, log);
    }

    /**
     * Verify a users password against a domain.
     *
     * <p><b>Note:</b> Windows appears to ignore domain if the computer is not part of a domain</p>
     *
     * @param domain the windows domain to check against.
     *   If domain is <code>null</code>, then the local computer (or the domain it is attached to) is checked.
     * @param username the username
     * @param password the password to verify
     * @param log where to log errors/debugging
     * @return true if the password matches the username
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>password</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkWin32Password(final String domain, final String username, final String password, final Log log) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }
        if (!Shaj.sInitOkay) {
            throw new IllegalStateException("native library not loaded");
        }
        return checkPasswordImpl(domain, username, password, log);
    }

    /**
     * Checks if a user is a member of a group. Domain groups are searched first, then local groups (including
     * indirect membership).
     * Groups are matched by name (as opposed to a SSID).
     * <p><b>Note:</b> the <code>domain</code> argument should be the fully qualifyied DNS name of the domain.</p>
     *
     * @param domain the windows domain to check against.
     *   If domain is <code>null</code>, then the local computer (or the domain it is attached to) is checked.
     * @param username the username to test for membership
     * @param group the group to look in
     * @param log where to log errors/debugging
     * @return true if the user is a member of the group
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>password</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkWin32GroupMembership(final String domain, final String username, final String group, final Log log) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if (group == null) {
            throw new IllegalArgumentException("group cannot be null");
        }
        if (!Shaj.sInitOkay) {
            throw new IllegalStateException("native library not loaded");
        }
        return checkGroupMembershipImpl(domain, username, group, log);
    }

}
