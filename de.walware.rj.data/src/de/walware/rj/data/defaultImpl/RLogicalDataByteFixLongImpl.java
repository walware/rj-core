/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;

import de.walware.rj.data.RJIO;


/**
 * Based on byte array.
 * 
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RLogicalDataByteFixLongImpl extends AbstractLogicalData
		implements ExternalizableRStore {
	
	
	public static final byte TRUE = TRUE_BYTE;
	public static final byte FALSE = FALSE_BYTE;
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	private final byte[][] boolValues;
	
	
	public RLogicalDataByteFixLongImpl(final long length) {
		this.length = length;
		this.boolValues = new2dByteArray(length, SEGMENT_LENGTH);
	}
	
	public RLogicalDataByteFixLongImpl(final byte[][] values) {
		this.length = check2dArrayLength(values, SEGMENT_LENGTH);
		this.boolValues = values;
	}
	
	
	public RLogicalDataByteFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.boolValues = new2dByteArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.boolValues.length; i++) {
			io.readByteData(this.boolValues[i], this.boolValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.boolValues.length; i++) {
			io.writeByteData(this.boolValues[i], this.boolValues[i].length);
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
	public boolean isNA(final int idx) {
		return (this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isNA(final long idx) {
		return (this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		return (this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == NA_logical_BYTE);
	}
	
	@Override
	public void setNA(final int idx) {
		this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				NA_logical_BYTE;
	}
	
	@Override
	public void setNA(final long idx) {
		this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				NA_logical_BYTE;
	}
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == TRUE_BYTE);
	}
	
	@Override
	public boolean getLogi(final long idx) {
		return (this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == TRUE_BYTE);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				(value) ? TRUE_BYTE : FALSE_BYTE;
	}
	
	@Override
	public void setLogi(final long idx, final boolean value) {
		this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				(value) ? TRUE_BYTE : FALSE_BYTE;
	}
	
	
	@Override
	public Boolean get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		switch(this.boolValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH]) {
		case TRUE_BYTE:
			return Boolean.TRUE;
		case FALSE_BYTE:
			return Boolean.FALSE;
		default:
			return null;
		}
	}
	
	@Override
	public Boolean get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		switch(this.boolValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)]) {
		case TRUE_BYTE:
			return Boolean.TRUE;
		case FALSE_BYTE:
			return Boolean.FALSE;
		default:
			return null;
		}
	}
	
	@Override
	public Boolean[] toArray() {
		final int l = checkToArrayLength();
		final Boolean[] array = new Boolean[l];
		int k = 0;
		for (int i = 0; i < this.boolValues.length; i++, k++) {
			final byte[] bools = this.boolValues[i];
			for (int j = 0; j < bools.length; j++) {
				switch(bools[j]) {
				case TRUE_BYTE:
					array[k] = Boolean.TRUE;
					continue;
				case FALSE_BYTE:
					array[k] = Boolean.FALSE;
					continue;
				default:
					continue;
				}
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOfNA(long fromIdx) {
		if (fromIdx < 0) {
			fromIdx= 0;
		}
		int i= (int) (fromIdx / SEGMENT_LENGTH);
		int j= (int) (fromIdx % SEGMENT_LENGTH);
		{	while (i < this.boolValues.length) {
				final byte[] bools= this.boolValues[i];
				while (j < bools.length) {
					if (bools[i] == NA_logical_BYTE) {
						return (i * (long) SEGMENT_LENGTH) + j;
					}
				}
				i++;
				j= 0;
			}
		}
		return -1;
	}
	
	@Override
	public long indexOf(final int integer, long fromIdx) {
		if (integer == NA_integer_INT ) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx= 0;
		}
		int i= (int) (fromIdx / SEGMENT_LENGTH);
		int j= (int) (fromIdx % SEGMENT_LENGTH);
		if (integer != 0) {
			while (i < this.boolValues.length) {
				final byte[] bools= this.boolValues[i];
				while (j < bools.length) {
					if (bools[i] == TRUE_BYTE) {
						return (i * (long) SEGMENT_LENGTH) + j;
					}
				}
				i++;
				j= 0;
			}
		}
		else {
			while (i < this.boolValues.length) {
				final byte[] bools= this.boolValues[i];
				while (j < bools.length) {
					if (bools[i] == FALSE_BYTE) {
						return (i * (long) SEGMENT_LENGTH) + j;
					}
				}
				i++;
				j= 0;
			}
		}
		return -1;
	}
	
}
