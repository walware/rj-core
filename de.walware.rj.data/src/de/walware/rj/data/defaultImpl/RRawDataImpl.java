/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RStore;


public class RRawDataImpl extends AbstractRawData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	protected byte[] byteValues;
	
	
	public RRawDataImpl() {
		this.byteValues = EMPTY_BYTE_ARRAY;
		this.length = 0;
	}
	
	public RRawDataImpl(final int length) {
		this.byteValues = new byte[length];
		this.length = length;
	}
	
	public RRawDataImpl(final byte[] initialValues) {
		this.byteValues = initialValues;
		this.length = this.byteValues.length;
	}
	
	public RRawDataImpl(final RJIO io) throws IOException {
		readExternal(io);
	}
	
	public void readExternal(final RJIO io) throws IOException {
		this.byteValues = io.readByteArray();
		this.length = this.byteValues.length;
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.byteValues = new byte[this.length];
		in.readFully(this.byteValues, 0, this.length);
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeByteArray(this.byteValues, this.length);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		out.write(this.byteValues, 0, this.length);
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public byte getRaw(final int idx) {
		return this.byteValues[idx];
	}
	
	@Override
	public final int getInt(final int idx) {
		return (this.byteValues[idx] & 0xff);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return false;
	}
	
	public boolean isMissing(final int idx) {
		return false;
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		this.byteValues[idx] = ((integer & 0xffffff00) == 0) ? (byte) integer : NA_byte_BYTE;
	}
	
	@Override
	public void setRaw(final int idx, final byte value) {
		this.byteValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		this.byteValues[idx] = NA_byte_BYTE;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.byteValues = prepareInsert(this.byteValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insert(final int idx, final byte value) {
		prepareInsert(new int[] { idx });
		this.byteValues[idx] = value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.byteValues[idx] = NA_byte_BYTE;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.byteValues[idx] = NA_byte_BYTE;
		}
	}
	
	public void remove(final int idx) {
		this.byteValues = remove(this.byteValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		this.byteValues = remove(this.byteValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	public Byte get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException();
		}
		return Byte.valueOf(this.byteValues[idx]);
	}
	
	@Override
	public Byte[] toArray() {
		final Byte[] array = new Byte[this.length];
		for (int i = 0; i < this.length; i++) {
			array[i] = Byte.valueOf(this.byteValues[i]);
		}
		return array;
	}
	
	public boolean allEqual(final RStore other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
