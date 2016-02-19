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


public class RNumericDataBFixLongImpl extends AbstractNumericData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final double[][] realValues;
	
	
	public RNumericDataBFixLongImpl(final long length) {
		this.length = length;
		this.realValues = new2dDoubleArray(length, SEGMENT_LENGTH);
	}
	
	public RNumericDataBFixLongImpl(final double[][] values) {
		this.length = check2dArrayLength(values, SEGMENT_LENGTH);
		this.realValues = values;
	}
	
	
	public RNumericDataBFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.realValues = new2dDoubleArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.realValues.length; i++) {
			io.readDoubleData(this.realValues[i], this.realValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.realValues.length; i++) {
			io.writeDoubleData(this.realValues[i], this.realValues[i].length);
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
		final double v = this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) == NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isNA(final long idx) {
		final double v = this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) == NA_numeric_INT_MATCH);
	}
	
	@Override
	public void setNA(final int idx) {
		this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				NA_numeric_DOUBLE;
	}
	
	@Override
	public void setNA(final long idx) {
		this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				NA_numeric_DOUBLE;
	}
	
	@Override
	public boolean isNaN(final int idx) {
		final double v = this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isNaN(final long idx) {
		final double v = this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (Double.isNaN(v)
				&& (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH);
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (Double.isNaN(
				this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] ));
	}
	
	@Override
	public boolean isMissing(final long idx) {
		return (Double.isNaN(
				this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] ));
	}
	
	@Override
	public double getNum(final int idx) {
		return this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public double getNum(final long idx) {
		return this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setNum(final int idx, final double value) {
		this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				(Double.isNaN(value)) ? NaN_numeric_DOUBLE : value;
	}
	
	@Override
	public void setNum(final long idx, final double value) {
		this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				(Double.isNaN(value)) ? NaN_numeric_DOUBLE : value;
	}
	
	
	@Override
	public Double get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			Double.valueOf(v) :
			null;
	}
	
	@Override
	public Double get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			Double.valueOf(v) :
			null;
	}
	
	
	@Override
	public Double[] toArray() {
		final int l = checkToArrayLength();
		final Double[] array = new Double[l];
		int k = 0;
		for (int i = 0; i < this.realValues.length; i++, k++) {
			final double[] reals = this.realValues[i];
			for (int j = 0; j < reals.length; j++) {
				final double v = reals[j];
				if (!Double.isNaN(v)
						|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) {
					array[k] = Double.valueOf(v);
				}
			}
		}
		return array;
	}
	
}
