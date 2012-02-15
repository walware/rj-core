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

package de.walware.rj.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

import de.walware.rj.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;


public final class ServerLogin implements Serializable {
	
	
	private static final long serialVersionUID = -596748668244272719L;
	
	
	private long id;
	
	private Key pubkey;
	
	private Callback[] callbacks;
	
	
	public ServerLogin() {
	}
	
	public ServerLogin(final long id, final Key pubkey, final Callback[] callbacks) {
		this.id = id;
		this.pubkey = pubkey;
		this.callbacks = callbacks;
	}
	
	
	public long getId() {
		return this.id;
	}
	
	/**
	 * The callback items which should be handled by the client.
	 * 
	 * @return an array with all callback objects
	 * @see CallbackHandler
	 */
	public Callback[] getCallbacks() {
		return this.callbacks;
	}
	
	/**
	 * Must be called by the client to create login data for authentification
	 * when connecting to server via {@link Server#start(ServerLogin, String[])}
	 * or {@link Server#connect(ServerLogin)}.
	 * 
	 * @return the login data prepared to send to the server
	 * @throws RjException when creating login data failed
	 */
	public ServerLogin createAnswer() throws RjException {
		try {
			Callback[] copy;
			if (this.callbacks != null) {
				copy = new Callback[this.callbacks.length];
				System.arraycopy(this.callbacks, 0, copy, 0, this.callbacks.length);
				if (this.pubkey != null) {
					process(copy, Cipher.ENCRYPT_MODE, this.pubkey);
				}
			}
			else {
				copy = null;
			}
			return new ServerLogin(this.id, null, copy);
		}
		catch (final Exception e) {
			throw new RjException("An error occurred when creating login data.", e);
		}
	}
	
	/**
	 * Is called by server to decrypt data. By default it is called in
	 * {@link ServerAuthMethod#performLogin(ServerLogin)}.
	 * 
	 * @param privateKey the key to decrypt the data
	 * @throws RjException when processing login data failed
	 */
	public void readAnswer(final Key privateKey) throws RjException {
		try {
			if (privateKey != null) {
				process(this.callbacks, Cipher.DECRYPT_MODE, privateKey);
			}
		}
		catch (final Exception e) {
			throw new RjException("An error occurred when processing login data.", e);
		}
	}
	
	private void process(final Callback[] callbacks, final int mode, final Key key) throws Exception {
		final Cipher with = Cipher.getInstance("RSA");
		with.init(mode, key);
		final Charset charset = Charset.forName("UTF-8");
		
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof PasswordCallback) {
				final PasswordCallback c = (PasswordCallback) callbacks[i];
				final char[] orgPassword = c.getPassword();
				if (orgPassword != null) {
					final byte[] orgBytes;
					if (mode == Cipher.ENCRYPT_MODE) {
						orgBytes = charset.encode(CharBuffer.wrap(orgPassword)).array();
					}
					else {
						orgBytes = new byte[orgPassword.length];
						for (int j = 0; j < orgBytes.length; j++) {
							orgBytes[j] = (byte) orgPassword[j];
						}
					}
					final byte[] encBytes = with.doFinal(orgBytes);
					final char[] encPassword;
					if (mode == Cipher.ENCRYPT_MODE) {
						encPassword = new char[encBytes.length];
						for (int j = 0; j < encPassword.length; j++) {
							encPassword[j] = (char) encBytes[j];
						}
					}
					else {
						encPassword = charset.decode(ByteBuffer.wrap(encBytes)).array();
					}
					
					if (mode == Cipher.ENCRYPT_MODE) {
						final PasswordCallback copy = new PasswordCallback(c.getPrompt(), c.isEchoOn());
						copy.setPassword(encPassword);
						callbacks[i] = copy;
					}
					else {
						c.clearPassword();
						c.setPassword(encPassword);
					}
					
					Arrays.fill(orgBytes, (byte) 0);
					Arrays.fill(orgPassword, (char) 0);
					Arrays.fill(encBytes, (byte) 0);
					Arrays.fill(encPassword, (char) 0);
				}
				continue;
			}
		}
	}
	
	/**
	 * Clear data, especially the password.
	 */
	public void clearData() {
		this.pubkey = null;
		if (this.callbacks != null) {
			for (int i = 0; i < this.callbacks.length; i++) {
				if (this.callbacks[i] instanceof PasswordCallback) {
					((PasswordCallback) this.callbacks[i]).clearPassword();
				}
				this.callbacks[i] = null;
			}
			this.callbacks = null;
		}
	}
	
}
