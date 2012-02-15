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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import de.walware.rj.data.RJIO;


public class RCharacterDataImpl extends AbstractCharacterData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	protected String[] charValues;
	
	
	public RCharacterDataImpl() {
		this.charValues = EMPTY_STRING_ARRAY;
		this.length = 0;
	}
	
	public RCharacterDataImpl(final String[] values) {
		this(values, values.length);
	}
	
	public RCharacterDataImpl(final String[] values, final int length) {
		this.charValues = values;
		this.length = length;
	}
	
	public RCharacterDataImpl(final int length) {
		this.charValues = new String[length];
		this.length = length;
		Arrays.fill(this.charValues, "");
	}
	
	
	RCharacterDataImpl(final RCharacterDataImpl source, final boolean reuse) {
		String[] values;
		if (reuse) {
			values = source.charValues;
		}
		else {
			values = ensureCapacity(this.charValues, source.length);
			System.arraycopy(source, 0, values, 0, source.length);
		}
		this.charValues = values;
		this.length = source.length;
	}
	
	
	public RCharacterDataImpl(final RJIO io) throws IOException {
		readExternal(io);
	}
	
	public RCharacterDataImpl(final ObjectInput in) throws IOException {
		readExternal(in);
	}
	
	public void readExternal(final RJIO io) throws IOException {
		this.charValues = io.readStringArray();
		this.length = this.charValues.length;
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.charValues = new String[this.length];
		for (int i = 0; i < this.length; i++) {
			final int l = in.readInt();
			if (l >= 0) {
				final char[] c = new char[l];
				for (int j = 0; j < l; j++) {
					c[j] = in.readChar();
				}
				this.charValues[i] = new String(c);
			}
//			else {
//				this.charValues[i] = null;
//			}
		}
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeStringArray(this.charValues, this.length);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			if (this.charValues[i] != null) {
				out.writeInt(this.charValues[i].length());
				out.writeChars(this.charValues[i]);
			}
			else {
				out.writeInt(-1);
			}
		}
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public String getChar(final int idx) {
		return this.charValues[idx];
	}
	
	
	@Override
	public int indexOf(final String value, int fromIdx) {
		if (value == null) {
			throw new NullPointerException();
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		while (fromIdx < this.length) {
			if (this.charValues[fromIdx] != null && this.charValues[fromIdx].equals(value)) {
				return fromIdx;
			}
			fromIdx++;
		}
		return -1;
	}
	
	
	@Override
	public boolean isNA(final int idx) {
		return (this.charValues[idx] == null);
	}
	
	public boolean isMissing(final int idx) {
		return (this.charValues[idx] == null);
	}
	
	@Override
	public void setChar(final int idx, final String value) {
//		assert (value != null);
		this.charValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.charValues[idx] == null) {
			return;
		}
		this.charValues[idx] = null;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.charValues = prepareInsert(this.charValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertChar(final int idx, final String value) {
//		assert (value != null);
		prepareInsert(new int[] { idx });
		this.charValues[idx] = value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.charValues[idx] = null;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int i = 0; i < idxs.length; i++) {
			this.charValues[idxs[i]] = null;
		}
	}
	
	public void remove(final int idx) {
		this.charValues = remove(this.charValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		this.charValues = remove(this.charValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	public String get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException();
		}
		return this.charValues[idx];
	}
	
	public String[] toArray() {
		final String[] array = new String[this.length];
		System.arraycopy(this.charValues, 0, array, 0, this.length);
		return array;
	}
	
}
