/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


/**
 * References a frame of the callstack.
 */
public abstract class FrameRef implements RJIOExternalizable {
	
	
	public static final class ByPosition extends FrameRef {
		
		
		private final int position;
		
		
		public ByPosition(final int position) {
			this.position= position;
		}
		
		public ByPosition(final RJIO in) throws IOException {
			this.position= in.readInt();
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
			out.writeInt(this.position);
		}
		
		
		public int getPosition() {
			return this.position;
		}
		
	}
	
	public static final class ByHandle extends FrameRef {
		
		
		private final long handle;
		
		
		public ByHandle(final long handle) {
			this.handle= handle;
		}
		
		public ByHandle(final RJIO in) throws IOException {
			this.handle= in.readLong();
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
			out.writeLong(this.handle);
		}
		
		
		public long getHandle() {
			return this.handle;
		}
	}
	
}
