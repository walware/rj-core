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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RStore;


/**
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RRawDataImpl extends AbstractRawData
		implements RDataResizeExtension<Byte>, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected byte[] byteValues;
	
	
	public RRawDataImpl() {
		this.byteValues = EMPTY_BYTE_ARRAY;
		this.length = 0;
	}
	
	public RRawDataImpl(final int length) {
		this.byteValues = new byte[length];
		this.length = length;
	}
	
	public RRawDataImpl(final byte[] values) {
		this.byteValues = values;
		this.length = this.byteValues.length;
	}
	
	
	public RRawDataImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.byteValues = new byte[length];
		io.readByteData(this.byteValues, length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeByteData(this.byteValues, this.length);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.byteValues = new byte[this.length];
		in.readFully(this.byteValues, 0, this.length);
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		out.write(this.byteValues, 0, this.length);
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	protected final int length() {
		return this.length;
	}
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public byte getRaw(final int idx) {
		return this.byteValues[idx];
	}
	
	@Override
	public byte getRaw(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.byteValues[(int) idx];
	}
	
	@Override
	public void setRaw(final int idx, final byte value) {
		this.byteValues[idx] = value;
	}
	
	@Override
	public void setRaw(final long idx, final byte value) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.byteValues[(int) idx] = value;
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.byteValues = prepareInsert(this.byteValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insert(final int idx, final byte value) {
		prepareInsert(new int[] { idx });
		this.byteValues[idx] = value;
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.byteValues[idx] = NA_byte_BYTE;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.byteValues[idx] = NA_byte_BYTE;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.byteValues = remove(this.byteValues, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.byteValues = remove(this.byteValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Byte get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return Byte.valueOf(this.byteValues[idx]);
	}
	
	@Override
	public Byte get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return Byte.valueOf(this.byteValues[(int) idx]);
	}
	
	@Override
	public Byte[] toArray() {
		final Byte[] array = new Byte[length()];
		final byte[] raws = this.byteValues;
		for (int i = 0; i < array.length; i++) {
			array[i] = Byte.valueOf(raws[i]);
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE
				|| (integer & 0xffffff00) != 0 ) {
			return -1;
		}
		final byte raw = (byte) (integer & 0xff);
		final int l = length();
		final byte[] raws = this.byteValues;
		for (int i = (fromIdx >= 0) ? ((int) fromIdx) : 0; i < l; i++) {
			if (raws[i] == raw) {
				return i;
			}
		}
		return -1;
	}
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
