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

import java.io.IOException;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import de.walware.rj.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;


/**
 * Draft for a JAAS auth method. 
 * Missing role/principal handling.
 */
public abstract class JaasAuthMethod extends ServerAuthMethod implements CallbackHandler, Runnable {
	
	
	private static String JAAS_NAME = "de.walware.rj.server";
	
	
	private class JaasConfig extends Configuration {
		
		
		private final AppConfigurationEntry entry;
		
		
		JaasConfig(final String clazz) {
			this.entry = new AppConfigurationEntry(clazz, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, new HashMap());
		}
		
		
		@Override
		public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
			return null;
		}
		
		@Override
		public void refresh() {
		}
		
	}
	
	
	private static final int STARTED = 1;
	private static final int CALLBACK = 2;
	private static final int PERFORM = 3;
	private static final int LOGGED_IN = 4;
	
	private static final int CANCEL = -1;
	private static final int FAILED = -2;
	
	
	private Configuration configuration;
	private LoginContext context;
	
	private Callback[] pendingLoginCallback;
	private int pendingLogin;
	private String pendingLoginMsg;
	
	private final Thread loginThread;
	
	
	protected JaasAuthMethod(final String id) {
		super(id, true);
		this.loginThread = new Thread(this);
	}
	
	
	@Override
	public void doInit(final String arg) throws RjException {
		this.configuration = Configuration.getConfiguration();
		if (this.configuration.getAppConfigurationEntry(JAAS_NAME) == null) {
			this.configuration = new JaasConfig(arg);
		}
		try {
			this.context = new LoginContext(JAAS_NAME, new Subject(), this, this.configuration);
		}
		catch (final LoginException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected Callback[] doCreateLogin() throws RjException {
		cancel();
		this.loginThread.start();
		synchronized (this) {
			while (this.pendingLogin >= 0 && this.pendingLogin < CALLBACK) {
				notifyAll();
				try {
					wait();
				}
				catch (final InterruptedException e) {}
			}
		}
		if (this.pendingLogin == FAILED) {
			throw new SecurityException("failed to init login", null);
		}
		return this.pendingLoginCallback;
	}
	
	@Override
	protected String doPerformLogin(final Callback[] callbacks) throws LoginException, RjException {
		this.pendingLoginCallback = callbacks;
		this.pendingLogin = PERFORM;
		synchronized (this) {
			while (this.loginThread.isAlive()) {
				notifyAll();
				try {
					wait();
				}
				catch (final InterruptedException e) {}
			}
		}
		if (this.pendingLogin != LOGGED_IN) {
			if (this.pendingLoginMsg != null) {
				throw new LoginException(this.pendingLoginMsg);
			}
			else {
				throw new SecurityException("", null);
			}
		}
		return null;
	}
	
	private void cancel() {
		this.pendingLoginCallback = null;
		this.pendingLoginMsg = null;
		this.pendingLogin = CANCEL;
		synchronized (this) {
			while (this.loginThread.isAlive()) {
				notifyAll();
				try {
					wait();
				}
				catch (final InterruptedException e) {}
			}
		}
		this.pendingLogin = 0;
	}
	
	public void run() {
		this.pendingLogin = STARTED;
		try {
			this.context.login();
			this.pendingLogin = LOGGED_IN;
			this.context.logout();
		}
		catch (final LoginException e) {
			this.pendingLogin = FAILED;
			this.pendingLoginMsg = e.getLocalizedMessage();
		}
		catch (final Throwable e) {
			this.pendingLogin = FAILED;
		}
		finally {
			synchronized (this) {
				notifyAll();
			}
		}
	}
	
	public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		if (this.pendingLogin == STARTED) {
			this.pendingLogin = CALLBACK;
			this.pendingLoginCallback = callbacks;
			
			synchronized (this) {
				while (this.pendingLogin == CALLBACK) {
					notifyAll();
					try {
						wait();
					}
					catch (final InterruptedException e) {}
				}
			}
			
			if (this.pendingLogin != PERFORM) {
				throw new IOException();
			}
			copyAnswer(this.pendingLoginCallback, callbacks);
		}
		else if (callbacks.length > 0){
			this.pendingLogin = FAILED;
			throw new UnsupportedCallbackException(callbacks[0]);
		}
	}
	
}
