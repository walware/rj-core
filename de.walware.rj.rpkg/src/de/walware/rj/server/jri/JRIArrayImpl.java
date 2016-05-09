/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import java.io.IOException;
import java.util.Arrays;

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RDataUtil;
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


public class JRIArrayImpl<DataType extends RStore<?>> extends AbstractRObject
		implements RArray<DataType>, ExternalizableRObject {
	
	
	private long length;
	private DataType data;
	
	private String className1;
	private int[] dimAttribute;
	private SimpleRListImpl<? extends RStore<?>> dimnamesAttribute;
	
	
	public JRIArrayImpl(final DataType data, final String className1, final int[] dim,
			final SimpleRListImpl<RCharacterStore> dimnames) {
		this.length = (data.getLength() >= 0) ? data.getLength() : RDataUtil.computeLengthFromDim(dim);
		this.data = data;
		this.className1 = className1;
		this.dimAttribute = dim;
		this.dimnamesAttribute = dimnames;
	}
	
	public JRIArrayImpl(final DataType data, final String className1, final int[] dim) {
		this.length = (data.getLength() >= 0) ? data.getLength() : RDataUtil.computeLengthFromDim(dim);
		this.data = data;
		this.className1 = className1;
		this.dimAttribute = dim;
	}
	
	public JRIArrayImpl(final RJIO io, final RObjectFactory factory) throws IOException {
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
		this.length = io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK));
		final int[] dim = io.readIntArray();
		this.dimAttribute = dim;
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			final RCharacterDataImpl names0 = new RCharacterDataImpl(io, dim.length);
			final RStore<?>[] names1 = new RStore[dim.length];
			for (int i = 0; i < dim.length; i++) {
				names1[i] = factory.readNames(io, dim[i]);
			}
			this.dimnamesAttribute = new SimpleRListImpl<>(names1, names0);
		}
		//-- data
		this.data = (DataType) factory.readStore(io, this.length);
		
		if (!customClass) {
			this.className1 = (dim.length == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY;
		}
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		int options = io.getVULongGrade(this.length);
		if (this.className1 != null
				&& !this.className1.equals((this.dimAttribute.length == 2) ?
						RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY )) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.dimnamesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		final RList attributes = ((io.flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		io.writeInt(options);
		//-- special attributes
		if ((options & RObjectFactory.O_CLASS_NAME) != 0) {
			io.writeString(this.className1);
		}
		io.writeVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK), this.length);
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
	
	
	@Override
	public final byte getRObjectType() {
		return TYPE_ARRAY;
	}
	
	@Override
	public String getRClassName() {
		return (this.className1 != null) ? this.className1 :
				((this.dimAttribute.length == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY);
	}
	
	@Override
	public long getLength() {
		return this.length;
	}
	
	@Override
	public RIntegerStore getDim() {
		return new JRIIntegerDataImpl(this.dimAttribute);
	}
	
	@Override
	public RCharacterStore getDimNames() {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.getNames();
		}
		return null;
	}
	
	@Override
	public RStore<?> getNames(final int dim) {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.get(dim);
		}
		return null;
	}
	
	
	@Override
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
