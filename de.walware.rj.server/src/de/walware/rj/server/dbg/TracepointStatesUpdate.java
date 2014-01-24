/*=============================================================================#
 # Copyright (c) 2011-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.dbg;

import java.io.IOException;
import java.util.List;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class TracepointStatesUpdate implements RJIOExternalizable {
	
	
	private final static int RESET =                        0x00000001;
	
	
	private final List<TracepointState> states;
	
	private final int properties;
	
	
	public TracepointStatesUpdate(final List<TracepointState> states,
			final boolean reset) {
		this.states = states;
		this.properties = (reset) ? RESET : 0;
	}
	
	public TracepointStatesUpdate(final RJIO io) throws IOException {
		this.properties = io.readInt();
		this.states = TracepointState.readList(io);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.properties);
		TracepointState.writeList(this.states, io);
	}
	
	
	public boolean getReset() {
		return ((this.properties & RESET) != 0);
	}
	
	public List<TracepointState> getStates() {
		return this.states;
	}
	
}
