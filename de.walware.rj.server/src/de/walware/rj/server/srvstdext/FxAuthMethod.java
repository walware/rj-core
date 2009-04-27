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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import de.walware.rj.server.FxCallback;
import de.walware.rj.server.RjException;
import de.walware.rj.server.srvext.ServerAuthMethod;
import de.walware.rj.server.srvext.ServerUtil;


/**
 * Authentication method 'none'
 * without any authentication mechanism.
 */
public class FxAuthMethod extends ServerAuthMethod {
	
	
	private File file;
	private FileInputStream fileInputStream;
	
	private final byte[] pendingKey = new byte[1024];
	private FileChannel fileChannel;
	
	
	public FxAuthMethod() {
		super("fx", false);
	}
	
	
	@Override
	public void doInit(String arg) throws RjException {
		final String configType;
		final String configValue;
		{	final String[] args = ServerUtil.getArgConfigValue(arg);
			configType = args[0];
			configValue = args[1];
		}
		if (configType.equals("file")) {
			if (configValue == null || configValue.length() == 0) {
				throw new RjException("Missing lock file name.", null);
			}
			this.file = new File(configValue);
			
			try {
				if (!this.file.exists()) {
					this.file.createNewFile();
				}
				this.fileChannel = new RandomAccessFile(this.file, "rws").getChannel();
				this.fileChannel.truncate(512);
			}
			catch (IOException e) {
				throw new RjException("Cannot read lock file.", e);
			}
		}
		else {
			throw new RjException("Unsupported configuration type '"+configType+"'.", null);
		}
	}
	
	@Override
	protected Callback[] doCreateLogin() throws RjException {
		getRandom().nextBytes(this.pendingKey);
		try {
			this.fileChannel.position(this.fileChannel.size());
		}
		catch (IOException e) {
			throw new RjException("Cannot read lock file.", e);
		}
		
		return new Callback[] {
				new NameCallback("Username"),
				new FxCallback(this.file.getPath(), this.pendingKey),
		};
	}
	
	@Override
	protected String doPerformLogin(Callback[] callbacks) throws LoginException, RjException {
		final String userName = ((NameCallback) callbacks[0]).getName();
		byte[] clientKey = ((FxCallback) callbacks[1]).getContent();
		if (clientKey.length < 1024) {
			throw new RjException("Unsufficient client key");
		}
		try {
			if (compare(this.pendingKey) && compare(clientKey)) {
				return userName;
			}
		}
		catch (IOException e) {
			throw new RjException("Cannot read lock file.", e);
		}
		throw new FailedLoginException();
	}
	
	private boolean compare(byte[] key) throws IOException {
		byte[] check = new byte[key.length];
		int n = this.fileChannel.read(ByteBuffer.wrap(check));
		if (n != key.length) {
			return false;
		}
		return Arrays.equals(key, check);
	}
	
}
