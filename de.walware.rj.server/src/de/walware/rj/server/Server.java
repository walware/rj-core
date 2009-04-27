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

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.security.auth.login.LoginException;


/**
 * Interface of the R server visible for the remote clients.
 *
 */
public interface Server extends Remote {
	
	/**
	 * Status code indicating, that the R engine is not yet started.
	 */
	public static final int S_NOT_STARTED = 	0x00011;
	
	/**
	 * Status code indicating, that the R engine is started and a/the client is connected.
	 */
	public static final int S_CONNECTED = 		0x00014;
	
	/**
	 * Status code indicating, that the client was disconnected.
	 */
	public static final int S_DISCONNECTED = 	0x00018;
	
	/**
	 * Status code indicating, that the client-server connection was lost.
	 */
	public static final int S_LOST = 			0x00019;
	
	/**
	 * Status code indicating, that the server was stopped.
	 */
	public static final int S_STOPPED = 		0x0001a;
	
	
	ServerInfo getInfo() throws RemoteException;
	
	/**
	 * Current state of this server. One of the constants with S_ prefix.
	 * 
	 * @return current state
	 * @throws RemoteException
	 */
	int getState() throws RemoteException;
	
	
	/**
	 * Creates and returns the ServerLogin with all information necessary
	 * to login (#start of #connect)
	 * 
	 * @return ServerLogin with information what i
	 * @throws RemoteException
	 */
	ServerLogin createLogin() throws RemoteException;
	
	
	/**
	 * Starts the R server.
	 * 
	 * @param login
	 * @param args
	 * @return ticket
	 * @throws RemoteException
	 *     - the server was already started
	 *     - other remote error occured
	 * @throws LoginException 
	 */
	int start(ServerLogin login, String[] args) throws RemoteException, LoginException;
	
	/**
	 * 
	 * @param login
	 * @return ticket
	 * @throws RemoteException
	 * @throws LoginException
	 */
	int connect(ServerLogin login) throws RemoteException, LoginException;
	
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
