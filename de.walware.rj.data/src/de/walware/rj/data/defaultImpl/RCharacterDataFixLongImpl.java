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

import java.io.IOException;
import java.util.Arrays;

import de.walware.rj.data.RJIO;


public class RCharacterDataFixLongImpl extends AbstractCharacterData
		implements ExternalizableRStore {
	
	
	public static final int SEGMENT_LENGTH = DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	
	protected final String[][] charValues;
	
	
	public RCharacterDataFixLongImpl(final long length) {
		this.length = length;
		this.charValues = new2dStringArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.charValues.length; i++) {
			Arrays.fill(this.charValues[i], "");
		}
	}
	
	public RCharacterDataFixLongImpl(final String[][] values) {
		this.length = check2dArrayLength(values, SEGMENT_LENGTH);
		this.charValues = values;
	}
	
	
	public RCharacterDataFixLongImpl(final RJIO io, final long length) throws IOException {
		this.length = length;
		this.charValues = new2dStringArray(length, SEGMENT_LENGTH);
		for (int i = 0; i < this.charValues.length; i++) {
			io.readStringData(this.charValues[i], this.charValues[i].length);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		for (int i = 0; i < this.charValues.length; i++) {
			io.writeStringData(this.charValues[i], this.charValues[i].length);
		}
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
		return (this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == null);
	}
	
	@Override
	public boolean isNA(final long idx) {
		return (this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == null);
	}
	
	@Override
	public void setNA(final int idx) {
		this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] = null;
	}
	
	@Override
	public void setNA(final long idx) {
		this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] = null;
	}
	
	@Override
	public boolean isMissing(final int idx) {
		return (this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] == null);
	}
	
	@Override
	public boolean isMissing(final long idx) {
		return (this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] == null);
	}
	
	@Override
	public String getChar(final int idx) {
		return this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public String getChar(final long idx) {
		return this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public void setChar(final int idx, final String value) {
//		assert (value != null);
		this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH] = value;
	}
	
	@Override
	public void setChar(final long idx, final String value) {
//		assert (value != null);
		this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)] = value;
	}
	
	
	@Override
	public String get(final int idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.charValues[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public String get(final long idx) {
		if (idx < 0 || idx >= this.length) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.charValues[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public String[] toArray() {
		final int l = checkToArrayLength();
		final String[] array = new String[l];
		int k = 0;
		for (int i = 0; i < this.charValues.length; i++) {
			final String[] chars = this.charValues[i];
			System.arraycopy(chars, 0, array, k, chars.length);
			k += chars.length;
		}
		return array;
	}
	
	
	@Override
	public long indexOf(final String character, long fromIdx) {
		if (character == null) {
			return -1;
		}
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		int i = (int) (fromIdx / SEGMENT_LENGTH);
		int j = (int) (fromIdx % SEGMENT_LENGTH);
		while (i < this.charValues.length) {
			final String[] chars = this.charValues[i];
			while (j < chars.length) {
				if (chars[j] != null && chars[j].equals(character)) {
					return (i * (long) SEGMENT_LENGTH) + j;
				}
			}
			i++;
			j = 0;
		}
		return -1;
	}
	
}
