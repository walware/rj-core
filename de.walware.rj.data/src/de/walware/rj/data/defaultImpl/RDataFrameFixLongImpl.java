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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RDataFrameFixLongImpl extends RListFixLongImpl
		implements RDataFrame, ExternalizableRObject {
	
	
	private final RStore<?> rownamesAttribute;
	private final long rowCount;
	
	
	public RDataFrameFixLongImpl(final RObject[][] columns, final String className1, final String[][] initialNames, final String[][] initialRownames) {
		this(columns, className1, initialNames, initialRownames, true);
	}
	
	protected RDataFrameFixLongImpl(final RObject[][] columns, final String className1, final String[][] initialNames, final String[][] initialRownames, final boolean check) {
		super(columns, className1, initialNames);
		if (columns.length == 0) {
			this.rowCount = 0;
		}
		else {
			this.rowCount = columns[0][0].getLength();
			if (check) {
				for (int i = 0; i < columns.length; i++) {
					final RObject[] segment = columns[i];
					for (int j = 0; j < segment.length; j++) {
						if (segment[j].getRObjectType() != RObject.TYPE_VECTOR
								|| (segment[j].getLength() != this.rowCount)) {
							throw new IllegalArgumentException("Length of column " + (i * (long) SEGMENT_LENGTH + j) + ": " + segment[j].getLength());
						}
					}
				}
			}
		}
		if (initialRownames != null) {
			this.rownamesAttribute = new RCharacterDataFixLongImpl(initialRownames);
			if (this.rownamesAttribute.getLength() != this.rowCount) {
				throw new IllegalArgumentException("Length of row names: " + this.rownamesAttribute.getLength());
			}
		}
		else {
			this.rownamesAttribute = null;
		}
	}
	
	public RDataFrameFixLongImpl(final RJIO io, final RObjectFactory factory, final int options) throws IOException {
		super(io, factory, options);
		
		this.rowCount = io.readLong();
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			this.rownamesAttribute = factory.readNames(io, this.rowCount);
		}
		else {
			this.rownamesAttribute = null;
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		int options = 0;
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.rownamesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		super.doWriteExternal(io, options, factory);
		io.writeLong(this.rowCount);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			factory.writeNames(this.rownamesAttribute, io);
		}
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_DATAFRAME;
	}
	
	@Override
	protected String getDefaultRClassName() {
		return RObject.CLASSNAME_DATAFRAME;
	}
	
	
	@Override
	public long getColumnCount() {
		return getLength();
	}
	
	@Override
	public RCharacterStore getColumnNames() {
		return getNames();
	}
	
	public String getColumnName(final int idx) {
		return getName(idx);
	}
	
	@Override
	public RStore<?> getColumn(final int idx) {
		final RObject obj = get(idx);
		return (obj != null) ? obj.getData() : null;
	}
	
	@Override
	public RStore<?> getColumn(final long idx) {
		final RObject obj = get(idx);
		return (obj != null) ? obj.getData() : null;
	}
	
	@Override
	public RStore<?> getColumn(final String name) {
		final RObject obj = get(name);
		return (obj != null) ? obj.getData() : null;
	}
	
	
	@Override
	public long getRowCount() {
		return this.rowCount;
	}
	
	@Override
	public RStore<?> getRowNames() {
		return this.rownamesAttribute;
	}
	
}
