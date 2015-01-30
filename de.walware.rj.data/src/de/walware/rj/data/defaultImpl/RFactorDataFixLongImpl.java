/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;
import java.util.Arrays;

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RStore;


public class RFactorDataFixLongImpl extends AbstractFactorData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final int[][] codes;
	
	protected final RCharacterDataImpl codeLabels;
	
	
	public RFactorDataFixLongImpl(final long length, final boolean isOrdered, final String[] levelLabels) {
		this.length = length;
		this.isOrdered = isOrdered;
		this.codes = new2dIntArray(length, SEGMENT_LENGTH);
		Arrays.fill(this.codes, NA_integer_INT);
		this.codeLabels = new RUniqueCharacterDataImpl(levelLabels);
	}
	
	public RFactorDataFixLongImpl(final int[][] codes, final boolean isOrdered, final String[] levelLabels) {
		this.length = check2dArrayLength(codes, SEGMENT_LENGTH);
		this.isOrdered = isOrdered;
		this.codes = codes;
		this.codeLabels = new RUniqueCharacterDataImpl(levelLabels);
	}
	
	
	public RFactorDataFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.isOrdered = io.readBoolean();
		this.codes = new2dIntArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.codes.length; i++) {
			io.readIntData(this.codes[i], this.codes[i].length);
		}
		this.codeLabels = readLabels(io, io.readInt());
	}
	protected RCharacterDataImpl readLabels(final RJIO io, final int l) throws IOException {
		return new RUniqueCharacterDataImpl(io, l);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeBoolean(this.isOrdered);
		for (int i = 0; i < this.codes.length; i++) {
			io.writeIntData(this.codes[i], this.codes[i].length);
		}
		io.writeInt(this.codeLabels.length());
		this.codeLabels.writeExternal(io);
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return false;
	}
	
	
	@Override
	public final long getLength() {
		return this.length;
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] <= 0);
	}
	
	@Override
	public boolean isNA(final long idx) {
		return (this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] <= 0);
	}
	
	@Override
	public void setNA(final int idx) {
		this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] =
				NA_integer_INT;
	}
	
	@Override
	public void setNA(final long idx) {
		this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] =
				NA_integer_INT;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] <= 0);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		return (this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] <= 0);
	}
	
	@Override
	public int getInt(final int idx) {
		return this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public int getInt(final long idx) {
		return this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setInt(final int idx, final int integer) {
		if (integer <= 0 || integer > this.codeLabels.length()) {
			throw new IllegalArgumentException();
		}
		this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] = integer;
	}
	
	@Override
	public void setInt(final long idx, final int integer) {
		if (integer <= 0 || integer > this.codeLabels.length()) {
			throw new IllegalArgumentException();
		}
		this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] = integer;
	}
	
	@Override
	public String getChar(final int idx) {
		final int v = this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (v > 0) ? this.codeLabels.getChar(v - 1): null;
	}
	
	@Override
	public String getChar(final long idx) {
		final int v = this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (v > 0) ? this.codeLabels.getChar(v - 1): null;
	}
	
	@Override
	public void setChar(final int idx, final String data) {
		final int code = this.codeLabels.indexOf(data, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] = code;
	}
	
	@Override
	public void setChar(final long idx, final String data) {
		final int code = this.codeLabels.indexOf(data, 0) + 1;
		if (code <= 0) {
			throw new IllegalArgumentException();
		}
		this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] = code;
	}
	
	
	@Override
	public RCharacterStore getLevels() {
		return this.codeLabels;
	}
	
	@Override
	public int getLevelCount() {
		return this.codeLabels.length();
	}
	
	@Override
	public RCharacterStore toCharacterData() {
		final String[][] data = new2dStringArray(this.length, SEGMENT_LENGTH);
		for (int i = 0; i < data.length; i++) {
			final String[] chars = data[i];
			final int[] ints = this.codes[i];
			for (int j = 0; j < ints.length; j++) {
				final int v = ints[j];
				if (v > 0) {
					chars[j] = this.codeLabels.getChar(v - 1);
				}
			}
		}
		return new RCharacterDataFixLongImpl(data);
	}
	
	
	@Override
	public Integer get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.codes[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
		return (v > 0) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		final int v = this.codes[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
		return (v > 0) ?
			Integer.valueOf(v) :
			null;
	}
	
	@Override
	public Integer[] toArray() {
		final int l = checkToArrayLength();
		final Integer[] array = new Integer[l];
		int k = 0;
		for (int i = 0; i < this.codes.length; i++, k++) {
			final int[] ints = this.codes[i];
			for (int j = 0; j < ints.length; j++) {
				final int v = ints[j];
				if (v > 0) {
					array[k] = Integer.valueOf(v);
				}
			}
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final int integer, long fromIdx) {
		if (integer <= 0 || integer > this.codeLabels.length()) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		int i = (int) (fromIdx / SEGMENT_LENGTH);
		int j = (int) (fromIdx % SEGMENT_LENGTH);
		while (i < this.codes.length) {
			final int[] ints = this.codes[i];
			while (j < ints.length) {
				if (ints[i] == integer) {
					return (i * (long) SEGMENT_LENGTH) + j;
				}
			}
			i++;
			j = 0;
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
