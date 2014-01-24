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
import java.util.Arrays;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class TracepointPosition implements Tracepoint, Comparable<TracepointPosition>,
		RJIOExternalizable {
	
	
	private final int type;
	
	private final long id;
	
	protected int[] exprIndex;
	protected int[] exprSrcref;
	
	
	public TracepointPosition(final int type, final long id,
			final int[] index, final int[] srcref) {
		this.type = type;
		this.id = id;
		this.exprIndex = index;
		this.exprSrcref = srcref;
	}
	
	TracepointPosition(final RJIO io) throws IOException {
		this.type = io.readInt();
		this.id = io.readLong();
		this.exprIndex = io.readIntArray();
		this.exprSrcref = io.readIntArray();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.type);
		io.writeLong(this.id);
		io.writeIntArray(this.exprIndex, this.exprIndex.length);
		io.writeIntArray(this.exprSrcref, (this.exprSrcref != null) ? 6 : -1);
	}
	
	
	@Override
	public int getType() {
		return this.type;
	}
	
	public long getId() {
		return this.id;
	}
	
	public int[] getIndex() {
		return this.exprIndex;
	}
	
	public int[] getSrcref() {
		return this.exprSrcref;
	}
	
	
	@Override
	public int compareTo(final TracepointPosition other) {
		for (int i = 0; i < this.exprIndex.length; i++) {
			if (i < other.exprIndex.length) {
				final int diff = this.exprIndex[i] - other.exprIndex[i];
				if (diff != 0) {
					return diff;
				}
				else {
					continue;
				}
			}
			return 1; // this deeper
		}
		if (this.exprIndex.length != other.exprIndex.length) {
			return -1; // other deeper
		}
		return this.type - other.type;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TracepointPosition)) {
			return false;
		}
		final TracepointPosition other = (TracepointPosition) obj;
		return (this.type == other.type
				&& Arrays.equals(this.exprIndex, other.exprIndex) );
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TracepointPosition");
		sb.append(" (type= ").append(this.type).append(")");
		sb.append("\n\t").append("exprIndex= ").append(Arrays.toString(this.exprIndex));
		sb.append("\n\t").append("exprSrcref= ").append(Arrays.toString(this.exprSrcref));
		return sb.toString();
	}
	
}
