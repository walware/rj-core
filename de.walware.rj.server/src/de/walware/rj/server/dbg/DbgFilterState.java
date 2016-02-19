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


public class DbgFilterState implements RJIOExternalizable {
	
	
	private final int stepFilterState;
	
	
	public DbgFilterState(final boolean stepFilterEnabled) {
		this.stepFilterState= (stepFilterEnabled) ? 0x1 : 0x0;
	}
	
	public DbgFilterState(final RJIO io) throws IOException {
		this.stepFilterState= io.readInt();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.stepFilterState);
	}
	
	
	public boolean stepFilterEnabled() {
		return ((this.stepFilterState & 0x1) != 0);
	}
	
}
