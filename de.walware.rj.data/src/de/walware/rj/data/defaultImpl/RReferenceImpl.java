/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RStore;


public class RReferenceImpl implements RReference, ExternalizableRObject {
	
	
	private long handle;
	private int type;
	private String baseClassName;
	
	
	public RReferenceImpl(final long handler, final int type, final String baseClass) {
		this.handle = handler;
		this.baseClassName = baseClass;
	}
	
	public RReferenceImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		this.handle = in.readLong();
		this.type = in.readInt();
		this.baseClassName = in.readUTF();
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		out.writeLong(this.handle);
		out.writeInt(this.type);
		out.writeUTF(this.baseClassName);
	}
	
	
	public byte getRObjectType() {
		return TYPE_REFERENCE;
	}
	
	public int getReferencedType() {
		return this.type;
	}
	
	public String getRClassName() {
		return this.baseClassName;
	}
	
	public int getLength() {
		return 0;
	}
	
	public long getHandle() {
		return this.handle;
	}
	
	public RObject getResolvedRObject() {
		return null;
	}
	
	public RStore getData() {
		return null;
	}
	
	public RList getAttributes() {
		return null;
	}
	
}
