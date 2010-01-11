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

import de.walware.rj.data.RStore;


public class RComplexDataBImpl extends AbstractComplexData
		implements RDataResizeExtension, Externalizable {
	
	// TODO refactor to JRIComplex
	public static RComplexDataBImpl createFromJRI(final double[] realValues, final double[] imaginaryValues) {
		assert (realValues.length == imaginaryValues.length);
		for (int i = 0; i < imaginaryValues.length; i++) {
			if (Double.isNaN(imaginaryValues[i])) {
				if (Double.doubleToRawLongBits(imaginaryValues[i]) == NA_numeric_LONG) {
					realValues[i] = NA_numeric_DOUBLE;
					imaginaryValues[i] = NA_numeric_DOUBLE;
				}
				else {
					realValues[i] = NaN_numeric_DOUBLE;
					imaginaryValues[i] = NaN_numeric_DOUBLE;
				}
			}
		}
		return new RComplexDataBImpl(realValues, imaginaryValues);
	}
	
	
	protected double[] realValues;
	protected double[] imaginaryValues;
	
	
	public RComplexDataBImpl() {
		this.realValues = new double[0];
		this.imaginaryValues = new double[0];
		this.length = 0;
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
	
	private RComplexDataBImpl(final double[] realValues, final double[] imaginaryValues) {
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
		this.length = realValues.length;
	}
	
	public RComplexDataBImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
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
	
	
	public double getR(final int idx) {
		return this.realValues[idx];
	}
	
	public double getI(final int idx) {
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
	public void setCplx(final int idx, final double realValue, final double imaginaryValue) {
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
