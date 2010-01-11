/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
import java.rmi.server.Unreferenced;

import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.Server;
import de.walware.rj.server.srvext.Client;


public final class ConsoleEngineImpl implements ConsoleEngine, Unreferenced {
	
	
	private final Server publicServer;
	private final InternalEngine internalEngine;
	private final Client client;
	
	
	public ConsoleEngineImpl(final Server publicServer, final InternalEngine internalEngine, final Client client) {
		this.publicServer = publicServer;
		this.internalEngine = internalEngine;
		this.client = client;
	}
	
	
	public Server getPublic() throws RemoteException {
		return this.publicServer;
	}
	
	public void interrupt() throws RemoteException {
		this.internalEngine.interrupt(this.client);
	}
	
	public void disconnect() throws RemoteException {
		this.internalEngine.disconnect(this.client);
	}
	
	public RjsComObject runAsync(final RjsComObject com) throws RemoteException {
		return this.internalEngine.runAsync(this.client, com);
	}
	
	public RjsComObject runMainLoop(final RjsComObject com) throws RemoteException {
		return this.internalEngine.runMainLoop(this.client, com);
	}
	
	public boolean isClosed() throws RemoteException {
		return (this.internalEngine.getCurrentClient() != this.client);
	}
	
	public void unreferenced() {
		try {
			if (this.internalEngine.getCurrentClient() == this.client) {
				this.internalEngine.disconnect(this.client);
			}
		} catch (final Exception e) {}
	}
	
}
