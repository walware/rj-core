/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
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

import de.walware.rj.data.RJIO;


public class RIntegerDataImpl extends AbstractIntegerData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	protected int[] intValues;
	
	
	public RIntegerDataImpl() {
		this.intValues = EMPTY_INT_ARRAY;
		this.length = 0;
	}
	
	public RIntegerDataImpl(final int[] values) {
		this.intValues = values;
		this.length = values.length;
	}
	
	public RIntegerDataImpl(final int[] values, final int length) {
		this.intValues = values;
		this.length = length;
	}
	
	public RIntegerDataImpl(final int[] values, final int[] naIdxs) {
		this.intValues = values;
		this.length = values.length;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.intValues[naIdxs[i]] = NA_integer_INT;
			}
		}
	}
	
	public RIntegerDataImpl(final int length) {
		this.intValues = new int[length];
		this.length = length;
	}
	
	
	public RIntegerDataImpl(final RJIO io) throws IOException {
		readExternal(io);
	}
	
	public void readExternal(final RJIO io) throws IOException {
		this.intValues = io.readIntArray();
		this.length = this.intValues.length;
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.intValues = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			this.intValues[i] = in.readInt();
		}
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeIntArray(this.intValues, this.length);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeInt(this.intValues[i]);
		}
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public int getInt(final int idx) {
		return this.intValues[idx];
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.intValues[idx] == NA_integer_INT);
	}
	
	public boolean isMissing(final int idx) {
		return (this.intValues[idx] == NA_integer_INT);
	}
	
	@Override
	public void setInt(final int idx, final int value) {
//		assert (value != NA_integer_INT);
		this.intValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		this.intValues[idx] = NA_integer_INT;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.intValues = prepareInsert(this.intValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertInt(final int idx, final int value) {
//		assert (value != NA_integer_INT);
		prepareInsert(new int[] { idx });
		this.intValues[idx] = value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.intValues[idx] = NA_integer_INT;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.intValues[idxs[idx]+idx] = NA_integer_INT;
		}
	}
	
	public void remove(final int idx) {
		this.intValues = remove(this.intValues, this.length, new int[] { idx });
		this.length --;
	}
	
	public void remove(final int[] idxs) {
		this.intValues = remove(this.intValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	public Integer get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException();
		}
		return (this.intValues[idx] != NA_integer_INT) ?
			Integer.valueOf(this.intValues[idx]) : null;
	}
	
	@Override
	public Integer[] toArray() {
		final Integer[] array = new Integer[this.length];
		for (int i = 0; i < this.length; i++) {
			if (this.intValues[i] != NA_integer_INT) {
				array[i] = Integer.valueOf(this.intValues[i]);
			}
		}
		return array;
	}
	
	
	@Override
	public final int indexOf(final int value, int fromIdx) {
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		if (value != NA_integer_INT) {
			while (fromIdx < this.length) {
				if (this.intValues[fromIdx] == value) {
					return fromIdx;
				}
				fromIdx++;
			}
		}
		return -1;
	}
	
	
	public void appendTo(final StringBuilder sb) {
		sb.append('[');
		if (this.length > 0) {
			for (int i = 0; i < this.length; i++) {
				sb.append(this.intValues[i]);
				sb.append(", ");
			}
			sb.delete(sb.length()-2, sb.length());
		}
		sb.append(']');
	}
	
}
