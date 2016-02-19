/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;


public class RDataUtil {
	
	/**
	 * Returns the common abbreviation for the type of the given data store.
	 * These are the abbreviations used by the R function <code>str(x)</code>.
	 * If there is no abbreviation for the given type, it return the class name 
	 * of an R vector with data of this type.
	 * 
	 * @param store the data store
	 * @return an abbreviation
	 */
	public static final String getStoreAbbr(final RStore<?> store) {
		switch (store.getStoreType()) {
		case RStore.LOGICAL:
			return "logi";
		case RStore.INTEGER:
			return "int";
		case RStore.NUMERIC:
			return "num";
		case RStore.CHARACTER:
			return "chr";
		case RStore.COMPLEX:
			return "cplx";
		case RStore.RAW:
			return "raw";
		case RStore.FACTOR:
			return ((RFactorStore) store).isOrdered() ? RObject.CLASSNAME_ORDERED : RObject.CLASSNAME_FACTOR;
		default:
			return "";
		}
	}
	
	/**
	 * Returns the class name of an R vector with data of the type of the given data store.
	 * 
	 * @param store the data store
	 * @return an class name
	 */
	public static final String getStoreClass(final RStore<?> store) {
		switch (store.getStoreType()) {
		case RStore.LOGICAL:
			return RObject.CLASSNAME_LOGICAL;
		case RStore.INTEGER:
			return RObject.CLASSNAME_INTEGER;
		case RStore.NUMERIC:
			return RObject.CLASSNAME_NUMERIC;
		case RStore.CHARACTER:
			return RObject.CLASSNAME_CHARACTER;
		case RStore.COMPLEX:
			return RObject.CLASSNAME_COMPLEX;
		case RStore.RAW:
			return RObject.CLASSNAME_RAW;
		case RStore.FACTOR:
			return ((RFactorStore) store).isOrdered() ? RObject.CLASSNAME_ORDERED : RObject.CLASSNAME_FACTOR;
		default:
			return "";
		}
	}
	
	public static final String getStoreMode(final int storeType) {
		switch (storeType) {
		case RStore.LOGICAL:
			return "logical";
		case RStore.INTEGER:
		case RStore.FACTOR:
			return "integer";
		case RStore.NUMERIC:
			return "numeric";
		case RStore.CHARACTER:
			return "character";
		case RStore.COMPLEX:
			return "complex";
		case RStore.RAW:
			return "raw";
		default:
			return "";
		}
	}
	
	public static final String getObjectTypeName(final byte type) {
		switch (type) {
		case RObject.TYPE_NULL:
			return "RNull";
		case RObject.TYPE_VECTOR:
			return "RVector";
		case RObject.TYPE_ARRAY:
			return "RArray";
		case RObject.TYPE_LIST:
			return "RList";
		case RObject.TYPE_DATAFRAME:
			return "RDataFrame";
		case RObject.TYPE_S4OBJECT:
			return "RS4Object";
		case RObject.TYPE_ENV:
			return "REnvironment";
		case RObject.TYPE_FUNCTION:
			return "RFunction";
		case RObject.TYPE_REFERENCE:
			return "RReference";
		case RObject.TYPE_OTHER:
			return "<other>";
		case RObject.TYPE_MISSING:
			return "<missing>";
		case RObject.TYPE_PROMISE:
			return "<promise>";
		default:
			return "<unkown>";
		}
	}
	
	public static final long computeLengthFromDim(final int[] dims) {
		if (dims.length == 0) {
			return 0;
		}
		long length = 1;
		for (int i = 0; i < dims.length; i++) {
			length *= dims[i];
		}
		return length;
	}
	
	public static final long getDataIdx(final int[] dims, final int... idxs) {
		assert (dims.length > 0);
		assert (dims.length == idxs.length);
		
		long dataIdx = idxs[0];
		long step = dims[0];
		// alt: idx=0; step=1; i=0;
		for (int i = 1; i < dims.length; i++) {
			dataIdx += step*idxs[i];
			step *= dims[i];
		}
		return dataIdx;
	}
	
	public static final long getDataIdx(final RStore<?> dims, final int... idxs) {
		assert (dims.getLength() > 0);
		assert (dims.getLength() == idxs.length);
		
		long dataIdx = idxs[0];
		long step = dims.getInt(0);
		// alt: idx=0; step=1; i=0;
		for (int i = 1; i < dims.getLength(); i++) {
			dataIdx += step * idxs[i];
			step *= dims.getInt(i);
		}
		return dataIdx;
	}
	
	/**
	 * Computes the index of a data value in a store of a matrix / 2 dimensional arrays
	 * specified by its column and row indexes.
	 * 
	 * @param rowCount count of rows of the matrix
	 * @param rowIdx row index (zero-based) of the matrix
	 * @param columnIdx column index (zero-based) of the matrix
	 * @return the index in the data store
	 * 
	 * @see #getDataIdx(int[], int...)
	 */
	public static final long getDataIdx(final long rowCount, final long rowIdx, final long columnIdx) {
		return rowIdx + rowCount * columnIdx;
	}
	
	public static final int[] getDataIdxs(final int[] dims, final int dim, final int idx) {
		assert (dims.length > 0);
		assert (0 <= dim && dim < dims.length);
		assert (0 <= idx && idx < dims[dim]);
		
		final int size;
		final int dimsCount = dims.length;
		if (dimsCount == 1) {
			size = 1;
		}
		else if (dimsCount == 2) {
			size = dims[(dim == 0) ? 1 : 0];
		}
		else {
			int counter = 1;
			for (int dimIdx = 0; dimIdx < dimsCount; dimIdx++) {
				if (dimIdx != dim) {
					counter *= dims[dimIdx];
				}
			}
			size = counter;
		}
		
		final int[] dataIdxs = new int[size];
		int step = 1;
		int stepCount = 1;
		for (int dimIdx = 0; dimIdx < dimsCount; dimIdx++) {
			final int dimSize = dims[dimIdx];
			if (dimIdx == dim) {
				final int add = step*idx;
				for (int i = 0; i < size; i++) {
					dataIdxs[i] += add;
				}
			}
			else {
				for (int i = 0, idxInDim = 0; i < size; idxInDim++) {
					if (idxInDim == dimSize) {
						idxInDim = 0;
					}
					final int add = step*idxInDim;
					for (int j = 0; j < stepCount && i < size; i++,j++) {
						dataIdxs[i] += add;
					}
				}
				stepCount *= dimSize;
			}
			step *= dimSize;
		}
		return dataIdxs;
	}
	
	
	public static final boolean isSingleString(final RObject obj) {
		final RStore<?> data;
		return (obj != null && (data = obj.getData()) != null
				&& data.getStoreType() == RStore.CHARACTER
				&& data.getLength() == 1 );
	}
	
	
	public static final RVector<?> checkRVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RVector<?>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static final RVector<RLogicalStore> checkRLogiVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getData().getStoreType() != RStore.LOGICAL) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getStoreAbbr(obj.getData()));
		}
		return (RVector<RLogicalStore>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static final RVector<RIntegerStore> checkRIntVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getData().getStoreType() != RStore.INTEGER) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getStoreAbbr(obj.getData()));
		}
		return (RVector<RIntegerStore>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static final RVector<RNumericStore> checkRNumVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getData().getStoreType() != RStore.NUMERIC) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getStoreAbbr(obj.getData()));
		}
		return (RVector<RNumericStore>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static final RVector<RCharacterStore> checkRCharVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getData().getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getStoreAbbr(obj.getData()));
		}
		return (RVector<RCharacterStore>) obj;
	}
	
	public static final RArray<?> checkRArray(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_ARRAY) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RArray<?>) obj;
	}
	
	public static final RArray<?> checkRArray(final RObject obj, final int dim) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_ARRAY) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		final RArray<?> array = (RArray<?>) obj;
		if (dim > 0) {
			if (dim != array.getDim().getLength()) {
				throw new UnexpectedRDataException("Unexpected R array dimension: " + array.getDim().getLength());
			}
		}
		return array;
	}
	
	public static final RArray<?> checkRArray(final RObject obj, final long dim) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_ARRAY) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		final RArray<?> array = (RArray<?>) obj;
		if (dim > 0) {
			if (dim != array.getDim().getLength()) {
				throw new UnexpectedRDataException("Unexpected R array dimension: " + array.getDim().getLength());
			}
		}
		return array;
	}
	
	@SuppressWarnings("unchecked")
	public static final RArray<RCharacterStore> checkRCharArray(final RObject obj, final int dim) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_ARRAY) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getData().getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(obj.getData()));
		}
		final RArray<RCharacterStore> array = (RArray<RCharacterStore>) obj;
		if (dim > 0) {
			if (dim != array.getDim().getLength()) {
				throw new UnexpectedRDataException("Unexpected R array dimension: " + array.getDim().getLength());
			}
		}
		return array;
	}
	
	public static final RList checkRList(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_LIST) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RList) obj;
	}
	
	public static final RDataFrame checkRDataFrame(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_DATAFRAME) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RDataFrame) obj;
	}
	
	public static final RDataFrame checkRDataFrame(final RObject obj, final long length) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_DATAFRAME) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getLength() != length) {
			throw new UnexpectedRDataException("Unexpected R dataframe column count: " + obj.getLength());
		}
		return (RDataFrame) obj;
	}
	
	public static final RReference checkRReference(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_REFERENCE) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RReference) obj;
	}
	
	public static final RLanguage checkRLanguage(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_LANGUAGE) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RLanguage) obj;
	}
	
	public static final Boolean checkSingleLogi(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.LOGICAL) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			return null;
		}
		return Boolean.valueOf(data.getLogi(0));
	}
	
	public static final boolean checkSingleLogiValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.LOGICAL) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getLogi(0);
	}
	
	public static final Integer checkSingleInt(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.INTEGER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			return null;
		}
		return Integer.valueOf(data.getInt(0));
	}
	
	public static final int checkSingleIntValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.INTEGER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getInt(0);
	}
	
	public static final Double checkSingleNum(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.NUMERIC) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			return null;
		}
		return Double.valueOf(data.getNum(0));
	}
	
	public static final double checkSingleNumValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.NUMERIC) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getNum(0);
	}
	
	public static final String checkSingleChar(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		return data.getChar(0);
	}
	
	public static final String checkSingleCharValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore<?> data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == 1 expected.");
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getChar(0);
	}
	
	public static final RList checkType(final RObject obj, final byte objectType) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != objectType) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + ", but " + objectType + " expected.");
		}
		return (RList) obj;
	}
	
	public static final RStore<?> checkData(final RStore<?> data, final byte storeType) throws UnexpectedRDataException {
		if (data.getStoreType() != storeType) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data) + ", but " + storeType + " expected.");
		}
		return data;
	}
	
	
	public static final int checkIntLength(final RObject obj) throws UnexpectedRDataException {
		final long length = obj.getLength();
		if (length < 0 || length > Integer.MAX_VALUE) {
			throw new UnexpectedRDataException("Unexpected R object length: " + length + ", but <= 2^31-1 expected.");
		}
		return (int) length;
	}
	
	public static final int checkIntLength(final RStore<?> data) throws UnexpectedRDataException {
		final long length = data.getLength();
		if (length < 0 || length > Integer.MAX_VALUE) {
			throw new UnexpectedRDataException("Unexpected R data length: " + length + ", but <= 2^31-1 expected.");
		}
		return (int) length;
	}
	
	public static final <T extends RObject> T checkLengthEqual(final T obj, final long length) throws UnexpectedRDataException {
		if (obj.getLength() != length) {
			throw new UnexpectedRDataException("Unexpected R object length: " + obj.getLength() + ", but == " + length + " expected.");
		}
		return obj;
	}
	
	public static final <T extends RStore<?>> T checkLengthEqual(final T data, final long length) throws UnexpectedRDataException {
		if (data.getLength() != length) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but == " + length + " expected.");
		}
		return data;
	}
	
	public static final <T extends RStore<?>> T checkLengthGreaterOrEqual(final T data, final long length) throws UnexpectedRDataException {
		if (data.getLength() < length) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength() + ", but >= " + length + " expected.");
		}
		return data;
	}
	
	public static final <P> P checkValue(final RStore<P> data, final int idx) throws UnexpectedRDataException {
		final P value= data.get(idx);
		if (value == null) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return value;
	}
	
	public static final <P> P checkValue(final RStore<P> data, final long idx) throws UnexpectedRDataException {
		final P value= data.get(idx);
		if (value == null) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return value;
	}
	
	public static final <P> P getValue(final RStore<P> data, final int idx, final P na) {
		final P value= data.get(idx);
		return (value != null) ? value : na;
	}
	
	public static final <P> P getValue(final RStore<P> data, final long idx, final P na) {
		final P value= data.get(idx);
		return (value != null) ? value : na;
	}
	
	
	/*-- 2d --*/
	
	public static final int getRowCount(final RArray<?> array) throws UnexpectedRDataException {
		return array.getDim().getInt(0);
	}
	
	public static final void checkRowCountEqual(final RArray<?> array, final int count) throws UnexpectedRDataException {
		if (array.getDim().getInt(0) != count) {
			throw new UnexpectedRDataException("Unexpected R matrix row count: " + array.getDim().getInt(0) + ", but == " + count + " expected.");
		}
	}
	
	public static final void checkRowCountEqual(final RDataFrame dataframe, final long count) throws UnexpectedRDataException {
		if (dataframe.getRowCount() != count) {
			throw new UnexpectedRDataException("Unexpected R dataframe row count: " + dataframe.getRowCount() + ", but == " + count + " expected.");
		}
	}
	
	public static final int getColumnCount(final RArray<?> array) throws UnexpectedRDataException {
		return array.getDim().getInt(1);
	}
	
	public static final void checkColumnCountEqual(final RArray<?> array, final int count) throws UnexpectedRDataException {
		if (array.getDim().getInt(1) != count) {
			throw new UnexpectedRDataException("Unexpected R matrix column count: " + array.getDim().getInt(1) + ", but == " + count + " expected.");
		}
	}
	
	public static final long checkColumnName(final RArray<?> array, final String name) throws UnexpectedRDataException {
		final long idx = array.getNames(1).indexOf(name);
		if (idx < 0) {
			throw new UnexpectedRDataException("Missing R matrix column: " + name);
		}
		return idx;
	}
	
	public static final void checkColumnCountEqual(final RDataFrame dataframe, final long count) throws UnexpectedRDataException {
		if (dataframe.getColumnCount() != count) {
			throw new UnexpectedRDataException("Unexpected R dataframe column count: " + dataframe.getColumnCount() + ", but == " + count + " expected.");
		}
	}
	
	
	/*-- int --*/
	
	public static final long binarySearch(final RVector<?> vector, final int value) {
		final RStore<?> data = vector.getData();
		return binarySearch(data, 0, data.getLength(), value);
	}
	
	public static final long binarySearch(final RStore<?> data, final int value) {
		return binarySearch(data, 0, data.getLength(), value);
	}
	
	public static final int binarySearch(final RStore<?> data, final int fromIdx, final int toIdx, final int value) {
		if (fromIdx < 0 || fromIdx > data.getLength()
				|| toIdx < 0 || toIdx > data.getLength()) {
			throw new IndexOutOfBoundsException(Long.toString(fromIdx));
		}
		if (fromIdx > toIdx) {
			throw new IllegalArgumentException();
		}
		
		return doBinarySearch(data, fromIdx, toIdx - 1, value);
	}
	
	private static int doBinarySearch(final RStore<?> data, int low, int high, final int value) {
		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final int midValue = data.getInt(mid);
			if (midValue < value) {
				low = mid + 1;
			}
			else if (midValue > value) {
				high = mid - 1;
			}
			else {
				return mid; // key found
			}
		}
		return -(low + 1);  // key not found.
	}
	
	public static final long binarySearch(final RStore<?> data, final long fromIdx, final long toIdx, final int value) {
		if (fromIdx < 0 || fromIdx > data.getLength()
				|| toIdx < 0 || toIdx > data.getLength()) {
			throw new IndexOutOfBoundsException(Long.toString(fromIdx));
		}
		if (fromIdx > toIdx) {
			throw new IllegalArgumentException();
		}
		if (toIdx <= Integer.MAX_VALUE) {
			return doBinarySearch(data, (int) fromIdx, (int) toIdx - 1, value);
		}
		
		return doBinarySearch(data, fromIdx, toIdx - 1, value);
	}
	
	private static long doBinarySearch(final RStore<?> data, long low, long high, final int value) {
		while (low <= high) {
			final long mid = (low + high) >>> 1;
			final int midValue = data.getInt(mid);
			if (midValue < value) {
				low = mid + 1;
			}
			else if (midValue > value) {
				high = mid - 1;
			}
			else {
				return mid; // key found
			}
		}
		return -(low + 1);  // key not found.
	}
	
	public static final int binarySearch(final RStore<?> data,
			final long[] fromIdxs, final int length, final int[] values) {
		if (fromIdxs.length > values.length) {
			throw new IllegalArgumentException();
		}
		if (length < 0 || length > data.getLength()) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < fromIdxs.length; i++) {
			if (fromIdxs[i] < 0 || fromIdxs[i] + length > data.getLength()) {
				throw new IndexOutOfBoundsException(Long.toString(fromIdxs[i]));
			}
		}
		
		int low = 0;
		int high = length - 1;
		ITER_IDX: while (low <= high) {
			final int mid = (low + high) >>> 1;
			for (int i = 0; i < fromIdxs.length; i++) {
				final int midValue = data.getInt(fromIdxs[i] + mid);
				if (midValue < values[i]) {
					low = mid + 1;
					continue ITER_IDX;
				}
				if (midValue > values[i]) {
					high = mid - 1;
					continue ITER_IDX;
				}
			}
			return mid; // key found
		}
		return -(low + 1);  // key not found.
	}
	
	public static final int compare(final int[] values1, final int[] values2) {
		for (int i = 0; i < values1.length; i++) {
			if (values1[i] < values2[i]) {
				return -1;
			}
			if (values1[i] > values2[i]) {
				return +1;
			}
		}
		return 0; // equal
	}
	
	
	/*-- long ~ num -- */
	
	public static final long binarySearch(final RStore<?> data, final long value) {
		return binarySearch(data, 0, data.getLength(), value);
	}
	
	public static final int binarySearch(final RStore<?> data, final int fromIdx, final int toIdx, final long value) {
		if (fromIdx < 0 || fromIdx > data.getLength()
				|| toIdx < 0 || toIdx > data.getLength()) {
			throw new IndexOutOfBoundsException(Long.toString(fromIdx));
		}
		if (fromIdx > toIdx) {
			throw new IllegalArgumentException();
		}
		
		return doBinarySearch(data, fromIdx, toIdx - 1, value);
	}
	
	private static int doBinarySearch(final RStore<?> data, int low, int high, final long value) {
		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final double midValue = data.getNum(mid);
			if (midValue < value) {
				low = mid + 1;
			}
			else if (midValue > value) {
				high = mid - 1;
			}
			else {
				return mid; // key found
			}
		}
		return -(low + 1);  // key not found.
	}
	
	public static final long binarySearch(final RStore<?> data, final long fromIdx, final long toIdx, final long value) {
		if (fromIdx < 0 || fromIdx > data.getLength()
				|| toIdx < 0 || toIdx > data.getLength()) {
			throw new IndexOutOfBoundsException(Long.toString(fromIdx));
		}
		if (fromIdx > toIdx) {
			throw new IllegalArgumentException();
		}
		if (toIdx <= Integer.MAX_VALUE) {
			return doBinarySearch(data, (int) fromIdx, (int) toIdx - 1, value);
		}
		
		return doBinarySearch(data, fromIdx, toIdx - 1, value);
	}
	
	private static long doBinarySearch(final RStore<?> data, long low, long high, final long value) {
		while (low <= high) {
			final long mid = (low + high) >>> 1;
		final double midValue = data.getNum(mid);
		if (midValue < value) {
			low = mid + 1;
		}
		else if (midValue > value) {
			high = mid - 1;
		}
		else {
			return mid; // key found
		}
		}
		return -(low + 1);  // key not found.
	}
	
	public static final int binarySearch(final RStore<?> data,
			final long[] fromIdxs, final int length, final long[] values) {
		if (fromIdxs.length > values.length) {
			throw new IllegalArgumentException();
		}
		if (length < 0 || length > data.getLength()) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < fromIdxs.length; i++) {
			if (fromIdxs[i] < 0 || fromIdxs[i] + length > data.getLength()) {
				throw new IndexOutOfBoundsException(Long.toString(fromIdxs[i]));
			}
		}
		
		int low = 0;
		int high = length - 1;
		ITER_IDX: while (low <= high) {
			final int mid = (low + high) >>> 1;
			for (int i = 0; i < fromIdxs.length; i++) {
				final double midValue = data.getNum(fromIdxs[i] + mid);
				if (midValue < values[i]) {
					low = mid + 1;
					continue ITER_IDX;
				}
				if (midValue > values[i]) {
					high = mid - 1;
					continue ITER_IDX;
				}
			}
			return mid; // key found
		}
		return -(low + 1);  // key not found.
	}
	
	public static final int compare(final long[] values1, final long[] values2) {
		for (int i = 0; i < values1.length; i++) {
			if (values1[i] < values2[i]) {
				return -1;
			}
			if (values1[i] > values2[i]) {
				return +1;
			}
		}
		return 0; // equal
	}
	
	
	public static final byte[] encodeLongToRaw(final long id) {
		final byte[] raw = new byte[8];
		raw[0] = (byte) (id >>> 56);
		raw[1] = (byte) (id >>> 48);
		raw[2] = (byte) (id >>> 40);
		raw[3] = (byte) (id >>> 32);
		raw[4] = (byte) (id >>> 24);
		raw[5] = (byte) (id >>> 16);
		raw[6] = (byte) (id >>> 8);
		raw[7] = (byte) (id);
		return raw;
	}
	
	public static final long decodeLongFromRaw(final byte[] raw) {
		return(	((long) (raw[0] & 0xff) << 56) |
				((long) (raw[1] & 0xff) << 48) |
				((long) (raw[2] & 0xff) << 40) |
				((long) (raw[3] & 0xff) << 32) |
				((long) (raw[4] & 0xff) << 24) |
				((raw[5] & 0xff) << 16) |
				((raw[6] & 0xff) << 8) |
				((raw[7] & 0xff)) );
	}
	
}
