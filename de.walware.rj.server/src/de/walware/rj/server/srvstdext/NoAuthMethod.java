/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvstdext;

import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import de.walware.rj.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;


/**
 * Authentication method 'none'
 * without any authentication mechanism.
 */
public class NoAuthMethod extends ServerAuthMethod {
	
	
	public NoAuthMethod() {
		super("none", false);
	}
	
	public NoAuthMethod(final String client) {
		super("none", false);
		setExpliciteClient(client);
		try {
			init("");
		}
		catch (final RjException e) {}
	}
	
	
	@Override
	public void doInit(final String arg) throws RjException {
	}
	
	@Override
	protected Callback[] doCreateLogin() throws RjException {
		return null;
	}
	
	@Override
	protected String doPerformLogin(final Callback[] callbacks) throws LoginException, RjException {
		return "-";
	}
	
}
