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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RS4Object;
import de.walware.rj.data.RStore;


public class RS4ObjectImpl extends AbstractRObject
		implements RS4Object, ExternalizableRObject {
	
	
	private String className;
	private int hasDataSlot;
	private RCharacterDataImpl slotNames;
	private RObject[] slotValues;
	
	
	public RS4ObjectImpl(final String className, final String[] slotNames, final RObject[] slotValues) {
		if (className == null || slotNames == null || slotValues == null) {
			throw new NullPointerException();
		}
		this.className = className;
		this.slotNames = new RCharacterDataImpl(slotNames);
		
		this.hasDataSlot = this.slotNames.indexOf(".Data");
		this.slotValues = slotValues;
	}
	
	public RS4ObjectImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		this.className = in.readUTF();
		this.hasDataSlot = in.readInt();
		this.slotNames = new RCharacterDataImpl(in);
		final int length = this.slotNames.getLength();
		this.slotValues = new RObject[length];
		for (int i = 0; i < length; i++) {
			this.slotValues[i] = factory.readObject(in, flags);
		}
	}
	
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		out.writeUTF(this.className);
		out.writeInt(this.hasDataSlot);
		this.slotNames.writeExternal(out);
		final int length = this.slotNames.getLength();
		for (int i = 0; i < length; i++) {
			factory.writeObject(this.slotValues[i], out, flags);
		}
	}
	
	public byte getRObjectType() {
		return TYPE_S4OBJECT;
	}
	
	public String getRClassName() {
		return this.className;
	}
	
	public boolean hasDataSlot() {
		return (this.hasDataSlot >= 0);
	}
	
	public RObject getDataSlot() {
		return (this.hasDataSlot >= 0) ? this.slotValues[this.hasDataSlot] : null;
	}
	
	public byte getDataType() {
		return (this.hasDataSlot >= 0 && this.slotValues[this.hasDataSlot] != null) ?
				this.slotValues[this.hasDataSlot].getData().getStoreType() : 0;
	}
	
	public RStore getData() {
		return (this.hasDataSlot >= 0 && this.slotValues[this.hasDataSlot] != null) ?
				this.slotValues[this.hasDataSlot].getData() :
				null;
	}
	
	public RCharacterStore getNames() {
		return this.slotNames;
	}
	
	public String getName(final int idx) {
		return this.slotNames.getChar(idx);
	}
	
	public RObject get(final int idx) {
		return this.slotValues[idx];
	}
	
	public RObject get(final String name) {
		if (this.hasDataSlot >= 0 && name.equals(".Data")) {
			return this.slotValues[this.hasDataSlot];
		}
		else {
			final int i = this.slotNames.indexOf(name);
			if (i >= 0) {
				return this.slotValues[i];
			}
		}
		throw new IllegalArgumentException();
	}
	
	public int getLength() {
		return this.slotValues.length;
	}
	
	public void insert(final int idx, final String name, final RObject component) {
		throw new UnsupportedOperationException();
	}
	
	public void add(final String name, final RObject component) {
		throw new UnsupportedOperationException();
	}
	
	public void remove(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public boolean set(final int idx, final RObject value) {
		this.slotValues[idx] = value;
		return true;
	}
	
	public boolean set(final String name, final RObject component) {
		if (this.hasDataSlot >= 0 && name.equals(".Data")) {
			this.slotValues[this.hasDataSlot] = component;
			return true;
		}
		else {
			final int i = this.slotNames.indexOf(name);
			if (i >= 0) {
				this.slotValues[i] = component;
				return true;
			}
		}
		return false;
	}
	
	public RObject[] toArray() {
		return null;
	}
	
	
	@Override
	public String toString() {
		return super.toString();
	}
	
}
