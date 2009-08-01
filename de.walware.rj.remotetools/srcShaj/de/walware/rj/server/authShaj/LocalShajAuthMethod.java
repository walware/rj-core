/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.authShaj;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import com.cenqua.shaj.Shaj;

import de.walware.rj.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;


/**
 * Authentication method 'local-shaj'
 * to authenticate against local user account.
 */
public class LocalShajAuthMethod extends ServerAuthMethod {
	
	
	private String[] users;
	
	
	public LocalShajAuthMethod() {
		super("local-shaj", true);
	}
	
	
	@Override
	public void doInit(final String arg) throws RjException {
		if (!Shaj.init()) {
			throw new RjException("Initializing authentication failed:\n" +
					"Initializing 'shaj'-library failed");
		}
		this.users = new String[] { System.getProperty("user.name") };
	}
	
	@Override
	protected Callback[] doCreateLogin() throws RjException {
		return new Callback[] {
				new NameCallback("Username"),
				new PasswordCallback("Password", false),
		};
	}
	
	@Override
	protected String doPerformLogin(final Callback[] callbacks) throws LoginException, RjException {
		final String loginName = ((NameCallback) callbacks[0]).getName();
		if (isValidUser(loginName)) {
			final char[] loginPassword = ((PasswordCallback) callbacks[1]).getPassword();
			if (Shaj.checkPassword(null, loginName, new String(loginPassword))) {
				return loginName;
			}
		}
		throw new FailedLoginException("Invalid username or password");
	}
	
	private boolean isValidUser(final String user) {
		for (final String s : this.users) {
			if (s.equals(user)) {
				return true;
			}
		}
		return false;
	}
	
}
