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

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RArrayImpl<DataType extends RStore> extends AbstractRObject
		implements RArray<DataType>, ExternalizableRObject {
	
	
	public static final void checkDim(final int length, final int[] dim) {
		int r;
		if (dim.length == 0) {
			r = 0;
		}
		else {
			r = 1;
			for (int i = 0; i < dim.length; i++) {
				r *= dim[i];
			}
		}
		if (r != length) {
			throw new IllegalArgumentException("dim");
		}
	}
	
	
	private DataType data;
	
	private String className1;
	private RIntegerDataImpl dimAttribute;
	private SimpleRListImpl<RStore> dimnamesAttribute;
	
	
	public RArrayImpl(final DataType data, final String className1, final int[] dim) {
		if (data == null || className1 == null || dim == null) {
			throw new NullPointerException();
		}
		if (data.getLength() >= 0) {
			checkDim(data.getLength(), dim);
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
		final boolean customClass = ((options & RObjectFactory.O_CLASS_NAME) != 0);
		//-- special attributes
		if (customClass) {
			this.className1 = io.readString();
		}
		final int[] dim = io.readIntArray();
		this.dimAttribute = new RIntegerDataImpl(dim);
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
		final boolean customClass = !this.className1.equals((this.dimAttribute.getLength() == 2) ?
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
		io.writeInt(options);
		//-- special attributes
		if (customClass) {
			io.writeString(this.className1);
		}
		this.dimAttribute.writeExternal(io);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			((ExternalizableRStore) this.dimnamesAttribute.getNames()).writeExternal(io);
			for (int i = 0; i < this.dimnamesAttribute.getLength(); i++) {
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
		for (int i = 0; i < this.dimAttribute.getLength(); i++) {
			length *= this.dimAttribute.getInt(i);
		}
		return length;
	}
	
	public RIntegerStore getDim() {
		return this.dimAttribute;
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
	
	
	protected int[] getDataInsertIdxs(final int dim, final int idx) {
		if (dim >= this.dimAttribute.length || idx >= this.dimAttribute.intValues[dim]) {
			throw new IllegalArgumentException();
		}
		final int size = this.data.getLength() / this.dimAttribute.intValues[dim];
		final int[] dataIdxs = new int[size];
		int step = 1;
		int stepCount = 1;
		for (int currentDimI = 0; currentDimI < this.dimAttribute.length; currentDimI++) {
			final int currentDimLength = this.dimAttribute.intValues[currentDimI];
			if (currentDimI == dim) {
				final int add = step*idx;
				for (int i = 0; i < size; i++) {
					dataIdxs[i] += add;
				}
			}
			else {
				if (currentDimI > 0) {
					int temp = 0;
					for (int i = 0; i < size; ) {
						final int add = step*temp;
						for (int j = 0; j < stepCount && i < size; i++,j++) {
							dataIdxs[i] += add;
						}
						temp++;
						if (temp == currentDimLength) {
							temp = 0;
						}
					}
				}
				stepCount *= currentDimLength;
			}
			step *= currentDimLength;
		}
		return dataIdxs;
	}
	
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
