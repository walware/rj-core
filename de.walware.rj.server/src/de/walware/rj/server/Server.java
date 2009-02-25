/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Interface of the R server visible for the remote clients.
 *
 */
public interface Server extends Remote {
	
	/**
	 * Status code indicating, that the server was stopped.
	 */
	public static final int S_STOPPED = 		0x00011;
	
	/**
	 * Status code indicating, that the client was disconnected.
	 */
	public static final int S_DISCONNECTED = 	0x00012;
	
	
	/**
	 * Starts the R server.
	 * 
	 * @param codeword
	 * @param args
	 * @return
	 * @throws RemoteException
	 *     - codeword is wrong
	 *     - the server was already started
	 *     - other remote error occured
	 */
	int start(long codeword, String[] args) throws RemoteException;
	
	
	int connect(long codeword) throws RemoteException;
	
	/**
	 * Tries to interrupt the current compution.
	 * 
	 * @param ticket
	 * @throws RemoteException
	 */
	void interrupt(int ticket) throws RemoteException;
	
	/**
	 * Disconnects the client from the server.
	 * The client can reconnect using {@link #connect(long)}.
	 * 
	 * @param ticket
	 * @throws RemoteException
	 */
	void disconnect(int ticket) throws RemoteException;
	
	RjsComObject runMainLoop(int ticket, RjsComObject com) throws RemoteException;
	
	RjsComObject runAsync(int ticket, RjsComObject com) throws RemoteException;
	
}
