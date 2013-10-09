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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import de.walware.rj.data.RJIO;


/**
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RCharacterDataImpl extends AbstractCharacterData
		implements RDataResizeExtension, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected String[] charValues;
	
	
	public RCharacterDataImpl() {
		this.length = 0;
		this.charValues = EMPTY_STRING_ARRAY;
	}
	
	public RCharacterDataImpl(final int length) {
		this.length = length;
		this.charValues = new String[length];
		Arrays.fill(this.charValues, "");
	}
	
	public RCharacterDataImpl(final String[] values) {
		this.length = values.length;
		this.charValues = values;
	}
	
	public RCharacterDataImpl(final String[] values, final int length) {
		this.length = length;
		this.charValues = values;
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
	
	
	public RCharacterDataImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.charValues = new String[length];
		io.readStringData(this.charValues, length);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeStringData(this.charValues, this.length);
	}
	
	@Override
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
	
	@Override
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
	
	
	public final int length() {
		return this.length;
	}
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.charValues[idx] == null);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.charValues[(int) idx] == null);
	}
	
	@Override
	public void setNA(final int idx) {
//		if (this.charValues[idx] == null) {
//			return;
//		}
		this.charValues[idx] = null;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
//		if (this.charValues[(int) idx] == null) {
//			return;
//		}
		this.charValues[(int) idx] = null;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.charValues[idx] == null);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.charValues[(int) idx] == null);
	}
	
	@Override
	public String getChar(final int idx) {
		return this.charValues[idx];
	}
	
	@Override
	public String getChar(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.charValues[(int) idx];
	}
	
	@Override
	public void setChar(final int idx, final String value) {
//		assert (value != null);
		this.charValues[idx] = value;
	}
	
	@Override
	public void setChar(final long idx, final String value) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
//		assert (value != null);
		this.charValues[(int) idx] = value;
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
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.charValues[idx] = null;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int i = 0; i < idxs.length; i++) {
			this.charValues[idxs[i]] = null;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.charValues = remove(this.charValues, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.charValues = remove(this.charValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public String get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.charValues[idx];
	}
	
	@Override
	public String get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.charValues[(int) idx];
	}
	
	@Override
	public String[] toArray() {
		final String[] array = new String[length()];
		System.arraycopy(this.charValues, 0, array, 0, array.length);
		return array;
	}
	
	
	public int indexOf(final String character, int fromIdx) {
		if (character == null) {
			throw new NullPointerException();
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		final int l = length();
		final String[] chars = this.charValues;
		while (fromIdx < l) {
			if (chars[fromIdx] != null && chars[fromIdx].equals(character)) {
				return fromIdx;
			}
			fromIdx++;
		}
		return -1;
	}
	
	@Override
	public long indexOf(final String character, final long fromIdx) {
		if (character == null
				|| fromIdx >= Integer.MAX_VALUE) {
			return -1;
		}
		final int l = length();
		final String[] chars = this.charValues;
		for (int i = (fromIdx >= 0) ? ((int) fromIdx) : 0; i < l; i++) {
			if (chars[i] != null && chars[i].equals(character)) {
				return i;
			}
		}
		return -1;
	}
	
}
