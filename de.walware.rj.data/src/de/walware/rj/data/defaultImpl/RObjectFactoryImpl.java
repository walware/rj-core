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

package de.walware.rj.data.defaultImpl;

import java.io.IOException;
import java.io.ObjectInput;

import de.walware.rj.data.RArray;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RLanguage;
import de.walware.rj.data.RList;
import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;


public class RObjectFactoryImpl implements RObjectFactory {
	
	
	public static final RObjectFactoryImpl INSTANCE = new RObjectFactoryImpl();
	
	public static final RNumericStore NUM_STRUCT_DUMMY = new RNumericDataStruct();
	public static final RComplexDataStruct CPLX_STRUCT_DUMMY = new RComplexDataStruct();
	public static final RIntegerDataStruct INT_STRUCT_DUMMY = new RIntegerDataStruct();
	public static final RLogicalDataStruct LOGI_STRUCT_DUMMY = new RLogicalDataStruct();
	public static final RRawDataStruct RAW_STRUCT_DUMMY = new RRawDataStruct();
	public static final RCharacterDataStruct CHR_STRUCT_DUMMY = new RCharacterDataStruct();
	
	
	private static int getArrayLength(final int[] dim) {
		if (dim.length == 0) {
			return 0;
		}
		int length = 1;
		for (int i = 0; i < dim.length; i++) {
			length *= dim[i];
		}
		return length;
	}
	
	
	public RObjectFactoryImpl() {
	}
	
	
	/*-- Vector --*/
	
	/**
	 * Creates an R vector with the given R data store and R class name.
	 * 
	 * @param data the data store
	 * @param classname the R class name
	 * @return the R vector
	 */
	public <DataType extends RStore> RVector<DataType> createVector(final DataType data, final String classname) {
		return new RVectorImpl<DataType>(data, classname);
	}
	
	/**
	 * Creates an R vector with the given R data store.
	 * <p>
	 * The vector has the default R class name for data type of the given store.</p>
	 * 
	 * @param data the data store
	 * @return the R vector
	 */
	public <DataType extends RStore> RVector<DataType> createVector(final DataType data) {
		return createVector(data, data.getBaseVectorRClassName());
	}
	
	
	/**
	 * Creates an R logical vector with values from a Java boolean array.
	 * <p>
	 * The vector has the default R class name 'logical'.</p>
	 * <p>
	 * Note that the R vector may use the array directly. For values
	 * see {@link RStore#setLogi(int, boolean)}.</p>
	 * 
	 * @param logicals the logical values
	 * @return the R logical vector
	 */
	public RVector<RLogicalStore> createLogiVector(final boolean[] logicals) {
		return createVector(createLogiData(logicals), RObject.CLASSNAME_LOGICAL);
	}
	
	/**
	 * Creates an R logical vector of the given length.
	 * <p>
	 * The vector has the default R class name 'logical'.</p>
	 * <p>
	 * The function works analog to the R function <code>logical(length)</code>;
	 * the vector is initialized with FALSE values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R logical vector
	 */
	public RVector<RLogicalStore> createLogiVector(final int length) {
		return createVector(createLogiData(length), RObject.CLASSNAME_LOGICAL);
	}
	
	/**
	 * Creates an R integer vector with values from a Java integer array.
	 * <p>
	 * The vector has the default R class name 'integer'.</p>
	 * <p>
	 * Note that the R vector may use the array directly. For values
	 * see {@link RStore#setInt(int, int)}.</p>
	 * 
	 * @param integers the integer values
	 * @return the R integer vector
	 */
	public RVector<RIntegerStore> createIntVector(final int[] integers) {
		return createVector(createIntData(integers), RObject.CLASSNAME_INTEGER);
	}
	
	/**
	 * Creates an R integer vector of the given length.
	 * <p>
	 * The vector has the default R class name 'integer'.</p>
	 * <p>
	 * The function works analog to the R function <code>integer(length)</code>;
	 * the vector is initialized with 0 values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R integer vector
	 */
	public RVector<RIntegerStore> createIntVector(final int length) {
		return createVector(createIntData(length), RObject.CLASSNAME_INTEGER);
	}
	
	/**
	 * Creates an R numeric vector with values from a Java double array.
	 * <p>
	 * The vector has the default R class name 'numeric'.</p>
	 * <p>
	 * Note that the R vector may use the array directly. For values
	 * see {@link RStore#setNum(int, double)}.</p>
	 * 
	 * @param numerics the numerics values
	 * @return the R numeric vector
	 */
	public RVector<RNumericStore> createNumVector(final double[] numerics) {
		return createVector(createNumData(numerics), RObject.CLASSNAME_NUMERIC);
	}
	
	/**
	 * Creates an R numeric vector of the given length.
	 * <p>
	 * The vector has the default R class name 'numeric'.</p>
	 * <p>
	 * The function works analog to the R function <code>numeric(length)</code>;
	 * the vector is initialized with 0.0 values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R numeric vector
	 */
	public RVector<RNumericStore> createNumVector(final int length) {
		return createVector(createNumData(length), RObject.CLASSNAME_NUMERIC);
	}
	
	/**
	 * Creates an R complex vector of the given length.
	 * <p>
	 * The vector has the default R class name 'complex'.</p>
	 * <p>
	 * The function works analog to the R function <code>complex(length)</code>;
	 * the vector is initialized with 0.0 values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R complex vector
	 */
	public RVector<RComplexStore> createCplxVector(final int length) {
		return createVector(createCplxData(length), RObject.CLASSNAME_COMPLEX);
	}
	
	/**
	 * Creates an R character vector with values from a Java String array.
	 * <p>
	 * The vector has the default R class name 'character'.</p>
	 * <p>
	 * Note that the R vector may use the array directly. For values
	 * see {@link RStore#setChar(int, String)}.</p>
	 * 
	 * @param characters the characters values
	 * @return the R character vector
	 */
	public RVector<RCharacterStore> createCharVector(final String[] characters) {
		return createVector(createCharData(characters), RObject.CLASSNAME_CHARACTER);
	}
	
	/**
	 * Creates an R character vector of the given length.
	 * <p>
	 * The vector has the default R class name 'character'.</p>
	 * <p>
	 * The function works analog to the R function <code>character(length)</code>;
	 * the vector is initialized with "" (empty String) values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R charcter vector
	 */
	public RVector<RCharacterStore> createCharVector(final int length) {
		return createVector(createCharData(length), RObject.CLASSNAME_CHARACTER);
	}
	
	/**
	 * Creates an R raw vector of the specified length.
	 * <p>
	 * The vector has the default R class name 'raw'.</p>
	 * <p>
	 * The function works analog to the R function <code>raw(length)</code>;
	 * the vector is initialized with 0.0 values.</p>
	 * 
	 * @param length the length of the vector
	 * @return the R complex vector
	 */
	public RVector<RRawStore> createRawVector(final int length) {
		return createVector(createRawData(length), RObject.CLASSNAME_RAW);
	}
	
	/**
	 * Creates an R (unordered) factor vector with level codes from a Java integer array.
	 * <p>
	 * The vector has the default R class name 'factor'.</p>
	 * <p>
	 * Note that the R vector may use the array directly.</p>
	 * 
	 * @param codes the coded levels
	 * @param levels the labels of the levels
	 * @return the R factor vector
	 */
	public RVector<RFactorStore> createFactorVector(final int[] codes, final String[] levels) {
		return createVector(createFactorData(codes, levels), RObject.CLASSNAME_FACTOR);
	}
	
	/**
	 * Creates an R (unordered) factor vector of the specified length.
	 * <p>
	 * The vector has the default R class name 'factor'.</p>
	 * 
	 * @param length the length of the vector
	 * @param levels the labels of the levels
	 * @return the R factor vector
	 */
	public RVector<RFactorStore> createFactorVector(final int length, final String[] levels) {
		return createVector(createFactorData(length, levels), RObject.CLASSNAME_FACTOR);
	}
	
	/**
	 * Creates an R ordered factor vector with level codes from a Java integer array.
	 * <p>
	 * The vector has the default R class name 'ordered'.</p>
	 * <p>
	 * Note that the R vector may use the array directly.</p>
	 * 
	 * @param codes the coded levels
	 * @param levels the labels of the levels
	 * @return the R factor vector
	 */
	public RVector<RFactorStore> createOrderedVector(final int[] codes, final String[] levels) {
		return createVector(createOrderedData(codes, levels), RObject.CLASSNAME_ORDERED);
	}
	
	/**
	 * Creates an R ordered factor vector of the specified length.
	 * <p>
	 * The vector has the default R class name 'factor'.</p>
	 * 
	 * @param length the length of the vector
	 * @param levels the labels of the levels
	 * @return the R factor vector
	 */
	public RVector<RFactorStore> createOrderedVector(final int length, final String[] levels) {
		return createVector(createOrderedData(length, levels), RObject.CLASSNAME_ORDERED);
	}
	
	
	/*-- Array/Matrix --*/
	
	public <DataType extends RStore> RArray<DataType> createArray(final DataType data, final int[] dim,
			final String classname) {
		return new RArrayImpl<DataType>(data, classname, dim);
	}
	
	public <DataType extends RStore> RArray<DataType> createArray(final DataType data, final int[] dim) {
		return createArray(data, dim, (dim.length == 2) ? RObject.CLASSNAME_MATRIX :RObject.CLASSNAME_ARRAY);
	}
	
	public <DataType extends RStore> RArray<DataType> createMatrix(final DataType data, final int dim1, final int dim2) {
		return createArray(data, new int[] { dim1, dim2 }, RObject.CLASSNAME_MATRIX);
	}
	
	
	public RArray<RLogicalStore> createLogiArray(final boolean[] logicals, final int[] dim) {
		return createArray(createLogiData(logicals), dim);
	}
	
	public RArray<RLogicalStore> createLogiArray(final int[] dim) {
		return createArray(createLogiData(getArrayLength(dim)), dim);
	}
	
	public RArray<RIntegerStore> createIntArray(final int[] integers, final int[] dim) {
		return createArray(createIntData(integers), dim);
	}
	
	public RArray<RIntegerStore> createIntArray(final int[] dim) {
		return createArray(createIntData(getArrayLength(dim)), dim);
	}
	
	public RArray<RNumericStore> createNumArray(final double[] numerics, final int[] dim) {
		return createArray(createNumData(numerics), dim);
	}
	
	public RArray<RNumericStore> createNumArray(final int[] dim) {
		return createArray(createNumData(getArrayLength(dim)), dim);
	}
	
	public RArray<RCharacterStore> createCharArray(final String[] characters, final int[] dim) {
		return createArray(createCharData(characters), dim);
	}
	
	public RArray<RCharacterStore> createCharArray(final int[] dim) {
		return createArray(createCharData(getArrayLength(dim)), dim);
	}
	
	
	public RArray<RLogicalStore> createLogiMatrix(final boolean[] logicals, final int dim1, final int dim2) {
		return createMatrix(createLogiData(logicals), dim1, dim2);
	}
	
	public RArray<RLogicalStore> createLogiMatrix(final int dim1, final int dim2) {
		return createMatrix(createLogiData(dim1*dim2), dim1, dim2);
	}
	
	public RArray<RIntegerStore> createIntMatrix(final int[] integers, final int dim1, final int dim2) {
		return createMatrix(createIntData(integers), dim1, dim2);
	}
	
	public RArray<RIntegerStore> createIntMatrix(final int dim1, final int dim2) {
		return createMatrix(createIntData(dim1*dim2), dim1, dim2);
	}
	
	public RArray<RNumericStore> createNumMatrix(final double[] numerics, final int dim1, final int dim2) {
		return createMatrix(createNumData(numerics), dim1, dim2);
	}
	
	public RArray<RNumericStore> createNumMatrix(final int dim1, final int dim2) {
		return createMatrix(createNumData(dim1*dim2), dim1, dim2);
	}
	
	public RArray<RCharacterStore> createCharMatrix(final String[] characters, final int dim1, final int dim2) {
		return createMatrix(createCharData(characters), dim1, dim2);
	}
	
	public RArray<RCharacterStore> createCharMatrix(final int dim1, final int dim2) {
		return createMatrix(createCharData(dim1*dim2), dim1, dim2);
	}
	
	
	/*-- DataFrame --*/
	
	public RDataFrame createDataFrame(final RStore[] colDatas, final String[] colNames) {
		return createDataFrame(colDatas, colNames, null);
	}
	
	public RDataFrame createDataFrame(final RStore[] colDatas, final String[] colNames, final String[] rowNames) {
		final RObject[] colVectors = new RObject[colDatas.length];
		for (int i = 0; i < colVectors.length; i++) {
			colVectors[i] = createVector(colDatas[i]);
		}
		return createDataFrame(colVectors, colNames, rowNames);
	}
	
	public RDataFrame createDataFrame(final RObject[] colVectors,
			final String[] colNames, final String[] rowNames) {
		return new RDataFrameImpl(colVectors, RObject.CLASSNAME_DATAFRAME, colNames, rowNames);
	}
	
	
	public RList createList(final RObject[] components, final String[] names, final String classname) {
		return new RListImpl(components, classname, names);
	}
	
	public RList createList(final RObject[] components, final String[] names) {
		return createList(components, names, RObject.CLASSNAME_LIST);
	}
	
	
	/*-- Language --*/
	
	public RLanguage createName(final String name) {
		return new RLanguageImpl(RLanguage.NAME, name, RObject.CLASSNAME_NAME);
	}
	
	public RLanguage createExpression(final String expr) {
		return new RLanguageImpl(RLanguage.EXPRESSION, expr, RObject.CLASSNAME_EXPRESSION);
	}
	
	
	/*-- Data/RStore --*/
	
	public RLogicalStore createLogiData(final boolean[] logiValues) {
		return new RLogicalDataByteImpl(logiValues);
	}
	
	public RLogicalStore createLogiData(final int length) {
		return new RLogicalDataByteImpl(length);
	}
	
	public RIntegerStore createIntData(final int[] intValues) {
		return new RIntegerDataImpl(intValues);
	}
	
	public RIntegerStore createIntData(final int length) {
		return new RIntegerDataImpl(length);
	}
	
	public RNumericStore createNumData(final double[] numValues) {
		return RNumericDataBImpl.isBforNASupported() ? new RNumericDataBImpl(numValues) : new RNumericDataImpl(numValues);
	}
	
	public RNumericStore createNumData(final int length) {
		return RNumericDataBImpl.isBforNASupported() ? new RNumericDataBImpl(length) : new RNumericDataImpl(length);
	}
	
	public RComplexStore createCplxData(final double[] reValues, final double[] imValues) {
		return RNumericDataBImpl.isBforNASupported() ? new RComplexDataBImpl(reValues, imValues, null) : new RComplexDataImpl(reValues, imValues, null);
	}
	
	public RComplexStore createCplxData(final int length) {
		return RNumericDataBImpl.isBforNASupported() ? new RComplexDataBImpl(length) : new RComplexDataImpl(length);
	}
	
	public RCharacterStore createCharData(final String[] charValues) {
		return new RCharacterDataImpl(charValues);
	}
	
	public RCharacterStore createCharData(final int length) {
		return new RCharacterDataImpl(length);
	}
	
	public RRawStore createRawData(final byte[] rawValues) {
		return new RRawDataImpl(rawValues);
	}
	
	public RRawStore createRawData(final int length) {
		return new RRawDataImpl(length);
	}
	
	public RFactorStore createFactorData(final int[] codes, final String[] levels) {
		return new RFactorDataImpl(codes, false, levels);
	}
	
	public RFactorStore createFactorData(final int length, final String[] levels) {
		return new RFactorDataImpl(length, false, levels);
	}
	
	public RFactorStore createOrderedData(final int[] codes, final String[] levels) {
		return new RFactorDataImpl(codes, true, levels);
	}
	
	public RFactorStore createOrderedData(final int length, final String[] levels) {
		return new RFactorDataImpl(length, true, levels);
	}
	
	
	/*-- Streaming --*/
	
	public RObject readObject(final RJIO io) throws IOException {
		final byte type = io.in.readByte();
		switch (type) {
		case -1:
			return null;
		case RObject.TYPE_NULL:
			return RNull.INSTANCE;
		case RObject.TYPE_VECTOR: {
			return new RVectorImpl(io, this); }
		case RObject.TYPE_ARRAY:
			return new RArrayImpl(io, this);
		case RObject.TYPE_LIST:
			return new RListImpl(io, this);
		case RObject.TYPE_DATAFRAME:
			return new RDataFrameImpl(io, this);
		case RObject.TYPE_ENV:
			return new REnvironmentImpl(io, this);
		case RObject.TYPE_LANGUAGE:
			return new RLanguageImpl(io, this);
		case RObject.TYPE_FUNCTION:
			return new RFunctionImpl(io, this);
		case RObject.TYPE_REFERENCE:
			return new RReferenceImpl(io, this);
		case RObject.TYPE_S4OBJECT:
			return new RS4ObjectImpl(io, this);
		case RObject.TYPE_OTHER:
			return new ROtherImpl(io, this);
		case RObject.TYPE_MISSING:
			return RMissing.INSTANCE;
		default:
			throw new IOException("object type = " + type);
		}
	}
	
	public void writeObject(final RObject robject, final RJIO io) throws IOException {
		if (robject == null) {
			io.out.writeByte(-1);
			return;
		}
		final byte type = robject.getRObjectType();
		io.out.writeByte(type);
		switch (type) {
		case RObject.TYPE_NULL:
		case RObject.TYPE_MISSING:
			return;
		case RObject.TYPE_VECTOR:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_ARRAY:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_LIST:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_DATAFRAME:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_ENV:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_LANGUAGE:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_FUNCTION:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_REFERENCE:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_S4OBJECT:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		case RObject.TYPE_OTHER:
			((ExternalizableRObject) robject).writeExternal(io, this);
			return;
		default:
			throw new IOException("object type = " + type);
		}
	}
	
	public RStore readStore(final RJIO io) throws IOException {
		if ((io.flags & F_ONLY_STRUCT) == 0) {
			final byte storeType = io.in.readByte();
			switch (storeType) {
			case RStore.LOGICAL:
				return new RLogicalDataByteImpl(io);
			case RStore.INTEGER:
				return new RIntegerDataImpl(io);
			case RStore.NUMERIC:
				return RNumericDataBImpl.isBforNASupported() ? new RNumericDataBImpl(io) : new RNumericDataImpl(io);
			case RStore.COMPLEX:
				return RComplexDataBImpl.isBforNASupported() ? new RComplexDataBImpl(io) : new RComplexDataImpl(io);
			case RStore.CHARACTER:
				return new RCharacterDataImpl(io);
			case RStore.RAW:
				return new RRawDataImpl(io);
			case RStore.FACTOR:
				return new RFactorDataImpl(io);
			default:
				throw new IOException("store type = " + storeType);
			}
		}
		else {
			final byte storeType = io.in.readByte();
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
				return new RFactorDataStruct(io.in.readBoolean(), io.in.readInt());
			default:
				throw new IOException("store type = " + storeType);
			}
		}
	}
	
	public void writeStore(final RStore data, final RJIO io) throws IOException {
		if ((io.flags & F_ONLY_STRUCT) == 0) {
			io.out.writeByte(data.getStoreType());
			((ExternalizableRStore) data).writeExternal(io);
		}
		else {
			final byte storeType = data.getStoreType();
			io.out.writeByte(storeType);
			if (storeType == RStore.FACTOR) {
				final RFactorStore factor = (RFactorStore) data;
				io.out.writeBoolean(factor.isOrdered());
				io.out.writeInt(factor.getLevelCount());
			}
		}
	}
	
	public RList readAttributeList(final RJIO io) throws IOException {
		return new RListImpl(io, this);
	}
	
	public void writeAttributeList(final RList list, final RJIO io) throws IOException {
		((ExternalizableRObject) list).writeExternal(io, this);
	}
	
	protected final int[] readDim(final ObjectInput in) throws IOException {
		final int length = in.readInt();
		final int[] dim = new int[length];
		for (int i = 0; i < length; i++) {
			dim[i] = in.readInt();
		}
		return dim;
	}
	
	protected final void writeDim(final int[] dim, final RJIO io) throws IOException {
		final int length = dim.length;
		io.out.writeInt(length);
		for (int i = 0; i < length; i++) {
			io.out.writeInt(dim[i]);
		}
	}
	
	public RStore readNames(final RJIO io) throws IOException {
		final byte type = io.in.readByte();
		if (type == RStore.CHARACTER) {
			return new RCharacterDataImpl(io);
		}
		if (type == 0) {
			return null;
		}
		throw new IOException();
	}
	
	public void writeNames(final RStore names, final RJIO io) throws IOException {
		if (names != null) {
			final byte type = names.getStoreType();
			if (type == RStore.CHARACTER) {
				io.out.writeByte(type);
				((ExternalizableRStore) names).writeExternal(io);
				return;
			}
		}
		io.out.writeByte(0);
	}
	
}
