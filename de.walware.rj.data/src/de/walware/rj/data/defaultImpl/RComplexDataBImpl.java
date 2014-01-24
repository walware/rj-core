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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RStore;


/**
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RComplexDataBImpl extends AbstractComplexData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected double[] realValues;
	protected double[] imaginaryValues;
	
	
	public RComplexDataBImpl() {
		this.length = 0;
		this.realValues = EMPTY_DOUBLE_ARRAY;
		this.imaginaryValues = EMPTY_DOUBLE_ARRAY;
	}
	
	public RComplexDataBImpl(final int length) {
		this.length = length;
		this.realValues = new double[length];
		this.imaginaryValues = new double[length];
	}
	
	protected RComplexDataBImpl(final double[] realValues, final double[] imaginaryValues) {
		this.length = realValues.length;
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
	}
	
	public RComplexDataBImpl(final double[] realValues, final double[] imaginaryValues, final int[] naIdxs) {
		if (realValues.length != imaginaryValues.length) {
			throw new IllegalArgumentException();
		}
		this.length = realValues.length;
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.realValues[naIdxs[i]] = NA_numeric_DOUBLE;
				this.imaginaryValues[naIdxs[i]] = NA_numeric_DOUBLE;
			}
		}
	}
	
	
	public RComplexDataBImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.realValues = new double[length];
		this.imaginaryValues = new double[length];
		io.readDoubleData(this.realValues, length);
		io.readDoubleData(this.imaginaryValues, length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeDoubleData(this.realValues, this.length);
		io.writeDoubleData(this.imaginaryValues, this.length);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.realValues = new double[this.length];
		this.imaginaryValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			this.realValues[i] = Double.longBitsToDouble(in.readLong());
			this.imaginaryValues[i] = Double.longBitsToDouble(in.readLong());
		}
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeLong(Double.doubleToRawLongBits(this.realValues[i]));
			out.writeLong(Double.doubleToRawLongBits(this.imaginaryValues[i]));
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
		this.imaginaryValues[idx] = NA_numeric_DOUBLE;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.realValues[(int) idx] = NA_numeric_DOUBLE;
		this.imaginaryValues[(int) idx] = NA_numeric_DOUBLE;
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
		final double value = this.realValues[(int) idx];
		return (Double.isNaN(value)
				&& (int) Double.doubleToRawLongBits(value) != NA_numeric_INT_MATCH);
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
	public double getCplxRe(final int idx) {
		return this.realValues[idx];
	}
	
	@Override
	public double getCplxRe(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.realValues[(int) idx];
	}
	
	@Override
	public double getCplxIm(final int idx) {
		return this.imaginaryValues[idx];
	}
	
	@Override
	public double getCplxIm(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.imaginaryValues[(int) idx];
	}
	
	@Override
	public void setCplx(final int idx, final double real, final double imaginary) {
		if (Double.isNaN(real) || Double.isNaN(imaginary)) {
			this.realValues[idx] = NaN_numeric_DOUBLE;
			this.imaginaryValues[idx] = NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[idx] = real;
			this.imaginaryValues[idx] = imaginary;
		}
	}
	
	@Override
	public void setCplx(final long idx, final double real, final double imaginary) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		if (Double.isNaN(real) || Double.isNaN(imaginary)) {
			this.realValues[(int) idx] = NaN_numeric_DOUBLE;
			this.imaginaryValues[(int) idx] = NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[(int) idx] = real;
			this.imaginaryValues[(int) idx] = imaginary;
		}
	}
	
	@Override
	public void setNum(final int idx, final double real) {
		if (Double.isNaN(real)) {
			this.realValues[idx] = NaN_numeric_DOUBLE;
			this.imaginaryValues[idx] = NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[idx] = real;
			this.imaginaryValues[idx] = 0.0;
		}
	}
	
	@Override
	public void setNum(final long idx, final double real) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		if (Double.isNaN(real)) {
			this.realValues[(int) idx] = NaN_numeric_DOUBLE;
			this.imaginaryValues[(int) idx] = NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[(int) idx] = real;
			this.imaginaryValues[(int) idx] = 0.0;
		}
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.realValues = prepareInsert(this.realValues, this.length, idxs);
		this.imaginaryValues = prepareInsert(this.imaginaryValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertCplx(final int idx, final double realValue, final double imaginaryValue) {
		prepareInsert(new int[] { idx });
		if (Double.isNaN(realValue) || Double.isNaN(imaginaryValue)) {
			this.realValues[idx] = NaN_numeric_DOUBLE;
			this.imaginaryValues[idx] = NaN_numeric_DOUBLE;
		}
		else {
			this.realValues[idx] = realValue;
			this.imaginaryValues[idx] = imaginaryValue;
		}
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = NA_numeric_DOUBLE;
		this.imaginaryValues[idx] = NA_numeric_DOUBLE;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.realValues[idx] = NA_numeric_DOUBLE;
			this.imaginaryValues[idx] = NA_numeric_DOUBLE;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.realValues = remove(this.realValues, this.length, new int[] { idx });
		this.imaginaryValues = remove(this.imaginaryValues, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.realValues = remove(this.realValues, this.length, idxs);
		this.imaginaryValues = remove(this.imaginaryValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Complex get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[idx];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			new Complex(v, this.imaginaryValues[idx]) :
			null;
	}
	
	@Override
	public Complex get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final double v = this.realValues[(int) idx];
		return (!Double.isNaN(v)
				|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) ?
			new Complex(v, this.imaginaryValues[(int) idx]) :
			null;
	}
	
	@Override
	public Complex[] toArray() {
		final double[] reals = this.realValues;
		final double[] imgs = this.imaginaryValues;
		final Complex[] array = new Complex[length()];
		for (int i = 0; i < array.length; i++) {
			final double v = reals[i];
			if (!Double.isNaN(v)
					|| (int) Double.doubleToRawLongBits(v) != NA_numeric_INT_MATCH) {
				array[i] = new Complex(v, imgs[i]);
			}
		}
		return array;
	}
	
	
	@Override
	public boolean allEqual(final RStore other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
