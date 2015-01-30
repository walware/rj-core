/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RStore;


public class RReferenceImpl implements RReference, ExternalizableRObject {
	
	
	private long handle;
	private byte type;
	private String baseClassName;
	
	
	public RReferenceImpl(final long handler, final byte type, final String baseClass) {
		this.handle = handler;
		this.type = type;
		this.baseClassName = baseClass;
	}
	
	public RReferenceImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		this.handle = io.readLong();
		this.type = io.readByte();
		this.baseClassName = io.readString();
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeLong(this.handle);
		io.writeByte(this.type);
		io.writeString(this.baseClassName);
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_REFERENCE;
	}
	
	@Override
	public byte getReferencedRObjectType() {
		return this.type;
	}
	
	@Override
	public String getRClassName() {
		return this.baseClassName;
	}
	
	@Override
	public long getLength() {
		return 0;
	}
	
	@Override
	public long getHandle() {
		return this.handle;
	}
	
	@Override
	public RObject getResolvedRObject() {
		return null;
	}
	
	@Override
	public RStore<?> getData() {
		return null;
	}
	
	@Override
	public RList getAttributes() {
		return null;
	}
	
	
	@Override
	public int hashCode() {
		return (int) this.handle;
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (this == obj
				|| (obj instanceof RReference
						&& this.handle == ((RReference) obj).getHandle() ));
	}
	
}
