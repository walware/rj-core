/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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
		
		this.dataSlotIdx = this.slotNames.indexOf(".Data", 0);
		this.slotValues = slotValues;
	}
	
	public RS4ObjectImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		//-- special attributes
		this.className = io.readString();
		//-- data
		final int l = (int) io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK));
		
		this.dataSlotIdx = io.readInt();
		this.slotNames = new RCharacterDataImpl(io, l);
		this.slotValues = new RObject[l];
		for (int i = 0; i < l; i++) {
			this.slotValues[i] = factory.readObject(io);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int l = this.slotValues.length;
		//-- options
		final int options = io.getVULongGrade(l);
		io.writeInt(options);
		//-- special attributes
		io.writeString(this.className);
		//-- data
		io.writeVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK), l);
		
		io.writeInt(this.dataSlotIdx);
		this.slotNames.writeExternal(io);
		for (int i = 0; i < l; i++) {
			factory.writeObject(this.slotValues[i], io);
		}
	}
	
	@Override
	public byte getRObjectType() {
		return TYPE_S4OBJECT;
	}
	
	@Override
	public String getRClassName() {
		return this.className;
	}
	
	
	@Override
	public long getLength() {
		return this.slotValues.length;
	}
	
	@Override
	public boolean hasDataSlot() {
		return (this.dataSlotIdx >= 0);
	}
	
	@Override
	public RObject getDataSlot() {
		return (this.dataSlotIdx >= 0) ? this.slotValues[this.dataSlotIdx] : null;
	}
	
	public byte getDataType() {
		return (this.dataSlotIdx >= 0 && this.slotValues[this.dataSlotIdx] != null) ?
				this.slotValues[this.dataSlotIdx].getData().getStoreType() : 0;
	}
	
	@Override
	public RStore<?> getData() {
		return (this.dataSlotIdx >= 0 && this.slotValues[this.dataSlotIdx] != null) ?
				this.slotValues[this.dataSlotIdx].getData() : null;
	}
	
	@Override
	public RCharacterStore getNames() {
		return this.slotNames;
	}
	
	@Override
	public String getName(final int idx) {
		return this.slotNames.getChar(idx);
	}
	
	@Override
	public String getName(final long idx) {
		return this.slotNames.getChar(idx);
	}
	
	@Override
	public RObject get(final int idx) {
		return this.slotValues[idx];
	}
	
	@Override
	public RObject get(final long idx) {
		if (idx < 0 || idx >= Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.slotValues[(int) idx];
	}
	
	@Override
	public RObject get(final String name) {
		final int idx = this.slotNames.indexOf(name, 0);
		if (idx >= 0) {
			return this.slotValues[idx];
		}
		throw new IllegalArgumentException();
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
			final int i = this.slotNames.indexOf(name, 0);
			if (i >= 0) {
				this.slotValues[i] = component;
				return true;
			}
		}
		return false;
	}
	
}
