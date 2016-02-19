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
import de.walware.rj.data.RStore;


public class RComplexDataBFixLongImpl extends AbstractComplexData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final double[][] realValues;
	protected final double[][] imaginaryValues;
	
	
	public RComplexDataBFixLongImpl(final long length) {
		this.length = length;
		this.realValues = new2dDoubleArray(length, SEGMENT_LENGTH);
		this.imaginaryValues = new2dDoubleArray(length, SEGMENT_LENGTH);
	}
	
	public RComplexDataBFixLongImpl(final double[][] realValues, final double[][] imaginaryValues) {
		this.length = check2dArrayLength(realValues, SEGMENT_LENGTH);
		if (this.length != check2dArrayLength(imaginaryValues, SEGMENT_LENGTH)) {
			throw new IllegalArgumentException();
		}
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
	}
	
	
	public RComplexDataBFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.realValues = new2dDoubleArray(length, SEGMENT_LENGTH);
		this.imaginaryValues = new2dDoubleArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.realValues.length; i++) {
			io.readDoubleData(this.realValues[i], this.realValues[i].length);
			io.readDoubleData(this.imaginaryValues[i], this.imaginaryValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.realValues.length; i++) {
			io.writeDoubleData(this.realValues[i], this.realValues[i].length);
			io.writeDoubleData(this.imaginaryValues[i], this.imaginaryValues[i].length);
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
		this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				NA_numeric_DOUBLE;
	}
	
	@Override
	public void setNA(final long idx) {
		this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				NA_numeric_DOUBLE;
		this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
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
		final double value = this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (Double.isNaN(value)
				&& (int) Double.doubleToRawLongBits(value) != NA_numeric_INT_MATCH);
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
	public double getCplxRe(final int idx) {
		return this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public double getCplxRe(final long idx) {
		return this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public double getCplxIm(final int idx) {
		return this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public double getCplxIm(final long idx) {
		return this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setCplx(final int idx, final double real, final double imaginary) {
		if (Double.isNaN(real) || Double.isNaN(imaginary)) {
			this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					NaN_numeric_DOUBLE;
			this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					real;
			this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					imaginary;
		}
	}
	
	@Override
	public void setCplx(final long idx, final double real, final double imaginary) {
		if (Double.isNaN(real) || Double.isNaN(imaginary)) {
			this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					NaN_numeric_DOUBLE;
			this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					real;
			this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					imaginary;
		}
	}
	
	@Override
	public void setNum(final int idx, final double real) {
		if (Double.isNaN(real)) {
			this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					NaN_numeric_DOUBLE;
			this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					real;
			this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
					0.0;
		}
	}
	
	@Override
	public void setNum(final long idx, final double real) {
		if (Double.isNaN(real)) {
			this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					NaN_numeric_DOUBLE;
			this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					real;
			this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
					0.0;
		}
	}
	
	
	@Override
	public Complex get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			new Complex(v, this.imaginaryValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH]) :
			null;
	}
	
	@Override
	public Complex get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			new Complex(v, this.imaginaryValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)]) :
			null;
	}
	
	@Override
	public Complex[] toArray() {
		final int l = checkToArrayLength();
		final Complex[] array = new Complex[l];
		int k = 0;
		for (int i = 0; i < this.realValues.length; i++, k++) {
			final double[] reals = this.realValues[i];
			final double[] imgs = this.imaginaryValues[i];
			for (int j = 0; j < reals.length; j++) {
				final double v = reals[j];
				if (!Double.isNaN(v)
						|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) {
					array[k] = new Complex(v, imgs[i]);
				}
			}
		}
		return array;
	}
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
