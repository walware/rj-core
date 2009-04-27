/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.Serializable;


public class ServerInfo implements Serializable {
	
	
	private static final long serialVersionUID = -7708452527229108455L;
	
	
	private String name;
	private String ownerUser;
	private String directory;
	private int state;
	private String currentUser;
	
	
	public ServerInfo(String name, String ownerUser, String directory,
			int state, String currentUser) {
		this.name = name;
		this.ownerUser = ownerUser;
		this.directory = directory;
		this.state = state;
		this.currentUser = currentUser;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public String getOwnerUsername() {
		return this.ownerUser;
	}
	
	public String getDirectory() {
		return this.directory;
	}
	
	public int getState() {
		return this.state;
	}
	
	public String getCurrentUsername() {
		return this.currentUser;
	}
	
}
