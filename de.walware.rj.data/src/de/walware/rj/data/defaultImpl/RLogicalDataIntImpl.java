/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
 * Based on int array
 */
public class RLogicalDataIntImpl extends AbstractLogicalData
		implements RDataResizeExtension, ExternalizableRStore {
	
	
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
	
	public RLogicalDataIntImpl(final RJIO io) throws IOException {
		readExternal(io);
	}
	
	public void readExternal(final RJIO io) throws IOException {
		this.length = io.readInt();
		this.boolValues = new int[this.length];
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
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.length);
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
	
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx] != FALSE_INT);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.boolValues[idx] == NA_logical_INT);
	}
	
	public boolean isMissing(final int idx) {
		return (this.boolValues[idx] == NA_logical_INT);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		this.boolValues[idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.boolValues[idx] == NA_logical_INT) {
			return;
		}
		this.boolValues[idx] = NA_logical_INT;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.boolValues = prepareInsert(this.boolValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertLogi(final int idx, final boolean value) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = NA_logical_INT;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_INT;
		}
	}
	
	public void remove(final int idx) {
		this.boolValues = remove(this.boolValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		this.boolValues = remove(this.boolValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	public Boolean get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException();
		}
		return (this.boolValues[idx] != NA_logical_INT) ?
				((this.boolValues[idx] == TRUE_INT) ? Boolean.TRUE : Boolean.FALSE) : null;
	}
	
	public Boolean[] toArray() {
		final Boolean[] array = new Boolean[this.length];
		for (int i = 0; i < this.length; i++) {
			if (this.boolValues[i] != NA_logical_INT) {
				array[i] = (this.boolValues[i] == TRUE_INT) ? Boolean.TRUE : Boolean.FALSE;
			}
		}
		return array;
	}
	
}
