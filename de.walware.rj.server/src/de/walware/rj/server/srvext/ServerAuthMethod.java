/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvext;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import de.walware.rj.RjException;
import de.walware.rj.RjInitFailedException;
import de.walware.rj.server.ServerLogin;


/**
 * Abstract class for authentication methods.
 * 
 * An authentication method must extend this class and implement the
 * abstract methods:
 * <li>{@link #doInit(String)}</li>
 * <li>{@link #doCreateLogin()}</li>
 * <li>{@link #doPerformLogin(Callback[])}</li>
 */
public abstract class ServerAuthMethod {
	
	
	protected static void copyAnswer(final Callback[] from, final Callback[] to) throws UnsupportedCallbackException {
		assert (from.length == to.length);
		for (int i = 0; i < from.length; i++) {
			if (from[i] instanceof TextOutputCallback) {
				continue;
			}
			if (from[i] instanceof NameCallback) {
				((NameCallback) to[i]).setName(((NameCallback) from[i]).getName());
				continue;
			}
			if (from[i] instanceof PasswordCallback) {
				((PasswordCallback) to[i]).setPassword(((PasswordCallback) from[i]).getPassword());
				((PasswordCallback) from[i]).clearPassword();
				continue;
			}
			if (from[i] instanceof TextInputCallback) {
				((TextInputCallback) to[i]).setText(((TextInputCallback) from[i]).getText());
				continue;
			}
			if (from[i] instanceof ChoiceCallback) {
				final int[] selectedIndexes = ((ChoiceCallback) from[i]).getSelectedIndexes();
				if (((ChoiceCallback) from[i]).allowMultipleSelections()) {
					((ChoiceCallback) to[i]).setSelectedIndexes(selectedIndexes);
				}
				else if (selectedIndexes.length == 1) {
					((ChoiceCallback) to[i]).setSelectedIndex(selectedIndexes[0]);
				}
				continue;
			}
			if (from[i] instanceof ConfirmationCallback) {
				((ConfirmationCallback) to[i]).setSelectedIndex(((ConfirmationCallback) from[i]).getSelectedIndex());
				continue;
			}
			if (from[i] instanceof LanguageCallback) {
				((LanguageCallback) to[i]).setLocale(Locale.getDefault());
				continue;
			}
			throw new UnsupportedCallbackException(to[i]);
		}
	}
	
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.auth");
	private final String logPrefix;
	
	
	private final String id;
	
	private Random randomGenerator;
	
	private final boolean usePubkeyExchange;
	private KeyPairGenerator keyPairGenerator;
	
	private String pendingLoginClient;
	private long pendingLoginId;
	private KeyPair pendingLoginKeyPair;
	
	private String expliciteClient;
	
	
	/**
	 * 
	 * @param usePubkeyExchange enables default encryption of secret data (password)
	 */
	protected ServerAuthMethod(final String id, final boolean usePubkeyExchange) {
		this.usePubkeyExchange = usePubkeyExchange;
		this.id = id;
		this.logPrefix = "[Auth:"+id+"]";
	}
	
	
	public void setExpliciteClient(final String client) {
		this.expliciteClient = client;
	}
	
	private String getCallingClient() throws ServerNotActiveException {
		if (this.expliciteClient != null) {
			return this.expliciteClient;
		}
		return RemoteServer.getClientHost();
	}
	
	public boolean isValid(final Client client) {
		try {
			return (getCallingClient().equals(client.clientId));
		}
		catch (final ServerNotActiveException e) {
			return false;
		}
	}
	
	public final void init(final String arg) throws RjException {
		try {
			if (this.usePubkeyExchange) {
				this.keyPairGenerator = KeyPairGenerator.getInstance("RSA");
				this.keyPairGenerator.initialize(2048);
			}
			
			this.randomGenerator = new SecureRandom();
			
			doInit(arg);
		}
		catch (final Exception e) {
			final RjException rje = (e instanceof RjException) ? (RjException) e :
					new RjInitFailedException("An error occurred when initializing authentication method '"+this.id+"'.", e);
			throw rje;
		}
	}
	
	protected final Random getRandom() {
		return this.randomGenerator;
	}
	
	/**
	 * Is called when the server is started
	 * 
	 * @param arg configuration argument
	 * 
	 * @throws RjException
	 */
	protected abstract void doInit(String arg) throws RjException;
	
	public final ServerLogin createLogin() throws RjException {
		try {
			final String client = getCallingClient();
			final boolean same = client.equals(this.pendingLoginClient);
			this.pendingLoginClient = client;
			
			LOGGER.log(Level.INFO, "{0} creating new login ({1}).",
					new Object[] { this.logPrefix, client });
			
			long nextId;
			do {
				nextId = this.randomGenerator.nextLong();
			} while (nextId == this.pendingLoginId);
			this.pendingLoginId = nextId;
			if (this.usePubkeyExchange) {
				if (this.pendingLoginKeyPair == null || !same) {
					this.pendingLoginKeyPair = this.keyPairGenerator.generateKeyPair();
				}
			}
			
			return createNewLogin(doCreateLogin());
		}
		catch (final Exception e) {
			final RjException rje = (e instanceof RjException) ? (RjException) e :
					new RjException("An unexpected error occurred when preparing login process.", e);
			throw rje;
		}
	}
	
	/**
	 * Is called when client initiates a login process.
	 * 
	 * @return login callbacks to handle by the client
	 * 
	 * @throws RjException
	 */
	protected abstract Callback[] doCreateLogin() throws RjException;
	
	private ServerLogin createNewLogin(final Callback[] callbacks) {
		if (this.usePubkeyExchange) {
			return new ServerLogin(this.pendingLoginId, this.pendingLoginKeyPair.getPublic(), callbacks);
		}
		else {
			return new ServerLogin(this.pendingLoginId, null, callbacks);
		}
	}
	
	public final Client performLogin(final ServerLogin login) throws RjException, LoginException {
		String client = null;
		try {
			client = getCallingClient();
			if (login.getId() != this.pendingLoginId ||
					!client.equals(this.pendingLoginClient)) {
				throw new FailedLoginException("Login process was interrupted by another client.");
			}
			
			login.readAnswer(this.usePubkeyExchange ? this.pendingLoginKeyPair.getPrivate() : null);
			this.pendingLoginKeyPair = null;
			final String name = doPerformLogin(login.getCallbacks());
			
			LOGGER.log(Level.INFO, "{0} performing login completed successfull: {1} ({2}).",
					new Object[] { this.logPrefix, name, client });
			return new Client(name, getCallingClient(), (byte) 0);
		}
		catch (final Exception e) {
			if (e instanceof LoginException) {
				final LogRecord log = new LogRecord(Level.INFO, "{0} performing login failed ({1}).");
				log.setParameters(new Object[] { this.logPrefix, client });
				log.setThrown(e);
				LOGGER.log(log);
				throw (LoginException) e;
			}
			if (e instanceof RjException) {
				throw (RjException) e;
			}
			throw new RjException("An unexpected error occurred when validating the login credential.", e);
		}
		finally {
			System.gc();
		}
	}
	
	/**
	 * This method is called when the client sends the login data
	 * 
	 * @param callbacks the callbacks handled by the client (note, the callbacks are not
	 *     the same instances returned by {@link #createLogin()}, but clones)
	 * 
	 * @return login id like username
	 * 
	 * @throws LoginException if login failed (but can usually fixed by other login data)
	 * @throws RjException
	 */
	protected abstract String doPerformLogin(Callback[] callbacks) throws LoginException, RjException;
	
}
