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
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RIntegerDataImpl extends AbstractIntegerData
		implements RDataResizeExtension<Integer>, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected int[] intValues;
	
	
	public RIntegerDataImpl() {
		this.length = 0;
		this.intValues = EMPTY_INT_ARRAY;
	}
	
	public RIntegerDataImpl(final int length) {
		this.intValues = new int[length];
		this.length = length;
	}
	
	public RIntegerDataImpl(final int[] values) {
		this.length = values.length;
		this.intValues = values;
	}
	
	public RIntegerDataImpl(final int[] values, final int length) {
		this.length = length;
		this.intValues = values;
	}
	
	public RIntegerDataImpl(final int[] values, final int[] naIdxs) {
		this.length = values.length;
		this.intValues = values;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.intValues[naIdxs[i]] = NA_integer_INT;
			}
		}
	}
	
	
	public RIntegerDataImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.intValues = io.readIntData(new int[length], length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeIntData(this.intValues, this.length);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.intValues = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			this.intValues[i] = in.readInt();
		}
	}
	
	@Override
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
	
	
	protected final int length() {
		return this.length;
	}
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.intValues[idx] == NA_integer_INT);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.intValues[(int) idx] == NA_integer_INT);
	}
	
	@Override
	public void setNA(final int idx) {
		this.intValues[idx] = NA_integer_INT;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.intValues[(int) idx] = NA_integer_INT;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.intValues[idx] == NA_integer_INT);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.intValues[(int) idx] == NA_integer_INT);
	}
	
	@Override
	public int getInt(final int idx) {
		return this.intValues[idx];
	}
	
	@Override
	public int getInt(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.intValues[(int) idx];
	}
	
	@Override
	public void setInt(final int idx, final int value) {
//		assert (value != NA_integer_INT);
		this.intValues[idx] = value;
	}
	
	@Override
	public void setInt(final long idx, final int value) {
//		assert (value != NA_integer_INT);
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.intValues[(int) idx] = value;
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
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.intValues[idx] = NA_integer_INT;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.intValues[idxs[idx]+idx] = NA_integer_INT;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.intValues = remove(this.intValues, this.length, new int[] { idx });
		this.length --;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.intValues = remove(this.intValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public Integer get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.intValues[idx];
		return (v != NA_integer_INT) ?
				Integer.valueOf(v) : null;
	}
	
	@Override
	public Integer get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.intValues[(int) idx];
		return (v != NA_integer_INT) ?
				Integer.valueOf(v) : null;
	}
	
	@Override
	public Integer[] toArray() {
		final Integer[] array = new Integer[length()];
		final int[] ints = this.intValues;
		for (int i = 0; i < array.length; i++) {
			final int v = ints[i];
			if (v != NA_integer_INT) {
				array[i] = Integer.valueOf(v);
			}
		}
		return array;
	}
	
	
	@Override
	public final long indexOfNA(long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx= 0;
		}
		final int l= length();
		final int[] ints= this.intValues;
		for (int i= (int) fromIdx; i < l; i++) {
			if (ints[i] == NA_integer_INT) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public final long indexOf(final int integer, long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE
				|| integer == NA_integer_INT ) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx= 0;
		}
		final int l= length();
		final int[] ints= this.intValues;
		for (int i= (int) fromIdx; i < l; i++) {
			if (ints[i] == integer) {
				return i;
			}
		}
		return -1;
	}
	
	
	public void appendTo(final StringBuilder sb) {
		sb.append('[');
		final int l = length();
		if (l > 0) {
			final int[] ints = this.intValues;
			for (int i = 0; i < l; i++) {
				sb.append(ints[i]);
				sb.append(", ");
			}
			sb.delete(sb.length()-2, sb.length());
		}
		sb.append(']');
	}
	
}
