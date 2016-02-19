/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.srvImpl;

import java.rmi.RemoteException;
import java.util.Map;

import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.Server;
import de.walware.rj.server.srvext.Client;


public final class ConsoleEngineImpl implements ConsoleEngine {
	
	
	private final Server publicServer;
	private final InternalEngine internalEngine;
	private final Client client;
	
	
	public ConsoleEngineImpl(final Server publicServer, final InternalEngine internalEngine, final Client client) {
		this.publicServer = publicServer;
		this.internalEngine = internalEngine;
		this.client = client;
	}
	
	
	@Override
	public Server getPublic() throws RemoteException {
		return this.publicServer;
	}
	
	@Override
	public Map<String, Object> getPlatformData() {
		return this.internalEngine.getPlatformData();
	}
	
	@Override
	public void setProperties(final Map<String, ? extends Object> properties) throws RemoteException {
		this.internalEngine.setProperties(this.client, properties);
	}
	
	@Override
	public void disconnect() throws RemoteException {
		this.internalEngine.disconnect(this.client);
	}
	
	@Override
	public RjsComObject runAsync(final RjsComObject com) throws RemoteException {
		return this.internalEngine.runAsync(this.client, com);
	}
	
	@Override
	public RjsComObject runMainLoop(final RjsComObject com) throws RemoteException {
		return this.internalEngine.runMainLoop(this.client, com);
	}
	
	@Override
	public boolean isClosed() throws RemoteException {
		return (this.internalEngine.getCurrentClient() != this.client);
	}
	
}
