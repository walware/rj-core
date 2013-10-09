/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
package com.cenqua.shaj;

import com.cenqua.shaj.log.Log;

/**
 * Checks passwords against the local PAM service.
 *
 * <p>PAM (Pluggable Authentication Modules) is common authentication mechanism
 * on many Unix systems (e.g. Linux, Solaris, OS-X).</p>
 *
 * <p>
 * Shaj needs to be told which <i>service name</i>
 * to use when conversing with PAM (this is the <code>domain</code> argument).
 * You can create a new service name in your PAM configuration
 * (typically /etc/pam.conf  or /etc/pam.d/), or tell Shaj
 * to use an existing service name (such as other, <code>login</code> or <code>xscreensaver</code>).
 * </p>
 *
 * <p>This class contains some static methods that can be used if you
 * wish to call to PAM directly.</p>
 *
 * <p>Some platform-specific advice for configuring PAM follows (assuming you want to create
 * a PAM service named <code>shaj</code>:)</p>
 *
 * <p><b>Linux</b>:
 * On many Linux distributions, you may need to create a <cpde>/etc/pam.d/shaj</code> file containing:</p>
 * <pre>auth       required     pam_stack.so service=system-auth</pre>
 *
 * <p><b>Mac OS-X</b>:
 *  On a default OS-X installation, you may need to create a <cpde>/etc/pam.d/shaj</code> file containing:</p>
 *<pre>auth       sufficient     pam_securityserver.so
 *auth       required       pam_deny.so</pre>
 *
 * <p><b>Solaris</b>:
 * If your are using the default <code>pam_unix_auth</code> PAM configuration on Solaris,
 * then you may need to add a line like this to your <code>/etc/pam.conf</code> file:</p>
 *<pre>shaj auth requisite          pam_authtok_get.so.1
 *shaj auth required           pam_unix_auth.so.1</pre>
 *
 * <p>If you test this and it does not work, it is probably because when using <code>pam_unix_auth</code> on Solaris,
 * the process doing the password check needs read access to /etc/shadow.
 * Giving the process Shaj is running in read access to this file may solve this problem, but using permissions
 * other than 0400 for <code>/etc/shadow</code> is not recommended.
 * You should discuss this with your system administrators first, and possibly change to a PAM module other than
 * <code>pam_unix_auth</code>.
 */
public class PAMAuthenticator extends Authenticator {

    static {
        Shaj.init();
    }

    /** lock object for calling getgrent(3) (which is not reentrant) */
    private static final Object GETGRENT_LOCK = new Object();

    private static native boolean isSupportedImpl(Log log);
    private static native boolean checkPasswordImpl(String service, String username, String password, Log log);
    private static native boolean checkGroupMembershipImpl(String username, String group, Log log);

    /**
     * Determines if this Authenticator can be used on the underlying platform.
     * @return true if this platform supports PAM authentication.
     */
    public static boolean isSupported() {
        return isSupported(Log.Factory.getInstance());
    }

    private static boolean isSupported(Log log) {
        if (!Shaj.sInitOkay) {
            return false;
        }
        return isSupportedImpl(log);
    }

    public boolean checkPassword(String domain, String username, String password, Log log) {
        return checkPAMPassword(domain, username, password, log);
    }

    public boolean checkGroupMembership(String domain, String username, String group, Log log) {
        return checkPAMGroupMembership(username, group, log);
    }

    /**
     * Checks a user's password in PAM.
     *
     * @param service the PAM service to use.
     *   May be <code>null</code> (in which case "other" is used).
     * @param username the username
     * @param password the password to verify
     * @param log where to log errors/debugging
     * @return true if the password matches the username
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>password</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkPAMPassword(String service, String username, String password, Log log) {
        if (service == null) {
            service = "other";
        }
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }
        if (!Shaj.sInitOkay) {
            throw new IllegalStateException("native library not loaded");
        }
        return checkPasswordImpl(service, username, password, log);
    }

    /**
     * Tests a user for membership in a unix group.
     *
     * <p><b>Note</b> The <code>getgrent(3)</code> system call is used to
     * test group membership, not PAM as the name of this method might suggest.
     * (PAM has no group-membership testing functions.)
     * </p>
     *
     * @param username the username to test for membership
     * @param group the group to look in
     * @param log where to log errors/debugging
     * @return true if the user is a member of the group
     * @throws IllegalArgumentException if <code>username</code>
     *  or <code>group</code> are <code>null</code>.
     * @throws IllegalStateException if Shaj did not load correctly (if {@link Shaj#init()} returns false).
     */
    public static boolean checkPAMGroupMembership(String username, String group, Log log) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if (group == null) {
            throw new IllegalArgumentException("group cannot be null");
        }
        if (!Shaj.sInitOkay) {
            throw new IllegalStateException("native library not loaded");
        }
        synchronized (GETGRENT_LOCK) {
            return checkGroupMembershipImpl(username, group, log);
        }
    }
}
