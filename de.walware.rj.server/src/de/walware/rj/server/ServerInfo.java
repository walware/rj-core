/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
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


public class ServerInfo implements Serializable {
	
	
	private static final long serialVersionUID = -5411479269748201535L;
	
	public static final String USER_OWNER = "owner";
	public static final String USER_CONSOLE = "console";
	public static final String USER_RSERVI = "rservi";
	
	
	private final String name;
	private final String[] userTypes;
	private final String[] userNames;
	private final String directory;
	private final long timestamp;
	private final int state;
	
	
	public ServerInfo(final String name, final String directory, final long timestamp,
			final String[] userTypes, final String[] userNames,
			final int state) {
		this.name = name;
		this.userTypes = userTypes;
		this.userNames = userNames;
		this.directory = directory;
		this.timestamp = timestamp;
		this.state = state;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public String getUsername(final String type) {
		for (int i = 0; i < this.userTypes.length; i++) {
			if (this.userTypes[i].equals(type)) {
				return this.userNames[i];
			}
		}
		return null;
	}
	
	public String getDirectory() {
		return this.directory;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public int getState() {
		return this.state;
	}
	
}
