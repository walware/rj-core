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

import de.walware.rj.data.RJIO;


/**
 * Based on int array.
 * 
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RLogicalDataIntImpl extends AbstractLogicalData
		implements RDataResizeExtension, ExternalizableRStore {
	
	
	private int length;
	
	protected int[] boolValues;
	
	
	public RLogicalDataIntImpl() {
		this.boolValues = EMPTY_INT_ARRAY;
		this.length = 0;
	}
	
	public RLogicalDataIntImpl(final boolean[] values, final int[] naIdxs) {
		this.boolValues = new int[values.length];
		for (int i = values.length-1; i >= 0; i--) {
			this.boolValues[i] = values[i] ? TRUE_INT : FALSE_INT;
		}
		this.length = this.boolValues.length;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.boolValues[naIdxs[i]] = NA_logical_INT;
			}
		}
	}
	
	/**
	 * Create new logical data based on the given Java int array
	 * encoded as follows:
	 * <code>FALSE</code> = 0
	 * <code>NA</code> = 2
	 * <code>TRUE</code> = anything else (default value 1)
	 * 
	 * @param values encoded value array, used directly (not copied)
	 */
	public RLogicalDataIntImpl(final int[] values) {
		this.length = values.length;
		this.boolValues = values;
	}
	
	public RLogicalDataIntImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.boolValues = new int[length];
		for (int i = 0; i < this.length; i++) {
			final byte b = io.readByte();
			if (b == TRUE_BYTE) {
				this.boolValues[i] = TRUE_INT;
			}
			else if (b == NA_logical_BYTE) {
				this.boolValues[i] = NA_integer_INT;
			}
			else {
				this.boolValues[i] = FALSE_INT;
			}
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.length; i++) {
			switch (this.boolValues[i]) {
			case FALSE_INT:
				io.writeByte(FALSE_BYTE);
				continue;
			case NA_logical_INT:
				io.writeByte(NA_logical_BYTE);
				continue;
			default:
				io.writeByte(TRUE_BYTE);
				continue;
			}
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
		return (this.boolValues[idx] == NA_logical_INT);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] == NA_logical_INT);
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.boolValues[idx] == NA_logical_INT);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] == NA_logical_INT);
	}
	
	@Override
	public void setNA(final int idx) {
		this.boolValues[idx] = NA_logical_INT;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.boolValues[(int) idx] = NA_logical_INT;
	}
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx] != FALSE_INT);
	}
	
	@Override
	public boolean getLogi(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] != FALSE_INT);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		this.boolValues[idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	@Override
	public void setLogi(final long idx, final boolean value) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.boolValues[(int) idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.boolValues = prepareInsert(this.boolValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertLogi(final int idx, final boolean value) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = NA_logical_INT;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_INT;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.boolValues = remove(this.boolValues, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.boolValues = remove(this.boolValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Boolean get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[idx] != NA_logical_INT) ?
				((this.boolValues[idx] == TRUE_INT) ? Boolean.TRUE : Boolean.FALSE) : null;
	}
	
	@Override
	public Boolean get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] != NA_logical_INT) ?
				((this.boolValues[(int) idx] == TRUE_INT) ? Boolean.TRUE : Boolean.FALSE) : null;
	}
	
	@Override
	public Boolean[] toArray() {
		final Boolean[] array = new Boolean[length()];
		for (int i = 0; i < array.length; i++) {
			if (this.boolValues[i] != NA_logical_INT) {
				array[i] = (this.boolValues[i] == TRUE_INT) ? Boolean.TRUE : Boolean.FALSE;
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE
				|| integer == NA_integer_INT ) {
			return -1;
		}
		final int l = length();
		final int[] ints = this.boolValues;
		if (integer != 0) {
			for (int i = (fromIdx >= 0) ? ((int) fromIdx) : 0; i < l; i++) {
				if (ints[i] == TRUE_INT) {
					return i;
				}
			}
		}
		else {
			for (int i = (fromIdx >= 0) ? ((int) fromIdx) : 0; i < l; i++) {
				if (ints[i] == FALSE_INT) {
					return i;
				}
			}
		}
		return -1;
	}
	
}
