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


/**
 * Based on byte array, default value is FALSE
 */
public class RLogicalDataByteImpl extends AbstractLogicalData
		implements RDataResizeExtension, Externalizable {
	
	
	public static final byte TRUE = TRUE_BYTE;
	public static final byte FALSE = FALSE_BYTE;
	
	
	private byte[] boolValues;
	
	
	public RLogicalDataByteImpl() {
		this.boolValues = new byte[0];
		this.length = 0;
	}
	
	public RLogicalDataByteImpl(final boolean[] values) {
		this.boolValues = new byte[values.length];
		for (int i = values.length-1; i >= 0; i--) {
			this.boolValues[i] = (values[i]) ? TRUE_BYTE : FALSE_BYTE;
		}
		this.length = this.boolValues.length;
	}
	
	public RLogicalDataByteImpl(final boolean[] values, final int[] naIdxs) {
		this.boolValues = new byte[values.length];
		for (int i = values.length-1; i >= 0; i--) {
			this.boolValues[i] = (values[i]) ? TRUE_BYTE : FALSE_BYTE;
		}
		this.length = this.boolValues.length;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.boolValues[naIdxs[i]] = NA_logical_BYTE;
			}
		}
	}
	
	public RLogicalDataByteImpl(final byte[] values) {
		this.boolValues = values;
		this.length = this.boolValues.length;
	}
	
	public RLogicalDataByteImpl(final byte[] values, final byte trueCode, final byte naCode) {
		if (trueCode != TRUE_BYTE || naCode != NA_logical_BYTE) {
			for (int i = values.length-1; i >= 0; i--) {
				final int value = values[i];
				if (value == trueCode) {
					values[i] = TRUE_BYTE;
				}
				else if (value == naCode) {
					values[i] = NA_logical_BYTE;
				}
				else {
					values[i] = FALSE_BYTE;
				}
			}
		}
		this.boolValues = values;
		this.length = this.boolValues.length;
	}
	
	public RLogicalDataByteImpl(final int length) {
		this.boolValues = new byte[length];
		this.length = length;
	}
	
	
	public RLogicalDataByteImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.boolValues = new byte[this.length];
		for (int i = 0; i < this.length; i++) {
			this.boolValues[i] = in.readByte();
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeByte(this.boolValues[i]);
		}
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx] == TRUE_BYTE);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.boolValues[idx] == NA_logical_BYTE);
	}
	
	public boolean isMissing(final int idx) {
		return (this.boolValues[idx] == NA_logical_BYTE);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		this.boolValues[idx] = value ? TRUE_BYTE : FALSE_BYTE;
	}
	
	@Override
	public void setNA(final int idx) {
		this.boolValues[idx] = NA_logical_BYTE;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.boolValues = prepareInsert(this.boolValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertLogi(final int idx, final boolean value) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = value ? TRUE_BYTE : FALSE_BYTE;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = NA_logical_BYTE;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_BYTE;
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
		return (this.boolValues[idx] != NA_logical_BYTE) ?
				((this.boolValues[idx] == TRUE_BYTE) ? Boolean.TRUE : Boolean.FALSE) : null;
	}
	
	public Boolean[] toArray() {
		final Boolean[] array = new Boolean[this.length];
		for (int i = 0; i < this.length; i++) {
			if (this.boolValues[i] != NA_logical_BYTE) {
				array[i] = (this.boolValues[i] == TRUE_BYTE) ? Boolean.TRUE : Boolean.FALSE;
			}
		}
		return array;
	}
	
}
