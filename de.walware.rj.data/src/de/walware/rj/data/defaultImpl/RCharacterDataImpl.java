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


public class RCharacterDataImpl extends AbstractCharacterData
		implements RDataReziseExtension, Externalizable {
	
	
	protected String[] charValues;
	protected int naCount;
	
	
	public RCharacterDataImpl() {
		this(new String[0], 0);
	}
	
	public RCharacterDataImpl(final String[] values) {
		this(values, values.length);
	}
	
	public RCharacterDataImpl(final String[] values, final int length) {
		this.charValues = values;
		this.length = length;
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
		for (int i = 0; i < this.length; i++) {
			if (this.charValues == null) {
				this.naCount++;
			}
		}
	}
	
	
	public RCharacterDataImpl(final ObjectInput in) throws IOException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		this.length = in.readInt();
		this.naCount = 0;
		this.charValues = new String[this.length];
		for (int i = 0; i < this.length; i++) {
			if (in.readBoolean()) {
				this.charValues[i] = in.readUTF();
			}
			else {
				this.naCount++;
			}
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			if (this.charValues[i] == null) {
				out.writeBoolean(false);
			}
			else {
				out.writeBoolean(true);
				out.writeUTF(this.charValues[i]);
			}
		}
	}
	
	
	@Override
	public String getChar(final int idx) {
		return this.charValues[idx];
	}
	
	public int getIdx(final String value) {
		for (int i = 0; i < this.length; i++) {
			if (this.charValues[i] != null && this.charValues[i].equals(value)) {
				return i;
			}
		}
		return -1;
	}
	
	public boolean contains(final String value) {
		for (int i = 0; i < this.length; i++) {
			if (this.charValues[i] != null && this.charValues[i].equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.charValues[idx] == null);
	}
	
	@Override
	public void setChar(final int idx, final String value) {
		assert (value != null);
		if (this.charValues[idx] == null) {
			this.naCount--;
		}
		this.charValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.charValues[idx] == null) {
			return;
		}
		this.charValues[idx] = null;
		this.naCount++;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.charValues = prepareInsert(this.charValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertChar(final int idx, final String value) {
		assert (value != null);
		prepareInsert(new int[] { idx });
		this.charValues[idx] = value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.charValues[idx] = null;
		this.naCount++;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int i = 0; i < idxs.length; i++) {
			this.charValues[idxs[i]] = null;
		}
		this.naCount+=idxs.length;
	}
	
	public void remove(final int idx) {
		if (this.charValues[idx] == null) {
			this.naCount--;
		}
		this.charValues = remove(this.charValues, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (this.charValues[idxs[i]] == null) {
				this.naCount--;
			}
		}
		this.charValues = remove(this.charValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	public String[] toArray() {
		final String[] array = new String[this.length];
		System.arraycopy(this.charValues, 0, array, 0, this.length);
		return array;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("character ");
		int end = (this.length <= 25) ? this.length : 10;
		for (int i = 0; true;) {
			if (isNA(i)) {
				sb.append("NA");
			}
			else {
				sb.append('"');
				sb.append(getChar(i));
				sb.append('"');
			}
			i++;
			if (i < end) {
				sb.append(", ");
				continue;
			}
			else {
				if (end == this.length) {
					break;
				}
				else {
					sb.append(", .., ");
					i = this.length - 10;
					end = this.length;
					continue;
				}
			}
		}
		return sb.toString();
	}
	
}
