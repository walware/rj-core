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

import de.walware.rj.data.RArray;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RList;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;


public class RObjectFactoryImpl implements RObjectFactory {
	
	
	public static final RObjectFactory INSTANCE = new RObjectFactoryImpl();
	
	public static final RNumericStore NUM_STRUCT_DUMMY = new RNumericDataStruct(-1);
	public static final RComplexDataStruct CPLX_STRUCT_DUMMY = new RComplexDataStruct(-1);
	public static final RIntegerDataStruct INT_STRUCT_DUMMY = new RIntegerDataStruct(-1);
	public static final RLogicalDataStruct LOGI_STRUCT_DUMMY = new RLogicalDataStruct(-1);
	public static final RRawDataStruct RAW_STRUCT_DUMMY = new RRawDataStruct(-1);
	public static final RCharacterDataStruct CHR_STRUCT_DUMMY = new RCharacterDataStruct(-1);
	
	
	public <DataType extends RStore> RVector<DataType> createVector(final DataType data) {
		return new RVectorImpl<DataType>(data, data.getBaseVectorRClassName());
	}
	
	public <DataType extends RStore> RArray<DataType> createArray(final DataType data, final int[] dim) {
		return new RArrayImpl<DataType>(data, RObject.CLASSNAME_ARRAY, dim);
	}
	
	public RList createList(final RObject[] components, final String[] names) {
		return new RListImpl(components, "list", names);
	}
	
	
	public RObject readObject(final ObjectInput in, final int flags) throws IOException, ClassNotFoundException {
		final int type = in.readInt();
		switch (type) {
		case -1:
			return null;
		case RObject.TYPE_NULL:
			return RNull.INSTANCE;
		case RObject.TYPE_VECTOR: {
			return new RVectorImpl(in, flags, this); }
		case RObject.TYPE_ARRAY:
			return new RArrayImpl(in, flags, this);
		case RObject.TYPE_LIST:
			return new RListImpl(in, flags, this);
		case RObject.TYPE_DATAFRAME:
			return new RDataFrameImpl(in, flags, this);
		case RObject.TYPE_ENV:
			return new REnvironmentImpl(in, flags, this);
		case RObject.TYPE_FUNCTION:
			return new RFunctionImpl(in, flags, this);
		case RObject.TYPE_REFERENCE:
			return new RReferenceImpl(in, flags, this);
		case RObject.TYPE_S4OBJECT:
			return new RS4ObjectImpl(in, flags, this);
		case RObject.TYPE_OTHER:
			return new ROtherImpl(in, flags, this);
		default:
			throw new IOException("object type = " + type);
		}
	}
	
	public void writeObject(final RObject robject, final ObjectOutput out, final int flags) throws IOException {
		if (robject == null) {
			out.writeInt(-1);
			return;
		}
		final int type = robject.getRObjectType();
		out.writeInt(type);
		switch (type) {
		case RObject.TYPE_NULL:
			return;
		case RObject.TYPE_VECTOR:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_ARRAY:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_LIST:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_DATAFRAME:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_ENV:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_FUNCTION:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_REFERENCE:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_S4OBJECT:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		case RObject.TYPE_OTHER:
			if (robject instanceof ExternalizableRObject) {
				((ExternalizableRObject) robject).writeExternal(out, flags, this);
				return;
			}
			throw new UnsupportedOperationException();
		default:
			throw new IOException("object type = " + type);
		}
	}
	
	public RStore readStore(final ObjectInput in, final int flags) throws IOException, ClassNotFoundException {
		if ((flags & F_ONLY_STRUCT) == 0) {
			final int storeType = in.readInt();
			switch (storeType) {
			case RStore.NUMERIC:
				return RNumericDataBImpl.isBforNASupported() ? new RNumericDataBImpl(in) : new RNumericDataImpl(in);
			case RStore.CHARACTER:
				return new RCharacterDataImpl(in);
			case RStore.INTEGER:
				return new RIntegerDataImpl(in);
			case RStore.LOGICAL:
				return new RLogicalDataByteImpl(in);
			case RStore.RAW:
				return new RRawDataStruct(in.readInt());
//				return new RRawDataImpl(in);
			case RStore.FACTOR:
				return new RFactorDataImpl(in);
			case RStore.COMPLEX:
				return RComplexDataBImpl.isBforNASupported() ? new RComplexDataBImpl(in) : new RComplexDataImpl(in);
			default:
				throw new IOException("store type = " + storeType);
			}
		}
		else {
			final int storeType = in.readInt();
			switch (storeType) {
			case RStore.LOGICAL:
				return LOGI_STRUCT_DUMMY;
			case RStore.INTEGER:
				return INT_STRUCT_DUMMY;
			case RStore.NUMERIC:
				return NUM_STRUCT_DUMMY;
			case RStore.COMPLEX:
				return CPLX_STRUCT_DUMMY;
			case RStore.CHARACTER:
				return CHR_STRUCT_DUMMY;
			case RStore.RAW:
				return RAW_STRUCT_DUMMY;
			case RStore.FACTOR:
				return new RFactorDataStruct(-1, in.readBoolean(), in.readInt());
			default:
				throw new IOException("store type = " + storeType);
			}
		}
	}
	
	public void writeStore(final RStore data, final ObjectOutput out, final int flags) throws IOException {
		if ((flags & F_ONLY_STRUCT) == 0) {
			out.writeInt(data.getStoreType());
			if (data instanceof Externalizable) {
				((Externalizable) data).writeExternal(out);
				return;
			}
			else {
				throw new IOException();
			}
		}
		else {
			final int storeType = data.getStoreType();
			out.writeInt(storeType);
			if (storeType == RStore.FACTOR) {
				final RFactorStore factor = (RFactorStore) data;
				out.writeBoolean(factor.isOrdered());
				out.writeInt(factor.getLevelCount());
			}
		}
	}
	
	public RList readAttributeList(final ObjectInput in, final int flags) throws IOException, ClassNotFoundException {
		return new RListImpl(in, flags, this);
	}
	
	public void writeAttributeList(final RList list, final ObjectOutput out, final int flags) throws IOException {
		if (list instanceof ExternalizableRObject) {
			((ExternalizableRObject) list).writeExternal(out, flags, this);
			return;
		}
		throw new UnsupportedOperationException();
	}
	
	protected final int[] readDim(final ObjectInput in) throws IOException {
		final int length = in.readInt();
		final int[] dim = new int[length];
		for (int i = 0; i < length; i++) {
			dim[i] = in.readInt();
		}
		return dim;
	}
	
	protected final void writeDim(final int[] dim, final ObjectOutput out) throws IOException {
		final int length = dim.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			out.writeInt(dim[i]);
		}
	}
	
}
