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

import java.util.Arrays;

import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RStore;


public abstract class AbstractRData implements RStore {
	
	
	protected static final long NA_numeric_LONG = 0x7ff80000000007A2L;
	protected static final double NA_numeric_DOUBLE = Double.longBitsToDouble(NA_numeric_LONG);
	protected static final long NaN_numeric_LONG = Double.doubleToLongBits(Double.NaN);
	protected static final double NaN_numeric_DOUBLE = Double.NaN;
	
	protected static final int NA_integer_INT = Integer.MIN_VALUE;
	
	protected static final byte NA_byte_BYTE = 0x0;
	
	protected static final byte FALSE_BYTE = 0;
	protected static final byte TRUE_BYTE = 1;
	protected static final byte NA_logical_BYTE = 2;
	
	protected static final int FALSE_INT = 0;
	protected static final int TRUE_INT = 1;
	protected static final int NA_logical_INT = Integer.MIN_VALUE;
	
	protected static final int WITH_NAMES = 0x1;
	
	protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	protected static final int[] EMPTY_INT_ARRAY = new int[0];
	protected static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
	protected static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	
	private final static boolean gIsBSupported;
	static {
		gIsBSupported = checkIsBSupported();
	}
	
	public static boolean isBforNASupported() {
		return gIsBSupported;
	}
	
	private static boolean checkIsBSupported() {
		double d = 0;
		long l = 0;
		
		d = NA_numeric_DOUBLE;
		l = Double.doubleToRawLongBits(d);
		if (l != NA_numeric_LONG) {
			return false;
		}
		
		d = Double.longBitsToDouble(l);
		l = Double.doubleToRawLongBits(d);
		if (l != NA_numeric_LONG) {
			return false;
		}
		return true;
	}
	
	
	protected static final int[] addIdx(final int[] idxs, final int newIdx) {
		int i = Arrays.binarySearch(idxs, newIdx);
		if (i >= 0) {
			return idxs;
		}
		final int[] newIdxs = new int[idxs.length+1];
		i = -1-i;
		System.arraycopy(idxs, 0, newIdxs, 0, i);
		newIdxs[i] = newIdx;
		System.arraycopy(idxs, i, newIdxs, i+1, idxs.length-i);
		return newIdxs;
	}
	
	protected static final int[] insertIdx(final int[] currentIdxs, final int[] insertedIdxs) {
		final int[] pos = new int[insertedIdxs.length];
		for (int i = 0; i < pos.length; i++) {
			pos[i] = Arrays.binarySearch(currentIdxs, insertedIdxs[i]);
			if (pos[i] < 0) {
				pos[i] = -1-pos[i];
			}
		}
		final int[] newIdxs = new int[currentIdxs.length+insertedIdxs.length];
		int l = pos.length;
		int ci = currentIdxs.length-1;
		while(l > 1) {
			final int stop = pos[l-1];
			for (; ci >= stop; ci--) {
				newIdxs[ci+l] = currentIdxs[ci] + l;
			}
			l--;
			newIdxs[ci+l] = insertedIdxs[l] + l;
		}
		System.arraycopy(currentIdxs, 0, newIdxs, 0, pos[0]);
		return newIdxs;
	}
	
	protected static final void updateIdxInserted(final int[] currentIdxs, final int[] insertedIdxs) {
		int pos = Arrays.binarySearch(currentIdxs, insertedIdxs[0]);
		if (pos < 0) {
			pos = -1-pos;
		}
		for (int i = 0; i < insertedIdxs.length; ) {
			i++;
			int stop = (i < insertedIdxs.length) ? Arrays.binarySearch(currentIdxs, insertedIdxs[i]) : currentIdxs.length;
			if (stop < 0) {
				stop = -1-stop;
			}
			while (pos < stop) {
				currentIdxs[pos] += i;
			}
			pos = stop;
		}
	}
	
	protected static final int[] updateIdxRemoved(final int[] currentIdxs, final int[] removedIdxs) {
		final int[] pos = new int[removedIdxs.length];
		int naCount = 0;
		for (int i = 0; i < pos.length; i++) {
			pos[i] = Arrays.binarySearch(currentIdxs, removedIdxs[i]);
			if (pos[i] < 0) {
				pos[i] = -1-pos[i];
			}
			else {
				naCount++;
			}
		}
		final int[] newIdxs = (naCount == 0) ? currentIdxs : new int[currentIdxs.length-naCount];
		int l = 0;
		int ci = 0;
		int ni = 0;
		if (newIdxs != currentIdxs) {
			System.arraycopy(currentIdxs, 0, newIdxs, 0, pos[0]);
		}
		while(l < removedIdxs.length) {
			if (currentIdxs[pos[l]] == removedIdxs[l]) {
				ci++;
			}
			l++;
			final int stop = (l < removedIdxs.length) ? pos[l] : currentIdxs.length;
			for (; ci < stop; ci++, ni++) {
				newIdxs[ni] = currentIdxs[ci] - l;
			}
		}
		return newIdxs;
	}
	
	protected static final int[] deleteIdx(final int[] idxs, final int removeIdx) {
		final int i = Arrays.binarySearch(idxs, removeIdx);
		if (i < 0) {
			return idxs;
		}
		final int[] newIdxs = new int[idxs.length-1];
		System.arraycopy(idxs, 0, newIdxs, 0, i);
		System.arraycopy(idxs, i+1, newIdxs, i, idxs.length-i-1);
		return newIdxs;
	}
	
	protected static final int getNewArraySize(final int length) {
		if (length >= 0xfffffff) {
			return Integer.MAX_VALUE;
		}
		return ((length+0x7) | 0xf) + 1;
	}
	
	protected static final double[] ensureCapacity(final double[] currentValues, final int length) {
		if (currentValues.length >= length) {
			return currentValues;
		}
		return new double[getNewArraySize(length)];
	}
	
	protected static final int[] ensureCapacity(final int[] currentValues, final int length) {
		final int diff = currentValues.length - length;
		if (diff >= 0 && diff <= 512) {
			return currentValues;
		}
		return new int[getNewArraySize(length)];
	}
	
	protected static final byte[] ensureCapacity(final byte[] currentValues, final int length) {
		if (currentValues.length >= length) {
			return currentValues;
		}
		return new byte[getNewArraySize(length)];
	}
	
	protected static final String[] ensureCapacity(final String[] currentValues, final int length) {
		if (currentValues.length >= length) {
			return currentValues;
		}
		return new String[getNewArraySize(length)];
	}
	
	protected static final double[] prepareInsert(final double[] currentValues, final int currentLength, final int[] idxs) {
		final double[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
		int i = idxs.length-1;
		System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, currentLength-idxs[i]);
		for (i--; i >= 0; i--) {
			System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, idxs[i+1]-idxs[i]);
		}
		if (newValues != currentValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		return newValues;
	}
	
	protected static final int[] prepareInsert(final int[] currentValues, final int currentLength, final int[] idxs) {
		final int[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
		int i = idxs.length-1;
		System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, currentLength-idxs[i]);
		for (i--; i >= 0; i--) {
			System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, idxs[i+1]-idxs[i]);
		}
		if (newValues != currentValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		return newValues;
	}
	
	protected static final byte[] prepareInsert(final byte[] currentValues, final int currentLength, final int[] idxs) {
		final byte[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
		int i = idxs.length-1;
		System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, currentLength-idxs[i]);
		for (i--; i >= 0; i--) {
			System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, idxs[i+1]-idxs[i]);
		}
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		return newValues;
	}
	
	protected static final String[] prepareInsert(final String[] currentValues, final int currentLength, final int[] idxs) {
		final String[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
		int i = idxs.length-1;
		System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, currentLength-idxs[i]);
		for (i--; i >= 0; i--) {
			System.arraycopy(currentValues, idxs[i], newValues, idxs[i]+i+1, idxs[i+1]-idxs[i]);
		}
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		return newValues;
	}
	
	protected static final double[] remove(final double[] currentValues, final int currentLength, final int[] idxs) {
		final double[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		int i = 0;
		for (; i < idxs.length-1; i++) {
			System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, idxs[i+1]-idxs[i]);
		}
		System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, currentLength-idxs[i]-1);
		return newValues;
	}
	
	protected static final int[] remove(final int[] currentValues, final int currentLength, final int[] idxs) {
		final int[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		int i = 0;
		for (; i < idxs.length-1; i++) {
			System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, idxs[i+1]-idxs[i]);
		}
		System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, currentLength-idxs[i]-1);
		return newValues;
	}
	
	protected static final byte[] remove(final byte[] currentValues, final int currentLength, final int[] idxs) {
		final byte[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		int i = 0;
		for (; i < idxs.length-1; i++) {
			System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, idxs[i+1]-idxs[i]);
		}
		System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, currentLength-idxs[i]-1);
		return newValues;
	}
	
	protected static final String[] remove(final String[] currentValues, final int currentLength, final int[] idxs) {
		final String[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
		if (currentValues != newValues) {
			System.arraycopy(currentValues, 0, newValues, 0, idxs[0]);
		}
		int i = 0;
		for (; i < idxs.length-1; i++) {
			System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, idxs[i+1]-idxs[i]);
		}
		System.arraycopy(currentValues, idxs[i]+1, newValues, idxs[i]-i, currentLength-idxs[i]-1);
		return newValues;
	}
	
	
	protected int length;
	
	
	public final int getLength() {
		return this.length;
	}
	
	
	public byte getRaw(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setRaw(final int idx, final byte raw) {
		throw new UnsupportedOperationException();
	}
	
	public int getInt(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setInt(final int idx, final int integer) {
		throw new UnsupportedOperationException();
	}
	
	public double getNum(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setNum(final int idx, final double real) {
		throw new UnsupportedOperationException();
	}
	
	public String getChar(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setChar(final int idx, final String character) {
		throw new UnsupportedOperationException();
	}
	
	public double getCplxIm(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public double getCplxRe(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setCplx(final int idx, final double real, final double imaginary) {
		throw new UnsupportedOperationException();
	}
	
	public boolean getLogi(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setLogi(final int idx, final boolean logi) {
		throw new UnsupportedOperationException();
	}
	
	
	public boolean isNA(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public void setNA(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	
	protected abstract boolean isStructOnly();
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(RDataUtil.getStoreAbbr(this));
		sb.append(' ');
		if (isStructOnly()) {
			sb.append("<struct only>");
		}
		else if (this.length > 0) {
			int end = (this.length <= 25) ? this.length : 10;
			if (getStoreType() == CHARACTER) {
				for (int i = 0; true;) {
					if (isNA(i)) {
						sb.append("NA");
					}
					else {
						sb.append('"');
						sb.append(getChar(i));
						sb.append('"');
					}
					i++;
					if (i < end) {
						sb.append(", ");
						continue;
					}
					else {
						if (end == this.length) {
							break;
						}
						else {
							sb.append(", .., ");
							i = this.length - 10;
							end = this.length;
							continue;
						}
					}
				}
			}
			else {
				for (int i = 0; true;) {
					if (isNA(i)) {
						sb.append("NA");
					}
					else {
						sb.append(getChar(i));
					}
					i++;
					if (i < end) {
						sb.append(", ");
						continue;
					}
					else {
						if (end == this.length) {
							break;
						}
						else {
							sb.append(", .., ");
							i = this.length - 10;
							end = this.length;
							continue;
						}
					}
				}
			}
		}
		return sb.toString();
	}
	
}
