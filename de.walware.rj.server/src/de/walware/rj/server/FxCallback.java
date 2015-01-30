/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.io.Serializable;

import javax.security.auth.callback.Callback;


/**
 * Callback for authentication using file permission
 */
public class FxCallback implements Callback, Serializable {
	
	
	private static final long serialVersionUID = 3559656299612581181L;
	
	
	private final String filename;
	private byte[] content;
	
	
	public FxCallback(final String filename, final byte[] pendingKey) {
		this.filename = filename;
		this.content = pendingKey;
	}
	
	
	public String getFilename() {
		return this.filename;
	}
	
	public byte[] createContent(final byte[] clientKey) {
		if (clientKey == null) {
			throw new NullPointerException();
		}
		final byte[] newContent = new byte[this.content.length+clientKey.length];
		System.arraycopy(this.content, 0, newContent, 0, this.content.length);
		System.arraycopy(clientKey, 0, newContent, this.content.length, clientKey.length);
		this.content = clientKey;
		return newContent;
	}
	
	public byte[] getContent() {
		return this.content;
	}
	
}
