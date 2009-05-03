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
 * Based on int array, default value is FALSE
 */
public class RLogicalDataIntImpl extends AbstractLogicalData
		implements RDataReziseExtension, Externalizable {
	
	
	public static RLogicalDataIntImpl createOtherTrue(final int[] values, final int falseCode, final int naCode) {
		int naCount = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == falseCode) {
				values[i] = FALSE_INT;
			}
			else if (values[i] == naCode) {
				values[i] = NA_logical_INT;
				naCount++;
			}
			else {
				values[i] = TRUE_INT;
			}
		}
		return new RLogicalDataIntImpl(values, naCount);
	}
	
	public static RLogicalDataIntImpl createOtherFalse(final int[] values, final int trueCode, final int naCode) {
		int naCount = 0;
		if (trueCode != TRUE_INT || naCode != NA_logical_INT) {
			for (int i = values.length-1; i >= 0; i--) {
				final int value = values[i];
				if (value == trueCode) {
					values[i] = TRUE_INT;
				}
				else if (value == naCode) {
					values[i] = NA_logical_INT;
					naCount++;
				}
				else {
					values[i] = FALSE_INT;
				}
			}
		}
		else {
			for (int i = 0; i < values.length; i++) {
				if (values[i] == NA_logical_INT) {
					naCount++;
				}
			}
		}
		return new RLogicalDataIntImpl(values, naCount);
	}
	
	
	private int[] boolValues;
	private int naCount;
	
	
	public RLogicalDataIntImpl() {
		this.boolValues = new int[0];
		this.length = 0;
		this.naCount = 0;
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
		this.naCount = naIdxs.length;
	}
	
	private RLogicalDataIntImpl(final int[] values, final int naCount) {
		this.length = values.length;
		this.boolValues = values;
		this.naCount = naCount;
	}
	
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naCount = 0;
		this.boolValues = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			if ((this.boolValues[i] = in.readByte()) == NA_logical_INT) {
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
		return (this.boolValues[idx] == TRUE_INT);
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.boolValues[idx] == NA_logical_INT);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		if (this.boolValues[idx] == NA_logical_INT) {
			this.naCount --;
		}
		this.boolValues[idx] = value ? TRUE_INT : FALSE_INT;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.boolValues[idx] == NA_logical_INT) {
			return;
		}
		this.boolValues[idx] = NA_logical_INT;
		this.naCount ++;
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
		this.naCount ++;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_INT;
		}
		this.naCount += idxs.length;
	}
	
	public void remove(final int idx) {
		if (this.boolValues[idx] == NA_logical_INT) {
			this.naCount --;
		}
		this.boolValues = remove(this.boolValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (this.boolValues[idxs[i]] == NA_logical_INT) {
				this.naCount --;
			}
		}
		this.boolValues = remove(this.boolValues, this.length, idxs);
		this.length -= idxs.length;
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
