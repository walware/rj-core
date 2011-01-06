/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jriImpl;

import java.io.IOException;
import java.util.Arrays;

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.AbstractRObject;
import de.walware.rj.data.defaultImpl.ExternalizableRObject;
import de.walware.rj.data.defaultImpl.ExternalizableRStore;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.SimpleRListImpl;


public class JRIArrayImpl<DataType extends RStore> extends AbstractRObject
		implements RArray<DataType>, ExternalizableRObject {
	
	
	private DataType data;
	
	private String className1;
	private int[] dimAttribute;
	private SimpleRListImpl<RStore> dimnamesAttribute;
	
	
	public JRIArrayImpl(final DataType data, final String className1, final int[] dim,
			final SimpleRListImpl<RStore> dimnames) {
		this.className1 = className1;
		this.dimAttribute = dim;
		this.data = data;
		this.dimnamesAttribute = dimnames;
	}
	
	public JRIArrayImpl(final DataType data, final String className1, final int[] dim) {
		this.className1 = className1;
		this.dimAttribute = dim;
		this.data = data;
	}
	
	public JRIArrayImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.in.readInt();
		final boolean customClass = ((options & RObjectFactory.O_CLASS_NAME) != 0);
		//-- special attributes
		if (customClass) {
			this.className1 = io.readString();
		}
		final int[] dim = io.readIntArray();
		this.dimAttribute = dim;
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			final RCharacterDataImpl names0 = new RCharacterDataImpl(io);
			final RStore[] names1 = new RStore[dim.length];
			for (int i = 0; i < dim.length; i++) {
				names1[i] = factory.readNames(io);
			}
			this.dimnamesAttribute = new SimpleRListImpl<RStore>(names0, names1);
		}
		//-- data
		this.data = (DataType) factory.readStore(io);
		
		if (!customClass) {
			this.className1 = (dim.length == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY;
		}
		//-- attributes
		if ((options & RObjectFactory.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = this.className1 != null
				&& !this.className1.equals((this.dimAttribute.length == 2) ?
						RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY);
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.dimnamesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		final RList attributes = ((io.flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		io.out.writeInt(options);
		//-- special attributes
		if (customClass) {
			io.writeString(this.className1);
		}
		io.writeIntArray(this.dimAttribute, this.dimAttribute.length);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			((ExternalizableRStore) this.dimnamesAttribute.getNames()).writeExternal(io);
			for (int i = 0; i < this.dimAttribute.length; i++) {
				factory.writeNames(this.dimnamesAttribute.get(i), io);
			}
		}
		//-- data
		factory.writeStore(this.data, io);
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			factory.writeAttributeList(attributes, io);
		}
	}
	
	
	public final byte getRObjectType() {
		return TYPE_ARRAY;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	public int getLength() {
		if (this.dimAttribute.length == 0) {
			return 0;
		}
		int length = this.data.getLength();
		if (length >= 0) {
			return length;
		}
		length = 1;
		for (int i = 0; i < this.dimAttribute.length; i++) {
			length *= this.dimAttribute[i];
		}
		return length;
	}
	
	public RIntegerStore getDim() {
		return new JRIIntegerDataImpl(this.dimAttribute);
	}
	
	public RCharacterStore getDimNames() {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.getNames();
		}
		return null;
	}
	
	public RStore getNames(final int dim) {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.get(dim);
		}
		return null;
	}
	
	
	public DataType getData() {
		return this.data;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=array, class=").append(getRClassName());
		sb.append("\n\tlength=").append(getLength());
		sb.append("\n\tdim=");
		sb.append(Arrays.toString(this.dimAttribute));
		sb.append("\n\tdata: ");
		sb.append(this.data.toString());
		return sb.toString();
	}
	
	
	public int[] getJRIDimArray() {
		return this.dimAttribute;
	}
	
}
