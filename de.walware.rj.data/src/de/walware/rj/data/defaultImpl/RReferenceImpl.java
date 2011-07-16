/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

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
	private int type;
	private String baseClassName;
	
	
	public RReferenceImpl(final long handler, final int type, final String baseClass) {
		this.handle = handler;
		this.baseClassName = baseClass;
	}
	
	public RReferenceImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		this.handle = io.readLong();
		this.type = io.readInt();
		this.baseClassName = io.readString();
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeLong(this.handle);
		io.writeInt(this.type);
		io.writeString(this.baseClassName);
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
