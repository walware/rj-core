/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RStore;


/**
 * This implementation is limited to length of 2<sup>31</sup>-1.
 */
public class RFactorDataImpl extends AbstractFactorData
		implements RDataResizeExtension<Integer>, ExternalizableRStore, Externalizable {
	
	
	private int length;
	
	protected int[] codes;
	
	protected RCharacterDataImpl codeLabels;
	
	
	public RFactorDataImpl(final int length, final boolean isOrdered, final String[] levelLabels) {
		if (levelLabels == null) {
			throw new NullPointerException();
		}
		this.length = length;
		this.isOrdered = isOrdered;
		this.codes = new int[length];
		Arrays.fill(this.codes, NA_integer_INT);
		this.codeLabels = new RUniqueCharacterDataImpl(levelLabels);
	}
	
	public RFactorDataImpl(final int[] codes, final boolean isOrdered, final String[] levelLabels) {
		if (codes == null || levelLabels == null) {
			throw new NullPointerException();
		}
		this.length = codes.length;
		this.isOrdered = isOrdered;
		this.codes = codes;
		this.codeLabels = new RUniqueCharacterDataImpl(levelLabels);
	}
	
	
	public RFactorDataImpl(final RJIO io, final int length) throws IOException {
		this.length = length;
		this.isOrdered = io.readBoolean();
		this.codes = new int[length];
		io.readIntData(this.codes, length);
		this.codeLabels = readLabels(io, io.readInt());
	}
	protected RCharacterDataImpl readLabels(final RJIO io, final int l) throws IOException {
		return new RUniqueCharacterDataImpl(io, l);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeBoolean(this.isOrdered);
		io.writeIntData(this.codes, this.length);
		io.writeInt(this.codeLabels.length());
		this.codeLabels.writeExternal(io);
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.isOrdered = in.readBoolean();
		this.length = in.readInt();
		this.codes = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			this.codes[i] = in.readInt();
		}
		this.codeLabels = new RUniqueCharacterDataImpl();
		this.codeLabels.readExternal(in);
	}
	
	@Override
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
	
	
	protected final int length() {
		return this.length;
	}
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.codes[idx] <= 0);
	}
	
	@Override
	public boolean isNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.codes[(int) idx] <= 0);
	}
	
	@Override
	public void setNA(final int idx) {
		this.codes[idx] = NA_integer_INT;
	}
	
	@Override
	public void setNA(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.codes[(int) idx] = NA_integer_INT;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.codes[idx] <= 0);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return (this.codes[(int) idx] <= 0);
	}
	
	@Override
	public int getInt(final int idx) {
		return this.codes[idx];
	}
	
	@Override
	public int getInt(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.codes[(int) idx];
	}
	
	@Override
	public void setInt(final int idx, final int integer) {
		if (integer <= 0 || integer > this.codeLabels.length()) {
			throw new IllegalArgumentException();
		}
		this.codes[idx] = integer;
	}
	
	@Override
	public void setInt(final long idx, final int integer) {
		if (integer <= 0 || integer > this.codeLabels.length()) {
			throw new IllegalArgumentException();
		}
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.codes[(int) idx] = integer;
	}
	
	@Override
	public String getChar(final int idx) {
		final int v = this.codes[idx];
		return (v > 0) ? this.codeLabels.getChar(v - 1): null;
	}
	
	@Override
	public String getChar(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.codes[(int) idx];
		return (v > 0) ? this.codeLabels.getChar(v - 1): null;
	}
	
	@Override
	public void setChar(final int idx, final String data) {
		final int code = this.codeLabels.indexOf(data, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codes[idx] = code;
	}
	
	@Override
	public void setChar(final long idx, final String data) {
		final int code = this.codeLabels.indexOf(data, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		this.codes[(int) idx] = code;
	}
	
	
	private void prepareInsert(final int[] idxs) {
		this.codes = prepareInsert(this.codes, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertChar(final int idx, final String data) {
		final int code = this.codeLabels.indexOf(data, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		prepareInsert(new int[] { idx });
		this.codes[idx] = code;
	}
	
	@Override
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.codes[idx] = NA_integer_INT;
	}
	
	@Override
	public void insertNA(final int[] idxs) {
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.codes[idx] = NA_integer_INT;
		}
	}
	
	@Override
	public void remove(final int idx) {
		this.codes = remove(this.codes, this.length, new int[] { idx });
		this.length--;
	}
	
	@Override
	public void remove(final int[] idxs) {
		this.codes = remove(this.codes, this.length, idxs);
		this.length -= idxs.length;
	}
	
	
	@Override
	public RCharacterStore getLevels() {
		return this.codeLabels;
	}
	
	@Override
	public int getLevelCount() {
		return this.codeLabels.length();
	}
	
	public void addLevel(final String label) {
		insertLevel(this.codes.length, label);
	}
	
	public void insertLevel(final int position, final String label) {
		this.codeLabels.insertChar(position, label);
		if (position < this.codeLabels.getLength()-1) {
			final int length = length();
			for (int i = 0; i < length; i++) {
				if (this.codes[i] >= position) {
					this.codes[i]++;
				}
			}
		}
	}
	
	public void renameLevel(final String oldLabel, final String newLabel) {
		final int code = this.codeLabels.indexOf(oldLabel, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.setChar(code - 1, newLabel);
	}
	
	public void removeLevel(final String label) {
		final int code = this.codeLabels.indexOf(label, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codeLabels.remove(code - 1);
		final int length = length();
		for (int i = 0; i < length; i++) {
			if (this.codes[i] == code) {
				this.codes[i] = NA_integer_INT;
			}
			else if (this.codes[i] > code) {
				this.codes[i]--;
			}
		}
	}
	
	@Override
	public RCharacterStore toCharacterData() {
		final String[] data = new String[length()];
		final int[] ints = this.codes;
		for (int i = 0; i < data.length; i++) {
			final int v = ints[i];
			if (v > 0) {
				data[i] = this.codeLabels.getChar(this.codes[i] - 1);
			}
		}
		return new RCharacterDataImpl(data);
	}
	
	
	@Override
	public Integer get(final int idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.codes[idx];
		return (v > 0) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer get(final long idx) {
		if (idx < 0 || idx >= length()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.codes[(int) idx];
		return (v > 0) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer[] toArray() {
		final Integer[] array = new Integer[length()];
		final int[] ints = this.codes;
		for (int i = 0; i < array.length; i++) {
			final int v = ints[i];
			if (v > 0) {
				array[i] = Integer.valueOf(v);
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		if (fromIdx >= Integer.MAX_VALUE
				|| integer <= 0 || integer > this.codeLabels.length()) {
			return -1;
		}
		final int l = length();
		final int[] ints = this.codes;
		for (int i = (fromIdx >= 0) ? ((int) fromIdx) : 0; i < l; i++) {
			if (ints[i] == integer) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public long indexOf(final String character, final long fromIdx) {
		final int code = this.codeLabels.indexOf(character, 0) + 1;
		return indexOf(code, fromIdx);
	}
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
}
