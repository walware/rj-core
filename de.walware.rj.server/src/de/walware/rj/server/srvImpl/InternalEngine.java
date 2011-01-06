/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvImpl;

import java.rmi.RemoteException;
import java.util.Map;

import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.srvext.Client;


public interface InternalEngine {
	
	
	int getState();
	Client getCurrentClient();
	Map<String, Object> getPlatformData();
	
	Object start(Client client, Map<String, ? extends Object> properties) throws RemoteException;
	Object connect(Client client, Map<String, ? extends Object> properties) throws RemoteException;
	void disconnect(Client client) throws RemoteException;
	void setProperties(Client client, Map<String, ? extends Object> properties) throws RemoteException;
	
	RjsComObject runMainLoop(Client client, RjsComObject com) throws RemoteException;
	RjsComObject runAsync(Client client, RjsComObject com) throws RemoteException;
	
	boolean interrupt(Client client) throws RemoteException;
	
}
