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


public class RIntegerDataImpl extends AbstractIntegerData
		implements RDataReziseExtension, Externalizable {
	
	
	public static RIntegerDataImpl createForServer(final int[] values) {
		return new RIntegerDataImpl(values);
	}
	
	public static RIntegerDataImpl createFromWithNAList(final int[] values, final int[] naIdxs) {
		return new RIntegerDataImpl(values, naIdxs);
	}
	
	
	private int[] intValues;
	private int naCount;
	
	
	public RIntegerDataImpl() {
		this.intValues = new int[0];
		this.length = 0;
		this.naCount = 0;
	}
	
	private RIntegerDataImpl(final int[] values) {
		this.intValues = values;
		this.length = this.intValues.length;
	}
	
	private RIntegerDataImpl(final int[] values, final int[] naIdxs) {
		this.intValues = values;
		this.length = this.intValues.length;
		this.naCount = 0;
		if (naIdxs != null) {
			for (int i = 0; i < naIdxs.length; i++) {
				this.intValues[naIdxs[i]] = NA_integer_INT;
			}
			this.naCount = naIdxs.length;
		}
	}
	
	
	public RIntegerDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.length = in.readInt();
		this.naCount = 0;
		this.intValues = new int[this.length];
		for (int i = 0; i < this.length; i++) {
			if ((this.intValues[i] = in.readInt()) == NA_integer_INT) {
				this.naCount++;
			}
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			out.writeInt(this.intValues[i]);
		}
	}
	
	
	@Override
	public int getInt(final int idx) {
		return this.intValues[idx];
	}
	
	@Override
	public boolean hasNA() {
		return (this.naCount > 0);
	}
	
	@Override
	public boolean isNA(final int idx) {
		return (this.intValues[idx] == NA_integer_INT);
	}
	
	@Override
	public void setInt(final int idx, final int value) {
		assert (value != NA_integer_INT);
		if (this.intValues[idx] == NA_integer_INT) {
			this.naCount --;
		}
		this.intValues[idx] = value;
	}
	
	@Override
	public void setNA(final int idx) {
		if (this.intValues[idx] == NA_integer_INT) {
			return;
		}
		this.intValues[idx] = NA_integer_INT;
	}
	
	private void prepareInsert(final int[] idxs) {
		this.intValues = prepareInsert(this.intValues, this.length, idxs);
		this.length += idxs.length;
	}
	
	public void insertInt(final int idx, final int value) {
		assert (value != NA_integer_INT);
		prepareInsert(new int[] { idx });
		this.intValues[idx] = value;
	}
	
	public void insertNA(final int idx) {
		prepareInsert(new int[] { idx });
		this.intValues[idx] = NA_integer_INT;
		this.naCount ++;
	}
	
	public void insertNA(final int[] idxs) {
		if (idxs.length == 0) {
			return;
		}
		prepareInsert(idxs);
		for (int idx = 0; idx < idxs.length; idx++) {
			this.intValues[idxs[idx]+idx] = NA_integer_INT;
		}
		this.naCount += idxs.length;
	}
	
	public void remove(final int idx) {
		if (this.intValues[idx] == NA_integer_INT) {
			this.naCount --;
		}
		this.intValues = remove(this.intValues, this.length, new int[] { idx });
		this.length --;
	}
	
	public void remove(final int[] idxs) {
		for (int i = 0; i < idxs.length; i++) {
			if (this.intValues[idxs[i]] == NA_integer_INT) {
				this.naCount --;
			}
		}
		this.intValues = remove(this.intValues, this.length, idxs);
		this.length -= idxs.length;
	}
	
}
