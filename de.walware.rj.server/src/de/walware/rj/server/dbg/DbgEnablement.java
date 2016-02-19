/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
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

package de.walware.rj.server.dbg;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class DbgEnablement implements RJIOExternalizable {
	
	
	private static final int ENABLE_BREAKPOINTS=           0x00000001;
	
	
	private final int properties;
	
	
	public DbgEnablement(final boolean breakpointsEnabled) {
		this.properties= (breakpointsEnabled) ? ENABLE_BREAKPOINTS : 0;
	}
	
	public DbgEnablement(final RJIO io) throws IOException {
		this.properties= io.readInt();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.properties);
	}
	
	
	public boolean getBreakpointsEnabled() {
		return ((this.properties & ENABLE_BREAKPOINTS) != 0);
	}
	
}
