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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class TracepointEvent implements Tracepoint, RJIOExternalizable {
	
	
	public static final byte KIND_ABOUT_TO_HIT=            0x01;
	
	
	private final byte kind;
	
	private final int type;
	
	private final String filePath;
	private final long id;
	
	private final String label;
	private final int flags;
	private final String message;
	
	
	public TracepointEvent(final byte kind, final int type, final String filePath, final long id,
			final String label, final int flags, final String message) {
		this.kind= kind;
		this.type= type;
		this.filePath= filePath;
		this.id= id;
		this.label= label;
		this.flags= flags;
		this.message= message;
	}
	
	public TracepointEvent(final RJIO io) throws IOException {
		this.kind= io.readByte();
		this.type= io.readInt();
		this.filePath= io.readString();
		this.id= io.readLong();
		this.label= io.readString();
		this.flags= io.readInt();
		this.message= io.readString();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeByte(this.kind);
		io.writeInt(this.type);
		io.writeString(this.filePath);
		io.writeLong(this.id);
		io.writeString(this.label);
		io.writeInt(this.flags);
		io.writeString(this.message);
	}
	
	
	public byte getKind() {
		return this.kind;
	}
	
	@Override
	public int getType() {
		return this.type;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public long getId() {
		return this.id;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public int getFlags() {
		return this.flags;
	}
	
}
