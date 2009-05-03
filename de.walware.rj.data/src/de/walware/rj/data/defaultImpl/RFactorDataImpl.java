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
import java.util.List;

import de.walware.rj.data.RCharacterStore;


public class RFactorDataImpl extends AbstractFactorData
		implements RDataReziseExtension, Externalizable {
	
	
	private int[] codes;
	private int naCount;
	
	private RUniqueCharacterDataImpl codeLabels;
	
	
	public RFactorDataImpl(final int[] codes, final boolean isOrdered, final String[] levelLabels) {
		if (codes == null || levelLabels == null) {
			throw new NullPointerException();
		}
		this.codes = codes;
		this.codeLabels = new RUniqueCharacterDataImpl(levelLabels);
		this.length = codes.length;
		this.isOrdered = isOrdered;
	}
	
	
	public RFactorDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.isOrdered = in.readBoolean();
		this.length = in.readInt();
		this.codes = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			this.codes[i] = in.readInt();
		}
		this.codeLabels = new RUniqueCharacterDataImpl(in);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeBoolean(this.isOrdered);
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeInt(this.codes[i]);
		}
		this.codeLabels.writeExternal(out);
	}
	
	
	@Override
	public String getChar(final int idx) {
		return (this.codes[idx] >= 0) ? this.codeLabels.getChar(this.codes[idx]): null;
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.codes[idx] == NA_integer_INT);
	}
	
	@Override
	public void setChar(final int idx, final String data) {
		if (this.codes[idx] == NA_integer_INT) {
			this.naCount--;
		}
		this.codes[idx] = this.codeLabels.getIdx(data);
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.codes[idx] == NA_integer_INT) {
			return;
		}
		this.codes[idx] = NA_integer_INT;
		this.naCount++;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.codes = prepareInsert(this.codes, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertChar(final int idx, final String data) {
		prepareInsert(new int[] { idx });
		this.codes[idx] = this.codeLabels.getIdx(data);
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.codes[idx] = NA_integer_INT;
		this.naCount++;
	}
	
	public void insertNA(final int[] idxs) {
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.codes[idx] = NA_integer_INT;
		}
		this.naCount+=idxs.length;
	}
	
	public void remove(final int idx) {
		if (this.codes[idx] == NA_integer_INT) {
			this.naCount--;
		}
		this.codes = remove(this.codes, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (this.codes[idxs[i]] == NA_integer_INT) {
				this.naCount--;
			}
		}
		this.codes = remove(this.codes, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	public List<String> getLevels() {
		return null;
	}
	
	public int getLevelCount() {
		return this.codeLabels.length;
	}
	
	public void addLevel(final String label) {
		insertLevel(this.codes.length, label);
	}
	
	public void insertLevel(final int position, final String label) {
		this.codeLabels.insertChar(position, label);
		if (position < this.codeLabels.getLength()-1) {
			for (int i = 0; i < this.length; i++) {
				if (this.codes[i] >= position) {
					this.codes[i]++;
				}
			}
		}
	}
	
	public void renameLevel(final String oldLabel, final String newLabel) {
		final int code = this.codeLabels.getIdx(oldLabel);
		if (code < 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.setChar(code, newLabel);
	}
	
	public void removeLevel(final String label) {
		final int code = this.codeLabels.getIdx(label);
		if (code < 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.remove(code);
		for (int i = 0; i < this.length; i++) {
			if (this.codes[i] == code) {
				this.codes[i] = NA_integer_INT;
			}
			else if (this.codes[i] > code) {
				this.codes[i]--;
			}
		}
	}
	
	public RCharacterStore toCharacterData() {
		final String[] data = new String[this.length];
		for (int i = 0; i < this.length; i++) {
			data[i] = (this.codes[i] >= 0) ? this.codeLabels.getChar(this.codes[i]) : null;
		}
		return new RCharacterDataImpl(data);
	}
	
	public Integer[] toArray() {
		final Integer[] array = new Integer[this.length];
		for (int i = 0; i < this.length; i++) {
			if (this.codes[i] != NA_integer_INT) {
				array[i] = Integer.valueOf(this.codes[i]);
			}
		}
		return array;
	}
	
}
