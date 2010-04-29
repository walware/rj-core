/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RS4Object;
import de.walware.rj.data.RStore;


public class RS4ObjectImpl extends AbstractRObject
		implements RS4Object, ExternalizableRObject {
	
	
	private String className;
	
	private int dataSlotIdx;
	private RCharacterDataImpl slotNames;
	private RObject[] slotValues;
	
	
	public RS4ObjectImpl(final String className, final String[] slotNames, final RObject[] slotValues) {
		if (className == null || slotNames == null || slotValues == null) {
			throw new NullPointerException();
		}
		this.className = className;
		this.slotNames = new RCharacterDataImpl(slotNames);
		
		this.dataSlotIdx = this.slotNames.indexOf(".Data");
		this.slotValues = slotValues;
	}
	
	public RS4ObjectImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		this.className = io.readString();
		this.dataSlotIdx = io.in.readInt();
		this.slotNames = new RCharacterDataImpl(io);
		final int length = this.slotNames.getLength();
		this.slotValues = new RObject[length];
		for (int i = 0; i < length; i++) {
			this.slotValues[i] = factory.readObject(io);
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeString(this.className);
		io.out.writeInt(this.dataSlotIdx);
		this.slotNames.writeExternal(io);
		final int length = this.slotNames.getLength();
		for (int i = 0; i < length; i++) {
			factory.writeObject(this.slotValues[i], io);
		}
	}
	
	public byte getRObjectType() {
		return TYPE_S4OBJECT;
	}
	
	public String getRClassName() {
		return this.className;
	}
	
	
	public int getLength() {
		return this.slotValues.length;
	}
	
	public boolean hasDataSlot() {
		return (this.dataSlotIdx >= 0);
	}
	
	public RObject getDataSlot() {
		return (this.dataSlotIdx >= 0) ? this.slotValues[this.dataSlotIdx] : null;
	}
	
	public byte getDataType() {
		return (this.dataSlotIdx >= 0 && this.slotValues[this.dataSlotIdx] != null) ?
				this.slotValues[this.dataSlotIdx].getData().getStoreType() : 0;
	}
	
	public RStore getData() {
		return (this.dataSlotIdx >= 0 && this.slotValues[this.dataSlotIdx] != null) ?
				this.slotValues[this.dataSlotIdx].getData() : null;
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
		final int idx = this.slotNames.indexOf(name);
		if (idx >= 0) {
			return this.slotValues[idx];
		}
		throw new IllegalArgumentException();
	}
	
	public final RObject[] toArray() {
		final RObject[] array = new RObject[this.slotValues.length];
		System.arraycopy(this.slotValues, 0, array, 0, this.slotValues.length);
		return array;
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
		if (this.dataSlotIdx >= 0 && name.equals(".Data")) {
			this.slotValues[this.dataSlotIdx] = component;
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
	
}
