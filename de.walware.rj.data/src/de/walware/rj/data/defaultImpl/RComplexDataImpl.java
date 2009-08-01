/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import de.walware.rj.data.RStore;


public class RComplexDataImpl extends AbstractComplexData
		implements RDataResizeExtension {
	
	
	private double[] realValues;
	private double[] imaginaryValues;
	private int[] naIdxs;
	
	
	public RComplexDataImpl() {
		this.realValues = new double[0];
		this.imaginaryValues = new double[0];
		this.naIdxs = new int[0];
		this.length = 0;
	}
	
	public RComplexDataImpl(final double realValues[], final double[] imaginaryValues, final int[] naIdxs) {
		assert (realValues.length == imaginaryValues.length);
		this.realValues = realValues;
		this.imaginaryValues = imaginaryValues;
		this.naIdxs = naIdxs;
		for (int i = 0; i < realValues.length; i++) {
			if (Double.isNaN(realValues[i]) || Double.isNaN(imaginaryValues[i])) {
				realValues[i] = Double.NaN;
				imaginaryValues[i] = Double.NaN;
			}
		}
		this.length = realValues.length;
	}
	
	public RComplexDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naIdxs = new int[0];
		this.realValues = new double[this.length];
		this.imaginaryValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			final long l = in.readLong();
			if (l == NA_numeric_LONG) {
				this.realValues[i] = Double.NaN;
				this.naIdxs = addIdx(this.naIdxs, i);
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
			if (Double.isNaN(this.realValues[i])) {
				if (Arrays.binarySearch(this.naIdxs, i) >= 0) {
					out.writeLong(NA_numeric_LONG);
				}
				else {
					out.writeLong(NaN_numeric_LONG);
				}
			}
			else {
				out.writeLong(Double.doubleToLongBits(this.realValues[i]));
			}
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
				&& Arrays.binarySearch(this.naIdxs, idx) >= 0);
	}
	
	public boolean isNaN(final int idx) {
		return (Double.isNaN(this.realValues[idx])
				&& Arrays.binarySearch(this.naIdxs, idx) < 0);
	}
	
	public boolean isMissing(final int idx) {
		return (Double.isNaN(this.realValues[idx]));
	}
	
	@Override
	public void setCplx(final int idx, final double realValue, final double imaginaryValue) {
		if (Double.isNaN(realValue) || Double.isNaN(imaginaryValue)) {
			if (Double.isNaN(this.realValues[idx])) {
				this.naIdxs = deleteIdx(this.naIdxs, idx);
			}
			this.realValues[idx] = Double.NaN;
			this.imaginaryValues[idx] = Double.NaN;
		}
		else {
			if (Double.isNaN(this.realValues[idx])) {
				this.naIdxs = deleteIdx(this.naIdxs, idx);
			}
			this.realValues[idx] = realValue;
			this.imaginaryValues[idx] = imaginaryValue;
		}
	}
	
	@Override
	public void setNA(final int idx) {
		this.realValues[idx] = Double.NaN;
		this.imaginaryValues[idx] = Double.NaN;
		this.naIdxs = addIdx(this.naIdxs, idx);
	}
	
	private void prepareInsert(final int[] idxs) {
		this.realValues = prepareInsert(this.realValues, this.length, idxs);
		this.imaginaryValues = prepareInsert(this.imaginaryValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertCplx(final int idx, final double realValue, final double imaginaryValue) {
		final int[] idxs = new int[] { idx };
		prepareInsert(idxs);
		if (Double.isNaN(realValue) || Double.isNaN(imaginaryValue)) {
			this.realValues[idx] = Double.NaN;
			this.imaginaryValues[idx] = Double.NaN;
		}
		else {
			this.realValues[idx] = realValue;
			this.imaginaryValues[idx] = imaginaryValue;
		}
		updateIdxInserted(this.naIdxs, idxs);
	}
	
	public void insertNA(final int idx) {
		insertNA(new int[] { idx });
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.realValues[idx] = Double.NaN;
			this.imaginaryValues[idx] = Double.NaN;
		}
		this.naIdxs = insertIdx(this.naIdxs, idxs);
	}
	
	public void remove(final int idx) {
		remove(new int[] { idx });
	}
	
	public void remove(final int[] idxs) {
		this.realValues = remove(this.realValues, this.length, idxs);
		this.imaginaryValues = remove(this.imaginaryValues, this.length, idxs);
		this.length -= idxs.length;
		this.naIdxs = updateIdxRemoved(this.naIdxs, idxs);
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
