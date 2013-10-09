/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractRObject implements RObject {
	
	
	protected static final RObject[] EMPTY_ROBJECT_ARRAY = new RObject[0];
	protected static final RObject[][] EMPTY_ROBJECT_2dARRAY = new RObject[0][];
	
	
	protected static final long check2dArrayLength(final RObject[][] array, final int segmentLength) {
		long length = 0;
		if (array.length > 0) {
			final int last = array.length - 1;
			for (int i = 0; i < last; i++) {
				if (array[i].length != segmentLength) {
					throw new IllegalArgumentException("Unexpected list segment length (" + array[i].length + ", but " + segmentLength + " expected)");
				}
			}
			length = last * (long) segmentLength;
			if (array[last].length > segmentLength) {
				throw new IllegalArgumentException("Unexpected list segment length (" + array[last].length + ", but max " + segmentLength + " expected)");
			}
			length += array[last].length;
		}
		return length;
	}
	
	protected static final RObject[][] new2dRObjectArray(final long length, final int segmentLength) {
		if (length == 0) {
			return EMPTY_ROBJECT_2dARRAY;
		}
		final RObject[][] array = new RObject[1 + (int) ((length - 1) / segmentLength)][];
		final int last = array.length - 1;
		for (int i = 0; i < last; i++) {
			array[i] = new RObject[segmentLength];
		}
		{	final int restLength = (int) (length % segmentLength);
			array[last] = new RObject[(restLength == 0) ? segmentLength : restLength];
		}
		return array;
	}
	
	
	protected static final int checkShortLength(final long length) throws IOException {
		if (length >= Integer.MAX_VALUE) {
			throw new IOException("Long length (" + length + ") not supported by this implementation.");
		}
		return (int) length;
	}
	
	
	protected static final int getNewArraySize(final int length) {
		if (length >= 0xfffffff) {
			return Integer.MAX_VALUE;
		}
		return ((length+0x7) | 0xf) + 1;
	}
	
	protected static final RObject[] ensureCapacity(final RObject[] currentValues, final int length) {
		if (currentValues.length >= length) {
			return currentValues;
		}
		return new RObject[getNewArraySize(length)];
	}
	
	protected static final RStore[] ensureCapacity(final RStore[] currentValues, final int length) {
		if (currentValues.length >= length) {
			return currentValues;
		}
		return new RStore[getNewArraySize(length)];
	}
	
	protected static final RObject[] prepareInsert(final RObject[] currentValues, final int currentLength, final int[] idxs) {
		final RObject[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
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
	
	protected static final RStore[] prepareInsert(final RStore[] currentValues, final int currentLength, final int[] idxs) {
		final RStore[] newValues = ensureCapacity(currentValues, currentLength+idxs.length);
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
	
	protected static final RObject[] remove(final RObject[] currentValues, final int currentLength, final int[] idxs) {
		final RObject[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
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
	
	protected static final RStore[] remove(final RStore[] currentValues, final int currentLength, final int[] idxs) {
		final RStore[] newValues = ensureCapacity(currentValues, currentLength-idxs.length);
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
	
	
	private RList attributes;
	
	
	protected void setAttributes(final RList attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public final RList getAttributes() {
		return this.attributes;
	}
	
}
