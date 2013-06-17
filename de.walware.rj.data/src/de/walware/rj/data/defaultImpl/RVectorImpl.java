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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;


public class RVectorImpl<DataType extends RStore> extends AbstractRObject
		implements RVector<DataType>, ExternalizableRObject {
	
	
	private DataType data;
	private int length;
	
	private String className1;
	private RStore namesAttribute;
	
	
	public RVectorImpl(final DataType data, final String className1) {
		this(data, data.getLength(), className1, null);
	}
	
	public RVectorImpl(final DataType data) {
		this(data, data.getLength(), data.getBaseVectorRClassName(), null);
	}
	
	public RVectorImpl(final DataType data, final String className1, final String[] initialNames) {
		this(data, data.getLength(), className1, initialNames);
	}
	
	public RVectorImpl(final DataType data, final int length, final String className1, final String[] initialNames) {
		if (data == null || className1 == null) {
			throw new NullPointerException();
		}
		if ((initialNames != null && initialNames.length != length)
				|| (data.getLength() >= 0 && data.getLength() != length) ) {
			throw new IllegalArgumentException();
		}
		this.data = data;
		this.length = length;
		this.className1 = className1;
		if (initialNames != null) {
			this.namesAttribute = new RCharacterDataImpl(initialNames);
		}
	}
	
	public RVectorImpl(final DataType data, final String className1, final RStore initialNames) {
		if (data == null || className1 == null) {
			throw new NullPointerException();
		}
		if ((initialNames != null && initialNames.getLength() != data.getLength())) {
			throw new IllegalArgumentException();
		}
		this.data = data;
		this.length = data.getLength();
		this.className1 = className1;
		this.namesAttribute = initialNames;
	}
	
	public RVectorImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		final boolean customClass = ((options & RObjectFactory.O_CLASS_NAME) != 0);
		//-- special attributes
		if (customClass) {
			this.className1 = io.readString();
		}
		this.length = io.readInt();
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			this.namesAttribute = factory.readNames(io);
		}
		//-- data
		this.data = (DataType) factory.readStore(io);
		if (!customClass) {
			this.className1 = this.data.getBaseVectorRClassName();
		}
		// attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = !this.className1.equals(this.data.getBaseVectorRClassName());
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.namesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		final RList attributes = ((io.flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		io.writeInt(options);
		//-- special attributes
		if (customClass) {
			io.writeString(this.className1);
		}
		io.writeInt(this.length);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			factory.writeNames(this.namesAttribute, io);
		}
		//-- data
		factory.writeStore(this.data, io);
		// attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			factory.writeAttributeList(attributes, io);
		}
	}
	
	
	public byte getRObjectType() {
		return TYPE_VECTOR;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public RStore getNames() {
		return this.namesAttribute;
	}
	
	
	public DataType getData() {
		return this.data;
	}
	
	
	public void setData(final DataType data) {
		this.data = data;
	}
	
	public void insert(final int idx) {
		((RDataResizeExtension) this.data).insertNA(idx);
	}
	
	public void remove(final int idx) {
		((RDataResizeExtension) this.data).remove(idx);
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=vector, class=").append(getRClassName());
		sb.append("\n\tlength=").append(getLength());
		sb.append("\n\tdata: ");
		sb.append(this.data.toString());
		return sb.toString();
	}
	
}
