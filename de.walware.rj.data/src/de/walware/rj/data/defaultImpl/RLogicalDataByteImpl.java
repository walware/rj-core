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
		implements RDataReziseExtension, Externalizable {
	
	
	private byte[] boolValues;
	private int naCount;
	
	
	public RLogicalDataByteImpl() {
		this.boolValues = new byte[0];
		this.length = 0;
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
		this.naCount = naIdxs.length;
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
	
	
	public RLogicalDataByteImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naCount = 0;
		this.boolValues = new byte[this.length];
		for (int i = 0; i < this.length; i++) {
			if ((this.boolValues[i] = in.readByte()) == NA_logical_BYTE) {
				this.naCount++;
			}
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeByte(this.boolValues[i]);
		}
	}
	
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx] == TRUE_BYTE);
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.boolValues[idx] == NA_logical_BYTE);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		if (this.boolValues[idx] == NA_logical_BYTE) {
			this.naCount --;
		}
		this.boolValues[idx] = value ? TRUE_BYTE : FALSE_BYTE;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.boolValues[idx] == NA_logical_BYTE) {
			return;
		}
		this.boolValues[idx] = NA_logical_BYTE;
		this.naCount ++;
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
		this.naCount ++;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_BYTE;
		}
		this.naCount += idxs.length;
	}
	
	public void remove(final int idx) {
		if (this.boolValues[idx] == NA_logical_BYTE) {
			this.naCount --;
		}
		this.boolValues = remove(this.boolValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (this.boolValues[idxs[i]] == NA_logical_BYTE) {
				this.naCount --;
			}
		}
		this.boolValues = remove(this.boolValues, this.length, idxs);
		this.length -= idxs.length;
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
