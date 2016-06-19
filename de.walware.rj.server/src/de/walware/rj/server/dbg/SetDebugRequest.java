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


public class SetDebugRequest implements RJIOExternalizable {
	
	public static final int FRAME=                          1 << 0;
	public static final int FUNCTION=                       1 << 1;
	
	private static final int ENABLED=                       1 << 0 << 24;
	
	private static final int TEMP=                          1 << 8;
	
	
	private final long frameId;
	private final String fName;
	
	private final int properties;
	
	
	public SetDebugRequest(final long frameId, final boolean enable, final boolean temp) {
		this.frameId= frameId;
		this.fName= null;
		int props= FRAME;
		if (enable) {
			props |= ENABLED;
		}
		if (temp) {
			props |= TEMP;
		}
		this.properties= props;
	}
	
	public SetDebugRequest(final int position, final String fName, final boolean enable, final boolean temp) {
		this.frameId= position;
		this.fName= fName;
		int props= FUNCTION;
		if (enable) {
			props |= ENABLED;
		}
		if (temp) {
			props |= TEMP;
		}
		this.properties= props;
	}
	
	public SetDebugRequest(final RJIO io) throws IOException {
		this.properties= io.readInt();
		switch (this.properties & 0xf) {
		case FRAME:
			this.frameId= io.readLong();
			this.fName= null;
			break;
		case FUNCTION:
			this.frameId= io.readInt();
			this.fName= io.readString();
			break;
		default:
			this.frameId= 0;
			this.fName= null;
			break;
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.properties);
		switch (this.properties & 0xf) {
		case FRAME:
			io.writeLong(this.frameId);
			break;
		case FUNCTION:
			io.writeInt((int) this.frameId);
			io.writeString(this.fName);
			break;
		default:
			break;
		}
	}
	
	
	public int getType() {
		return (this.properties & 0xf);
	}
	
	public long getHandle() {
		return this.frameId;
	}
	
	public int getPosition() {
		return (int) this.frameId;
	}
	
	public String getName() {
		return this.fName;
	}
	
	public boolean isTemp() {
		return ((this.properties & TEMP) != 0);
	}
	
	public int getDebug() {
		return ((this.properties & 0xff000000) >>> 24);
	}
	
}
