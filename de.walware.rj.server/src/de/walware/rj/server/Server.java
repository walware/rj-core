/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

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
	 * Status code indicating, that the R engine is started and the client is connected but
	 * not active during the last minutes.
	 */
	public static final int S_CONNECTED_STALE = 0x00016;
	
	/**
	 * Status code indicating, that the R engine is started and the client was disconnected.
	 */
	public static final int S_DISCONNECTED = 	0x00018;
	
	/**
	 * Status code indicating, that R engine is started and the client-server connection was lost.
	 */
	public static final int S_LOST = 			0x00019;
	
	/**
	 * Status code indicating, that the server was stopped.
	 */
	public static final int S_STOPPED = 		0x0001a;
	
	public static final String C_CONSOLE_START = "console.start";
	
	public static final String C_CONSOLE_CONNECT = "console.connect";
	
	public static final String C_RSERVI_NODECONTROL = "rservi.nodecontrol";
	
	
	/**
	 * Triple of API version of the this server
	 * 
	 * @return the version number
	 * @throws RemoteException
	 */
	int[] getVersion() throws RemoteException;
	
	/**
	 * The current server information
	 * 
	 * The information represents the state this method is call
	 * and is not updated. To check for updates the method must be
	 * called again. 
	 * 
	 * @return a server information object
	 * @throws RemoteException
	 */
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
	 * to login ({@link #execute(String, Map, ServerLogin)})
	 * to run the specified command 
	 * 
	 * @param the execute command constant the login will be for
	 * @return the server login for the command
	 * @throws RemoteException
	 */
	ServerLogin createLogin(String command) throws RemoteException;
	
	
	/**
	 * Universal method to executes a server command
	 * 
	 * @param command a command, default are available as constants with C_ prefix. 
	 * @param login the login creditals a answer of {@link #createConsoleLogin()}
	 * @return the return value of the command, see command description
	 * @throws LoginException if login failed
	 * @throws RemoteException
	 */
	Object execute(String command, Map<String, ? extends Object> args, ServerLogin login) throws LoginException, RemoteException;
	
}
