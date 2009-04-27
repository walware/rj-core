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

package de.walware.rj.server.srvstdext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import de.walware.rj.server.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;
import de.walware.rj.server.srvext.ServerUtil;


/**
 * Authentication method 'name-pass'
 * to authenticate against a given name password pair.
 */
public class SimpleNamePassAuthMethod extends ServerAuthMethod {
	
	
	private Properties users;
	
	private byte[] digestSash;
	private MessageDigest digestService;
	private Charset digestCharset;
	
	
	public SimpleNamePassAuthMethod() {
		super("name-pass", true);
	}
	
	
	@Override
	public void doInit(String arg) throws RjException {
		final String configType;
		final String configValue;
		{	final String[] args = ServerUtil.getArgConfigValue(arg);
			configType = args[0];
			configValue = args[1];
		}
		try {
			this.digestSash = new byte[8];
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.nextBytes(this.digestSash);
			this.digestService = MessageDigest.getInstance("SHA-512");
			this.digestCharset = Charset.forName("UTF-8");
		}
		catch (Exception e) {
			throw new RjException("", e);
		}
		
		if (configType.equals("file")) {
			if (configValue == null || configValue.length() == 0) {
				throw new RjException("Missing password file name.", null);
			}
			File file = new File(configValue);
			this.users = new Properties();
			try {
				this.users.load(new FileInputStream(file));
			}
			catch (IOException e) {
				throw new RjException("Reading password file failed.", null);
			}
		}
		else {
			throw new RjException("Unsupported configuration type '"+configType+"'.", null);
		}
		this.digestService.update(this.digestSash);
		Set<Entry<Object,Object>> entrySet = this.users.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			byte[] password = this.digestService.digest(this.digestCharset.encode(
					(String) entry.getValue()).array());
			entry.setValue(password);
		}
		System.gc();
	}
	
	@Override
	protected Callback[] doCreateLogin() throws RjException {
		return new Callback[] {
				new NameCallback("Loginname"),
				new PasswordCallback("Password", false),
		};
	}
	
	@Override
	protected String doPerformLogin(Callback[] callbacks) throws LoginException, RjException {
		final String loginName = ((NameCallback) callbacks[0]).getName();
		Object object = this.users.get(loginName);
		if (object instanceof byte[]) {
			final byte[] loginPassword = getPass((PasswordCallback) callbacks[1]);
			if (Arrays.equals((byte[]) object, loginPassword)) {
				return loginName;
			}
		}
		throw new FailedLoginException("Invalid loginname or password");
	}
	
	private byte[] getPass(PasswordCallback callback) {
		final char[] loginPassword = callback.getPassword();
		final byte[] loginBytes;
		if (loginPassword == null) {
			return new byte[0];
		}
		this.digestService.update(this.digestSash);
		loginBytes = this.digestService.digest(this.digestCharset.encode(
				CharBuffer.wrap(loginPassword)).array());
		callback.clearPassword();
		Arrays.fill(loginPassword, (char) 0);
		return loginBytes;
	}
	
}
