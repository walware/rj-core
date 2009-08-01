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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RStore;


public class RFactorDataImpl extends AbstractFactorData
		implements RDataResizeExtension, Externalizable {
	
	
	protected int[] codes;
	
	protected RCharacterDataImpl codeLabels;
	
	
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
		this.codeLabels = readLabels(in);
	}
	protected RCharacterDataImpl readLabels(final ObjectInput in) throws IOException, ClassNotFoundException {
		return new RUniqueCharacterDataImpl(in);
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
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public String getChar(final int idx) {
		return (this.codes[idx] >= 0) ? this.codeLabels.getChar(this.codes[idx]): null;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.codes[idx] == NA_integer_INT);
	}
	
	public boolean isMissing(final int idx) {
		return (this.codes[idx] == NA_integer_INT);
	}
	
	@Override
	public void setChar(final int idx, final String data) {
		final int code = this.codeLabels.indexOf(data) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codes[idx] = code;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.codes[idx] == NA_integer_INT) {
			return;
		}
		this.codes[idx] = NA_integer_INT;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.codes = prepareInsert(this.codes, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertChar(final int idx, final String data) {
		final int code = this.codeLabels.indexOf(data) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		prepareInsert(new int[] { idx });
		this.codes[idx] = code;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.codes[idx] = NA_integer_INT;
	}
	
	public void insertNA(final int[] idxs) {
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.codes[idx] = NA_integer_INT;
		}
	}
	
	public void remove(final int idx) {
		this.codes = remove(this.codes, this.length, new int[] { idx });
		this.length--;
	}
	
	public void remove(final int[] idxs) {
		this.codes = remove(this.codes, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	public RCharacterStore getLevels() {
		return this.codeLabels;
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
		final int level = this.codeLabels.indexOf(oldLabel);
		if (level < 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.setChar(level, newLabel);
	}
	
	public void removeLevel(final String label) {
		final int level = this.codeLabels.indexOf(label);
		if (level < 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.remove(level);
		for (int i = 0; i < this.length; i++) {
			if (this.codes[i] == level) {
				this.codes[i] = NA_integer_INT;
			}
			else if (this.codes[i] > level) {
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
	
	public Integer get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException();
		}
		return (this.codes[idx] != NA_integer_INT) ? Integer.valueOf(this.codes[idx]) : null;
	}
	
	@Override
	public Integer[] toArray() {
		final Integer[] array = new Integer[this.length];
		for (int i = 0; i < this.length; i++) {
			if (this.codes[i] != NA_integer_INT) {
				array[i] = Integer.valueOf(this.codes[i]);
			}
		}
		return array;
	}
	
	public boolean allEqual(final RStore other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
