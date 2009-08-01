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

package de.walware.rj.server.jriImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.AbstractRObject;
import de.walware.rj.data.defaultImpl.ExternalizableRObject;


public class JRIArrayImpl<DataType extends RStore> extends AbstractRObject
		implements RArray<DataType>, ExternalizableRObject {
	
	
	private DataType data;
	
	private String className1;
	private int[] dimAttribute;
	private List<RCharacterStore> dimnamesAttribute;
	
	
	public JRIArrayImpl(final DataType data, final String className1, final int[] dim) {
		this.className1 = className1;
		this.dimAttribute = dim;
		this.data = data;
	}
	
	public JRIArrayImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		//-- options
		final int options = in.readInt();
		final boolean customClass = ((options & RObjectFactory.O_CLASS_NAME) != 0);
		//-- special attributes
		if (customClass) {
			this.className1 = in.readUTF();
		}
		final int dimCount = in.readInt();
		final int[] dim = new int[dimCount];
		for (int i = 0; i < dimCount; i++) {
			dim[i] = in.readInt();
		}
		this.dimAttribute = dim;
//		this.namesAttribute = new RCharacterDataImpl(in);
		//-- data
		this.data = (DataType) factory.readStore(in, flags);
		
		if (!customClass) {
			this.className1 = (dimCount == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY;
		}
		//-- attributes
		if ((options & RObjectFactory.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(in, flags));
		}
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = this.className1 != null
				&& !this.className1.equals((this.dimAttribute.length == 2) ? RObject.CLASSNAME_MATRIX : RObject.CLASSNAME_ARRAY);
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		final RList attributes = ((flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		out.writeInt(options);
		//-- special attributes
		if (customClass) {
			out.writeUTF(this.className1);
		}
		final int dimCount = this.dimAttribute.length;
		out.writeInt(dimCount);
		for (int i = 0; i < dimCount; i++) {
			out.writeInt(this.dimAttribute[i]);
		}
//		this.namesAttribute.writeExternal(out);
		//-- data
		factory.writeStore(this.data, out, flags);
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, out, flags);
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
	
	public List<RCharacterStore> getDimNames() {
		return this.dimnamesAttribute;
	}
	
	public DataType getData() {
		return this.data;
	}
	
	
	protected int[] getDataInsertIdxs(final int dim, final int idx) {
		if (dim >= this.dimAttribute.length || idx >= this.dimAttribute[dim]) {
			throw new IllegalArgumentException();
		}
		final int size = this.data.getLength() / this.dimAttribute[dim];
		final int[] dataIdxs = new int[size];
		int step = 1;
		int stepCount = 1;
		for (int currentDimI = 0; currentDimI < this.dimAttribute.length; currentDimI++) {
			final int currentDimLength = this.dimAttribute[currentDimI];
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
		sb.append("\n\tdim=").append(Arrays.toString(this.dimAttribute));
		sb.append("\n\tdata: ");
		sb.append(this.data.toString());
		return sb.toString();
	}
	
	
	public int[] getJRIDimArray() {
		return this.dimAttribute;
	}
	
}
