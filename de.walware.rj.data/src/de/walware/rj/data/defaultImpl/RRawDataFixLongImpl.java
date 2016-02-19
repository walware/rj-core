/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
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
import de.walware.rj.data.RStore;


public class RRawDataFixLongImpl extends AbstractRawData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final byte[][] byteValues;
	
	
	public RRawDataFixLongImpl(final long length) {
		this.length = length;
		this.byteValues = new2dByteArray(length, SEGMENT_LENGTH);
	}
	
	public RRawDataFixLongImpl(final byte[][] values) {
		this.length = check2dArrayLength(values, SEGMENT_LENGTH);
		this.byteValues = values;
	}
	
	
	public RRawDataFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.byteValues = new2dByteArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.byteValues.length; i++) {
			io.readByteData(this.byteValues[i], this.byteValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.byteValues.length; i++) {
			io.writeByteData(this.byteValues[i], this.byteValues[i].length);
		}
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public byte getRaw(final int idx) {
		return this.byteValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public byte getRaw(final long idx) {
		return this.byteValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setRaw(final int idx, final byte value) {
		this.byteValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				value;
	}
	
	@Override
	public void setRaw(final long idx, final byte value) {
		this.byteValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				value;
	}
	
	
	@Override
	public Byte get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return Byte.valueOf(this.byteValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH]);
	}
	
	@Override
	public Byte get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return Byte.valueOf(this.byteValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)]);
	}
	
	@Override
	public Byte[] toArray() {
		final int l = checkToArrayLength();
		final Byte[] array = new Byte[l];
		int k = 0;
		for (int i = 0; i < this.byteValues.length; i++, k++) {
			final byte[] raws = this.byteValues[i];
			for (int j = 0; j < raws.length; j++) {
				array[k] = Byte.valueOf(raws[j]);
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		if ((integer & 0xffffff00) != 0) {
			return -1;
		}
		final byte raw = (byte) (integer & 0xff);
		int i = (int) (fromIdx / SEGMENT_LENGTH);
		int j = (int) (fromIdx % SEGMENT_LENGTH);
		while (i < this.byteValues.length) {
			final byte[] raws = this.byteValues[i];
			while (j < raws.length) {
				if (raws[i] == raw) {
					return (i * (long) SEGMENT_LENGTH) + j;
				}
			}
			i++;
			j = 0;
		}
		return -1;
	}
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
