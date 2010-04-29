/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RDataFrameImpl extends RListImpl
		implements RDataFrame, ExternalizableRObject {
	
	
	private RStore rownamesAttribute;
	private int rowCount;
	
	
	public RDataFrameImpl(final RObject[] columns, final String className1, final String[] initialNames, final String[] initialRownames) {
		this(columns, className1, initialNames, initialRownames, true);
	}
	
	protected RDataFrameImpl(final RObject[] columns, final String className1, final String[] initialNames, final String[] initialRownames, final boolean check) {
		super(columns, className1, initialNames);
		if (columns.length == 0) {
			this.rowCount = 0;
		}
		else {
			this.rowCount = columns[0].getLength();
			if (check) {
				for (int i = 0; i < columns.length; i++) {
					if (columns[i].getRObjectType() != RObject.TYPE_VECTOR
							|| (columns[i].getLength() >= 0 && columns[i].getLength() != this.rowCount)) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		if (initialRownames != null && initialRownames.length == this.rowCount) {
			this.rownamesAttribute = new RUniqueCharacterDataImpl(initialRownames);
		}
	}
	
	public RDataFrameImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		super(io, factory);
	}
	
	@Override
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int options = super.doReadExternal(io, factory);
		this.rowCount = io.in.readInt();
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			this.rownamesAttribute = factory.readNames(io);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		int options = 0;
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.rownamesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		super.doWriteExternal(io, options, factory);
		io.out.writeInt(this.rowCount);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			factory.writeNames(this.rownamesAttribute, io);
		}
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_DATAFRAME;
	}
	
	
	public int getColumnCount() {
		return getLength();
	}
	
	public RCharacterStore getColumnNames() {
		return getNames();
	}
	
	public String getColumnName(final int idx) {
		return getName(idx);
	}
	
	public RStore getColumn(final int idx) {
		final RObject obj = get(idx);
		return (obj != null) ? obj.getData() : null;
	}
	
	public RStore getColumn(final String name) {
		final RObject obj = get(name);
		return (obj != null) ? obj.getData() : null;
	}
	
//	public void setColumn(final int idx, final RStore column) {
//		if (this.length == 0) {
//			throw new IndexOutOfBoundsException();
//		}
//		else {
//			if (column.getLength() != this.rowCount) {
//				throw new IllegalArgumentException();
//			}
//		}
//		this.columns[idx] = new RVectorImpl(column, RDataUtil.getStoreVectorClass(column));
//	}
	
	@Override
	public boolean set(final int idx, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		if (getLength() == 0) {
			throw new IndexOutOfBoundsException();
		}
		else {
			if (component.getRObjectType() != RObject.TYPE_VECTOR) {
				throw new IllegalArgumentException();
			}
			if (component.getLength() != this.rowCount) {
				throw new IllegalArgumentException();
			}
		}
		return super.set(idx, component);
	}
	
//	public void insertColumn(final int idx, final String name, final RStore column) {
//		if (column == null) {
//			throw new NullPointerException();
//		}
//		if (column.getDataType() <= 0) {
//			throw new IllegalArgumentException();
//		}
//		if (this.length == 0) {
//			this.rowCount = column.getLength();
//		}
//		else if (this.rowCount != column.getLength()) {
//			throw new IllegalArgumentException();
//		}
//		prepareInsert(idx);
//		this.columns[idx] = new RVectorImpl(column, RDataUtil.getStoreVectorClass(column));
//		this.namesAttribute.insertChar(idx, name);
//	}
//
	
	
	public int getRowCount() {
		return this.rowCount;
	}
	
	public RStore getRowNames() {
		return this.rownamesAttribute;
	}
	
	public void insertRow(final int idx) {
		final int length = getLength();
		for (int i = 0; i < length; i++) {
			((RDataResizeExtension) get(i)).insertNA(idx);
		}
		this.rowCount++;
//		if (this.rownamesAttribute != null) {
//			((RDataResizeExtension) this.rownamesAttribute).insertAuto(idx);
//		}
	}
	
	public void removeRow(final int idx) {
		final int length = getLength();
		for (int i = 0; i < length; i++) {
			((RDataResizeExtension) this.get(i)).remove(idx);
		}
		this.rowCount--;
//		if (this.rownamesAttribute != null) {
//			this.rownamesAttribute.remove(idx);
//		}
	}
	
}
