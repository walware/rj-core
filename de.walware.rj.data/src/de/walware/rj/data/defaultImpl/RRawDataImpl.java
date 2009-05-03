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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RRawStore;


public class RRawDataImpl extends AbstractRawData
		implements RRawStore, RDataReziseExtension, Externalizable {
	
	
	public static RRawDataImpl createForServer(final byte[] initialValues) {
		return new RRawDataImpl(initialValues);
	}
	
	
	private byte[] byteValues;
	
	
	public RRawDataImpl() {
		this.byteValues = new byte[0];
		this.length = 0;
	}
	
	public RRawDataImpl(final byte[] initialValues) {
		this.byteValues = initialValues;
		this.length = this.byteValues.length;
	}
	
	public RRawDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.byteValues = new byte[this.length];
		in.readFully(this.byteValues, 0, this.length);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		out.write(this.byteValues, 0, this.length);
	}
	
	
	@Override
	public byte getRaw(final int idx) {
		return this.byteValues[idx];
	}
	
	@Override
	public boolean hasNA() {
		return false;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return false;
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
	
	@Override
	public Byte[] toArray() {
		final Byte[] array = new Byte[this.length];
		for (int i = 0; i < this.length; i++) {
			array[i] = Byte.valueOf(this.byteValues[i]);
		}
		return array;
	}
	
}
