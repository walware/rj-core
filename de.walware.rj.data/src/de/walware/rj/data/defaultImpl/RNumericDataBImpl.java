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


public class RNumericDataBImpl extends AbstractNumericData
		implements RDataReziseExtension, Externalizable {
	
	
	public static RNumericDataBImpl createForServer(final double[] values) {
		return new RNumericDataBImpl(values);
	}
	
	public static RNumericDataBImpl createFromWithNAList(final double[] values, final int[] naIdxs) {
		return new RNumericDataBImpl(values, naIdxs);
	}
	
	
	private double[] realValues;
	private int naCount;
	
	
	public RNumericDataBImpl() {
		this.realValues = new double[0];
		this.length = this.realValues.length;
		this.naCount = 0;
	}
	
	private RNumericDataBImpl(final double[] values) {
		this.realValues = values;
		this.length = values.length;
	}
	
	private RNumericDataBImpl(final double[] values, final int[] naIdxs) {
		this.realValues = values;
		this.length = this.realValues.length;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.realValues[naIdxs[i]] = NA_numeric_DOUBLE;
			}
			this.naCount = naIdxs.length;
		}
	}
	
	
	public RNumericDataBImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naCount = 0;
		this.realValues = new double[this.length];
		for (int i = 0; i < this.length; i++) {
			final long l = in.readLong();
			if (l == NA_numeric_LONG) {
				this.realValues[i] = NA_numeric_DOUBLE;
				this.naCount++;
			}
			else {
				this.realValues[i] = Double.longBitsToDouble(l);
			}
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeLong(Double.doubleToRawLongBits(this.realValues[i]));
		}
	}
	
	
	@Override
	public double getNum(final int idx) {
		return this.realValues[idx];
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (Double.isNaN(this.realValues[idx])
				&& Double.doubleToRawLongBits(this.realValues[idx]) == NA_numeric_LONG);
	}
	
	@Override
	public void setNum(final int idx, final double value) {
		this.realValues[idx] = Double.isNaN(value) ? Double.NaN : value;
	}
	
	@Override
	public void setNA(final int idx) {
		if (Double.isNaN(this.realValues[idx])
				&& Double.doubleToRawLongBits(this.realValues[idx]) == NA_numeric_LONG) {
			return;
		}
		this.realValues[idx] = NA_numeric_DOUBLE;
		this.naCount ++;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.realValues = prepareInsert(this.realValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertReal(final int idx, final double value) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = Double.isNaN(value) ? Double.NaN : value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.realValues[idx] = NA_numeric_DOUBLE;
		this.naCount ++;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.realValues[idx] = NA_numeric_DOUBLE;
		}
		this.naCount += idxs.length;
	}
	
	public void remove(final int idx) {
		if (Double.isNaN(this.realValues[idx])
				&& Double.doubleToRawLongBits(this.realValues[idx]) == NA_numeric_LONG) {
			this.naCount --;
		}
		this.realValues = remove(this.realValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (Double.isNaN(this.realValues[idxs[i]])
					&& Double.doubleToRawLongBits(this.realValues[idxs[i]]) == NA_numeric_LONG) {
				this.naCount --;
			}
		}
		this.realValues = remove(this.realValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	@Override
	public Double[] toArray() {
		final Double[] array = new Double[this.length];
		for (int i = 0; i < this.length; i++) {
			if (!Double.isNaN(this.realValues[i])
					|| Double.doubleToRawLongBits(this.realValues[i]) != NA_numeric_LONG) {
				array[i] = Double.valueOf(this.realValues[i]);
			}
		}
		return array;
	}
	
}
