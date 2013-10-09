/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
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


public class RIntegerDataFixLongImpl extends AbstractIntegerData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final int[][] intValues;
	
	
	public RIntegerDataFixLongImpl(final long length) {
		this.length = length;
		this.intValues = new2dIntArray(length, SEGMENT_LENGTH);
	}
	
	public RIntegerDataFixLongImpl(final int[][] values) {
		this.length = check2dArrayLength(values, SEGMENT_LENGTH);
		this.intValues = values;
	}
	
	
	public RIntegerDataFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.intValues = new2dIntArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.intValues.length; i++) {
			io.readIntData(this.intValues[i], this.intValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.intValues.length; i++) {
			io.writeIntData(this.intValues[i], this.intValues[i].length);
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
		return (this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == NA_integer_INT);
	}
	
	@Override
	public boolean isNA(final long idx) {
		return (this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == NA_integer_INT);
	}
	
	@Override
	public void setNA(final int idx) {
		this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				NA_integer_INT;
	}
	
	@Override
	public void setNA(final long idx) {
		this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				NA_integer_INT;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == NA_integer_INT);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		return (this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == NA_integer_INT);
	}
	
	@Override
	public int getInt(final int idx) {
		return this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public int getInt(final long idx) {
		return this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setInt(final int idx, final int value) {
//		assert (value != NA_integer_INT);
		this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				value;
	}
	
	@Override
	public void setInt(final long idx, final int value) {
//		assert (value != NA_integer_INT);
		this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				value;
	}
	
	
	@Override
	public Integer get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.intValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (v != NA_integer_INT) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.intValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (v != NA_integer_INT) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer[] toArray() {
		final int l = checkToArrayLength();
		final Integer[] array = new Integer[l];
		int k = 0;
		for (int i = 0; i < this.intValues.length; i++, k++) {
			final int[] ints = this.intValues[i];
			for (int j = 0; j < ints.length; j++) {
				final int v = ints[j];
				if (v != NA_integer_INT) {
					array[k] = Integer.valueOf(v);
				}
			}
		}
		return array;
	}
	
	
	@Override
	public final long indexOf(final int integer, long fromIdx) {
		if (integer == NA_integer_INT ) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		int i = (int) (fromIdx / SEGMENT_LENGTH);
		int j = (int) (fromIdx % SEGMENT_LENGTH);
		while (i < this.intValues.length) {
			final int[] ints = this.intValues[i];
			while (j < ints.length) {
				if (ints[i] == integer) {
					return (i * (long) SEGMENT_LENGTH) + j;
				}
			}
			i++;
			j = 0;
		}
		return -1;
	}
	
}
