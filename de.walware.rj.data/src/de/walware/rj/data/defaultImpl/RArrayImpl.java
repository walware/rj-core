/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RArrayImpl<DataType extends RStore> extends AbstractRObject
		implements RArray<DataType>, ExternalizableRObject {
	
	
	private long length;
	private DataType data;
	
	private String className1;
	private RIntegerDataImpl dimAttribute;
	private SimpleRListImpl<RStore> dimnamesAttribute;
	
	
	public RArrayImpl(final DataType data, final String className1, final int[] dim) {
		if (data == null || className1 == null || dim == null) {
			throw new NullPointerException();
		}
		this.length = RDataUtil.computeLengthFromDim(dim);
		if (data.getLength() >= 0 && data.getLength() != this.length) {
			throw new IllegalArgumentException("dim");
		}
		this.className1 = className1;
		this.dimAttribute = new RIntegerDataImpl(dim);
		this.data = data;
	}
	
	public RArrayImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		//-- special attributes
		if ((options & RObjectFactory.O_CLASS_NAME) != 0) {
			this.className1 = io.readString();
		}
		this.length = io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK));
		final int[] dim = io.readIntArray();
		this.dimAttribute = new RIntegerDataImpl(dim);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			final RCharacterDataImpl names0 = new RCharacterDataImpl(io, dim.length);
			final RStore[] names1 = new RStore[dim.length];
			for (int i = 0; i < dim.length; i++) {
				names1[i] = factory.readNames(io, dim[i]);
			}
			this.dimnamesAttribute = new SimpleRListImpl<RStore>(names1, names0);
		}
		//-- data
		this.data = (DataType) factory.readStore(io, this.length);
		
		if ((options & RObjectFactory.O_CLASS_NAME) == 0) {
			this.className1 = (dim.length == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY;
		}
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int n = this.dimAttribute.length();
		//-- options
		int options = io.getVULongGrade(this.length);
		if (!this.className1.equals((n == 2) ?
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
		io.writeInt(n);
		this.dimAttribute.writeExternal(io);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			((ExternalizableRStore) this.dimnamesAttribute.getNames()).writeExternal(io);
			for (int i = 0; i < n; i++) {
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
		return this.className1;
	}
	
	@Override
	public long getLength() {
		return this.length;
	}
	
	@Override
	public RIntegerStore getDim() {
		return this.dimAttribute;
	}
	
	@Override
	public RCharacterStore getDimNames() {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.getNames();
		}
		return null;
	}
	
	@Override
	public RStore getNames(final int dim) {
		if (this.dimnamesAttribute != null) {
			return this.dimnamesAttribute.get(dim);
		}
		return null;
	}
	
	
	@Override
	public DataType getData() {
		return this.data;
	}
	
	
//	protected int[] getDataInsertIdxs(final int dim, final int idx) {
//		if (dim >= this.dimAttribute.length || idx >= this.dimAttribute.intValues[dim]) {
//			throw new IllegalArgumentException();
//		}
//		final int size = this.data.getLength() / this.dimAttribute.intValues[dim];
//		final int[] dataIdxs = new int[size];
//		int step = 1;
//		int stepCount = 1;
//		for (int currentDimI = 0; currentDimI < this.dimAttribute.length; currentDimI++) {
//			final int currentDimLength = this.dimAttribute.intValues[currentDimI];
//			if (currentDimI == dim) {
//				final int add = step*idx;
//				for (int i = 0; i < size; i++) {
//					dataIdxs[i] += add;
//				}
//			}
//			else {
//				if (currentDimI > 0) {
//					int temp = 0;
//					for (int i = 0; i < size; ) {
//						final int add = step*temp;
//						for (int j = 0; j < stepCount && i < size; i++,j++) {
//							dataIdxs[i] += add;
//						}
//						temp++;
//						if (temp == currentDimLength) {
//							temp = 0;
//						}
//					}
//				}
//				stepCount *= currentDimLength;
//			}
//			step *= currentDimLength;
//		}
//		return dataIdxs;
//	}
//	
//	public void setDim(final int[] dim) {
//		checkDim(getLength(), dim);
//		this.dimAttribute = dim;
//	}
//	
//	public void setDimNames(final List<RCharacterStore> list) {
//	}
	
//	public void insert(final int dim, final int idx) {
//		((RDataResizeExtension) this.data).insertNA(getDataInsertIdxs(dim, idx));
//		this.dimAttribute[dim]++;
//		if (this.dimnamesAttribute != null) {
//			final RVector<RCharacterStore> names = (RVector<RCharacterStore>) this.dimnamesAttribute.get(dim);
//			if (names != null) {
//				names.insert(idx);
//			}
//		}
//	}
//	
//	public void remove(final int dim, final int idx) {
//		((RDataResizeExtension) this.data).remove(RDataUtil.getDataIdxs(this.dimAttribute, dim, idx));
//		this.dimAttribute[dim]--;
//		if (this.dimnamesAttribute != null) {
//			final RVector<RCharacterStore> names = (RVector<RCharacterStore>) this.dimnamesAttribute.get(dim);
//			if (names != null) {
//				names.remove(idx);
//			}
//		}
//	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=array, class=").append(getRClassName());
		sb.append("\n\tlength=").append(getLength());
		sb.append("\n\tdim=");
		this.dimAttribute.appendTo(sb);
		sb.append("\n\tdata: ");
		sb.append(this.data.toString());
		return sb.toString();
	}
	
}
