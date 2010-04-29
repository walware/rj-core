/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
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


public class RComplexDataBImpl extends AbstractComplexData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	protected double[] realValues;
	protected double[] imaginaryValues;
	
	
	public RComplexDataBImpl() {
		this.realValues = EMPTY_DOUBLE_ARRAY;
		this.imaginaryValues = EMPTY_DOUBLE_ARRAY;
		this.length = 0;
	}
	
	public RComplexDataBImpl(final int length) {
		this.realValues = new double[length];
		this.imaginaryValues = new double[length];
		this.length = length;
	}
	
	public RComplexDataBImpl(final double[] realValues, final double[] imaginaryValues, final int[] naIdxs) {
		assert (realValues.length == imaginaryValues.length);
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
		this.length = realValues.length;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.realValues[naIdxs[i]] = NA_numeric_DOUBLE;
				this.imaginaryValues[naIdxs[i]] = NA_numeric_DOUBLE;
			}
		}
	}
	
	protected RComplexDataBImpl(final double[] realValues, final double[] imaginaryValues) {
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
		this.length = realValues.length;
	}
	
	public RComplexDataBImpl(final RJIO io) throws IOException {
		readExternal(io);
	}
	
	public void readExternal(final RJIO io) throws IOException {
		final ObjectInput in = io.in;
		this.length = in.readInt();
		this.realValues = new double[this.length];
		this.imaginaryValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			final long l = in.readLong();
			if (l == NA_numeric_LONG) {
				this.realValues[i] = NA_numeric_DOUBLE;
			}
			else {
				this.realValues[i] = Double.longBitsToDouble(l);
			}
			this.imaginaryValues[i] = Double.longBitsToDouble(in.readLong());
		}
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.realValues = new double[this.length];
		this.imaginaryValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			final long l = in.readLong();
			if (l == NA_numeric_LONG) {
				this.realValues[i] = NA_numeric_DOUBLE;
			}
			else {
				this.realValues[i] = Double.longBitsToDouble(l);
			}
			this.imaginaryValues[i] = Double.longBitsToDouble(in.readLong());
		}
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		final ObjectOutput out = io.out;
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeLong(Double.doubleToRawLongBits(this.realValues[i]));
			out.writeLong(Double.doubleToRawLongBits(this.imaginaryValues[i]));
		}
	}
	
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
	
	
	@Override
	public double getCplxRe(final int idx) {
		return this.realValues[idx];
	}
	
	@Override
	public double getCplxIm(final int idx) {
		return this.imaginaryValues[idx];
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (Double.isNaN(this.realValues[idx])
				&& Double.doubleToRawLongBits(this.realValues[idx]) == NA_numeric_LONG);
	}
	
	public boolean isNaN(final int idx) {
		return (Double.isNaN(this.realValues[idx])
				&& Double.doubleToRawLongBits(this.realValues[idx]) != NA_numeric_LONG);
	}
	
	public boolean isMissing(final int idx) {
		return (Double.isNaN(this.realValues[idx]));
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
	public void setNA(final int idx) {
		this.realValues[idx] = NA_numeric_DOUBLE;
		this.imaginaryValues[idx] = NA_numeric_DOUBLE;
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
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = NA_numeric_DOUBLE;
		this.imaginaryValues[idx] = NA_numeric_DOUBLE;
	}
	
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
	
	public void remove(final int idx) {
		this.realValues = remove(this.realValues, this.length, new int[] { idx });
		this.imaginaryValues = remove(this.imaginaryValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		this.realValues = remove(this.realValues, this.length, idxs);
		this.imaginaryValues = remove(this.imaginaryValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	public Object get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	public boolean allEqual(final RStore other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
