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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;


/**
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RNumericDataBImpl extends AbstractNumericData
		implements RDataResizeExtension<Double>, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected double[] realValues;
	
	
	public RNumericDataBImpl() {
		this.length = 0;
		this.realValues = EMPTY_DOUBLE_ARRAY;
	}
	
	public RNumericDataBImpl(final int length) {
		this.length = length;
		this.realValues = new double[length];
	}
	
	public RNumericDataBImpl(final double[] values) {
		this.length = values.length;
		this.realValues = values;
	}
	
	public RNumericDataBImpl(final double[] values, final int[] naIdxs) {
		this.length = values.length;
		this.realValues = values;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.realValues[naIdxs[i]] = NA_numeric_DOUBLE;
			}
		}
	}
	
	
	public RNumericDataBImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.realValues = new double[length];
		io.readDoubleData(this.realValues, length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeDoubleData(this.realValues, this.length);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.realValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			this.realValues[i] = Double.longBitsToDouble(in.readLong());
		}
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeLong(Double.doubleToRawLongBits(this.realValues[i]));
		}
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
	public boolean isNA(final int idx) {
		final double v = this.realValues[idx];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) == NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) idx];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) == NA_numeric_INT_MATCH);
	}
	
	@Override
	public void setNA(final int idx) {
		this.realValues[idx] = NA_numeric_DOUBLE;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.realValues[(int) idx] = NA_numeric_DOUBLE;
	}
	
	@Override
	public boolean isNaN(final int idx) {
		final double v = this.realValues[idx];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isNaN(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) idx];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (Double.isNaN(this.realValues[idx]));
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (Double.isNaN(this.realValues[(int) idx]));
	}
	
	@Override
	public double getNum(final int idx) {
		return this.realValues[idx];
	}
	
	@Override
	public double getNum(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.realValues[(int) idx];
	}
	
	@Override
	public void setNum(final int idx, final double value) {
		this.realValues[idx] = (Double.isNaN(value)) ? NaN_numeric_DOUBLE : value;
	}
	
	@Override
	public void setNum(final long idx, final double value) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.realValues[(int) idx] = (Double.isNaN(value)) ? NaN_numeric_DOUBLE : value;
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.realValues = prepareInsert(this.realValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertNum(final int idx, final double value) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = (Double.isNaN(value)) ? NaN_numeric_DOUBLE : value;
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = NA_numeric_DOUBLE;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.realValues[idx] = NA_numeric_DOUBLE;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.realValues = remove(this.realValues, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.realValues = remove(this.realValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Double get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[idx];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
						Double.valueOf(v) : null;
	}
	
	@Override
	public Double get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) idx];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
						Double.valueOf(v) : null;
	}
	
	
	@Override
	public Double[] toArray() {
		final Double[] array = new Double[length()];
		final double[] reals = this.realValues;
		for (int i = 0; i < array.length; i++) {
			final double v = reals[i];
			if (!Double.isNaN(v)
					|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) {
				array[i] = Double.valueOf(v);
			}
		}
		return array;
	}
	
}
