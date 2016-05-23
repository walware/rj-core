/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;


/**
 * Based on byte array.
 * 
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RLogicalDataByteImpl extends AbstractLogicalData
		implements RDataResizeExtension<Boolean>, ExternalizableRStore, Externalizable {
	
	
	public static final byte TRUE = TRUE_BYTE;
	public static final byte FALSE = FALSE_BYTE;
	
	
	private int length;
	
	private byte[] boolValues;
	
	
	public RLogicalDataByteImpl() {
		this.length = 0;
		this.boolValues = EMPTY_BYTE_ARRAY;
	}
	
	public RLogicalDataByteImpl(final int length) {
		this.length = length;
		this.boolValues = new byte[length];
	}
	
	public RLogicalDataByteImpl(final boolean[] values) {
		this.length = values.length;
		this.boolValues = new byte[values.length];
		for (int i = values.length-1; i >= 0; i--) {
			this.boolValues[i] = (values[i]) ? TRUE_BYTE : FALSE_BYTE;
		}
	}
	
	public RLogicalDataByteImpl(final boolean[] values, final int[] naIdxs) {
		this.length = values.length;
		this.boolValues = new byte[values.length];
		for (int i = values.length-1; i >= 0; i--) {
			this.boolValues[i] = (values[i]) ? TRUE_BYTE : FALSE_BYTE;
		}
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.boolValues[naIdxs[i]] = NA_logical_BYTE;
			}
		}
	}
	
	public RLogicalDataByteImpl(final byte[] values) {
		this.length = values.length;
		this.boolValues = values;
	}
	
	public RLogicalDataByteImpl(final byte[] values, final byte trueCode, final byte naCode) {
		this.length = values.length;
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
	}
	
	
	public RLogicalDataByteImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.boolValues = io.readByteData(new byte[length], length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeByteData(this.boolValues, this.length);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.boolValues = new byte[this.length];
		in.readFully(this.boolValues, 0, this.length);
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		out.write(this.boolValues, 0, this.length);
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
		return (this.boolValues[idx] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.boolValues[idx] == NA_logical_BYTE);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] == NA_logical_BYTE);
	}
	
	@Override
	public void setNA(final int idx) {
		this.boolValues[idx] = NA_logical_BYTE;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.boolValues[(int) idx] = NA_logical_BYTE;
	}
	
	@Override
	public boolean getLogi(final int idx) {
		return (this.boolValues[idx] == TRUE_BYTE);
	}
	
	@Override
	public boolean getLogi(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.boolValues[(int) idx] == TRUE_BYTE);
	}
	
	@Override
	public void setLogi(final int idx, final boolean value) {
		this.boolValues[idx] = (value) ? TRUE_BYTE : FALSE_BYTE;
	}
	
	@Override
	public void setLogi(final long idx, final boolean value) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.boolValues[(int) idx] = (value) ? TRUE_BYTE : FALSE_BYTE;
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.boolValues = prepareInsert(this.boolValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertLogi(final int idx, final boolean value) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = value ? TRUE_BYTE : FALSE_BYTE;
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.boolValues[idx] = NA_logical_BYTE;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.boolValues[idxs[idx]+idx] = NA_logical_BYTE;
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
		switch(this.boolValues[idx]) {
		case TRUE_BYTE:
			return Boolean.TRUE;
		case FALSE_BYTE:
			return Boolean.FALSE;
		default:
			return null;
		}
	}
	
	@Override
	public Boolean get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		switch(this.boolValues[(int) idx]) {
		case TRUE_BYTE:
			return Boolean.TRUE;
		case FALSE_BYTE:
			return Boolean.FALSE;
		default:
			return null;
		}
	}
	
	@Override
	public Boolean[] toArray() {
		final Boolean[] array = new Boolean[length()];
		final byte[] bools = this.boolValues;
		for (int i = 0; i < array.length; i++) {
			switch(bools[i]) {
			case TRUE_BYTE:
				array[i] = Boolean.TRUE;
				continue;
			case FALSE_BYTE:
				array[i] = Boolean.FALSE;
				continue;
			default:
				continue;
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOfNA(long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx= 0;
		}
		final long l= getLength();
		final byte[] bools= this.boolValues;
		for (int i= (int) fromIdx; i < l; i++) {
			if (bools[i] == NA_logical_BYTE) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public long indexOf(final int integer, long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE
				|| integer == NA_integer_INT ) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		final int l= length();
		final byte[] bools= this.boolValues;
		if (integer != 0) {
			for (int i= (int) fromIdx; i < l; i++) {
				if (bools[i] == TRUE_BYTE) {
					return i;
				}
			}
		}
		else {
			for (int i= (int) fromIdx; i < l; i++) {
				if (bools[i] == FALSE_BYTE) {
					return i;
				}
			}
		}
		return -1;
	}
	
}
