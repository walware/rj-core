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

package de.walware.rj.server.srvext;

import de.walware.rj.server.Server;
import de.walware.rj.server.srvImpl.AbstractServerControl;
import de.walware.rj.server.srvImpl.RJClassLoader;


/**
 * Interface for {@link Server} for local (none-exported) methods
 */
public interface ExtServer {
	
	
	public void init(AbstractServerControl control, Server publicServer, RJClassLoader loader) throws Exception;
	
	public void addPlugin(ServerRuntimePlugin plugin);
	
	public void removePlugin(ServerRuntimePlugin plugin);
	
}
