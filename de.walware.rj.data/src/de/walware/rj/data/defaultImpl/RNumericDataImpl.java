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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import de.walware.rj.data.RNumericStore;


public class RNumericDataImpl extends AbstractNumericData
		implements RNumericStore, RDataReziseExtension, Externalizable {
	
	
	public static RNumericDataImpl createFromWithNAList(final double[] values, final int[] naIdxs) {
		return new RNumericDataImpl(values, naIdxs);
	}
	
	
	private double[] realValues;
	private int[] naIdxs;
	
	
	public RNumericDataImpl() {
		this.realValues = new double[0];
		this.naIdxs = new int[0];
	}
	
	private RNumericDataImpl(final double[] values, final int[] naIdxs) {
		this.realValues = values;
		this.naIdxs = naIdxs;
	}
	
	
	public RNumericDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naIdxs = new int[0];
		this.realValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			final long l = in.readLong();
			if (l == NA_numeric_LONG) {
				this.realValues[i] = Double.NaN;
				this.naIdxs = addIdx(this.naIdxs, i);
			}
			else {
				this.realValues[i] = Double.longBitsToDouble(l);
			}
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
		}
	}
	
	
	@Override
	public double getNum(final int idx) {
		return this.realValues[idx];
	}
	
	@Override
	public boolean hasNA() {
		return (this.naIdxs.length > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (Double.isNaN(this.realValues[idx])
				&& Arrays.binarySearch(this.naIdxs, idx) >= 0);
	}
	
	@Override
	public void setNum(final int idx, final double value) {
		if (Double.isNaN(this.realValues[idx])) {
			this.naIdxs = deleteIdx(this.naIdxs, idx);
		}
		this.realValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		this.realValues[idx] = Double.NaN;
		this.naIdxs = addIdx(this.naIdxs, idx);
	}
	
	private void prepareInsert(final int[] idxs) {
		this.realValues = prepareInsert(this.realValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertReal(final int idx, final double value) {
		final int[] idxs = new int[] { idx };
		prepareInsert(idxs);
		this.realValues[idx] = Double.NaN;
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
		}
		this.naIdxs = insertIdx(this.naIdxs, idxs);
	}
	
	public void remove(final int idx) {
		remove(new int[] { idx });
	}
	
	public void remove(final int[] idxs) {
		this.realValues = remove(this.realValues, this.length, idxs);
		this.naIdxs = updateIdxRemoved(this.naIdxs, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Double[] toArray() {
		final Double[] array = new Double[this.length];
		for (int i = 0; i < this.length; i++) {
			if (!Double.isNaN(this.realValues[i])
					|| Arrays.binarySearch(this.naIdxs, i) < 0) {
				array[i] = Double.valueOf(this.realValues[i]);
			}
		}
		return array;
	}
	
}
