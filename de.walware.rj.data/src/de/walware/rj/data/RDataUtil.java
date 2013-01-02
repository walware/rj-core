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
	public static String getStoreAbbr(final RStore store) {
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
	public static String getStoreClass(final RStore store) {
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
	
	public static String getStoreMode(final int storeType) {
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
	
	public static String getObjectTypeName(final byte type) {
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
	
	public static int getDataIdx(final int[] dims, final int... idxs) {
		assert (dims.length > 0);
		assert (dims.length == idxs.length);
		
		int dataIdx = idxs[0];
		int step = dims[0];
		// alt: idx=0; step=1; i=0;
		for (int i = 1; i < dims.length; i++) {
			dataIdx += step*idxs[i];
			step *= dims[i];
		}
		return dataIdx;
	}
	
	public static int getDataIdx(final RStore dims, final int... idxs) {
		assert (dims.getLength() > 0);
		assert (dims.getLength() == idxs.length);
		
		int dataIdx = idxs[0];
		int step = dims.getInt(0);
		// alt: idx=0; step=1; i=0;
		for (int i = 1; i < dims.getLength(); i++) {
			dataIdx += step*idxs[i];
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
	public static int getDataIdx(final int rowCount, final int rowIdx, final int columnIdx) {
		return rowIdx + rowCount*columnIdx; 
	}
	
	public static int[] getDataIdxs(final int[] dims, final int dim, final int idx) {
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
	
	
	public static boolean isSingleString(final RObject obj) {
		final RStore data;
		return (obj != null && (data = obj.getData()) != null
				&& data.getStoreType() == RStore.CHARACTER
				&& data.getLength() == 1 );
	}
	
	
	public static RVector<?> checkRVector(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_VECTOR) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RVector<?>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static RVector<RLogicalStore> checkRLogiVector(final RObject obj) throws UnexpectedRDataException {
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
	public static RVector<RIntegerStore> checkRIntVector(final RObject obj) throws UnexpectedRDataException {
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
	public static RVector<RNumericStore> checkRNumVector(final RObject obj) throws UnexpectedRDataException {
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
	public static RVector<RCharacterStore> checkRCharVector(final RObject obj) throws UnexpectedRDataException {
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
	
	public static RArray<?> checkRArray(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_ARRAY) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RArray<?>) obj;
	}
	
	public static RArray<?> checkRArray(final RObject obj, final int dim) throws UnexpectedRDataException {
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
	public static RArray<RCharacterStore> checkRCharArray(final RObject obj, final int dim) throws UnexpectedRDataException {
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
	
	public static RList checkRList(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_LIST) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RList) obj;
	}
	
	public static RDataFrame checkRDataFrame(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_DATAFRAME) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RDataFrame) obj;
	}
	
	public static RDataFrame checkRDataFrame(final RObject obj, final int length) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_DATAFRAME) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		if (obj.getLength() != length) {
			throw new UnexpectedRDataException("Unexpected R object length: " + obj.getLength());
		}
		return (RDataFrame) obj;
	}
	
	public static RReference checkRReference(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_REFERENCE) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RReference) obj;
	}
	
	public static RLanguage checkRLanguage(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		if (obj.getRObjectType() != RObject.TYPE_LANGUAGE) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()));
		}
		return (RLanguage) obj;
	}
	
	
	public static Boolean checkSingleLogi(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.LOGICAL) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			return null;
		}
		return Boolean.valueOf(data.getLogi(0));
	}
	
	public static boolean checkSingleLogiValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.LOGICAL) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getLogi(0);
	}
	
	public static Integer checkSingleInt(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.INTEGER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			return null;
		}
		return Integer.valueOf(data.getInt(0));
	}
	
	public static int checkSingleIntValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.INTEGER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getInt(0);
	}
	
	public static Double checkSingleNum(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.NUMERIC) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			return null;
		}
		return Double.valueOf(data.getNum(0));
	}
	
	public static double checkSingleNumValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.NUMERIC) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getNum(0);
	}
	
	public static String checkSingleChar(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		return data.getChar(0);
	}
	
	public static String checkSingleCharValue(final RObject obj) throws UnexpectedRDataException {
		if (obj == null) {
			throw new UnexpectedRDataException("Missing R object.");
		}
		final RStore data = obj.getData();
		if (data == null) {
			throw new UnexpectedRDataException("Unexpected R object type: " + getObjectTypeName(obj.getRObjectType()) + " (without R data)");
		}
		if (data.getStoreType() != RStore.CHARACTER) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		if (data.getLength() != 1) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		if (data.isNA(0)) {
			throw new UnexpectedRDataException("Unexpected R data value: NA");
		}
		return data.getChar(0);
	}
	
	public static RStore checkData(final RStore data, final byte storeType) throws UnexpectedRDataException {
		if (data.getStoreType() != storeType) {
			throw new UnexpectedRDataException("Unexpected R data type: " + getStoreAbbr(data));
		}
		return data;
	}
	
	public static <T extends RStore> T checkLengthEqual(final T data, final int length) throws UnexpectedRDataException {
		if (data.getLength() != length) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		return data;
	}
	
	public static <T extends RStore> T checkLengthGreaterOrEqual(final T data, final int length) throws UnexpectedRDataException {
		if (data.getLength() < length) {
			throw new UnexpectedRDataException("Unexpected R data length: " + data.getLength());
		}
		return data;
	}
	
	
	public static void checkColCountEqual(final RArray<?> array, final int count) throws UnexpectedRDataException {
		if (array.getDim().getInt(1) != count) {
			throw new UnexpectedRDataException("Unexpected R column count: " + array.getDim().getInt(1));
		}
	}
	
	public static int checkColName(final RArray<?> array, final String name) throws UnexpectedRDataException {
		final int idx = array.getNames(1).indexOf(name);
		if (idx < 0) {
			throw new UnexpectedRDataException("Missing R data column: " + name);
		}
		return idx;
	}
	
	
	public static int binarySearch(final RVector<?> vector, final int value) {
		final RStore data = vector.getData();
		return binarySearch(data, 0, data.getLength(), value);
	}
	
	public static int binarySearch(final RStore data, final int value) {
		return binarySearch(data, 0, data.getLength(), value);
	}
	
	public static int binarySearch(final RStore data, final int fromIdx, final int toIdx, final int value) {
		if (fromIdx < 0 || fromIdx > data.getLength()
				|| toIdx < 0 || toIdx > data.getLength()) {
			throw new IndexOutOfBoundsException();
		}
		if (fromIdx > toIdx) {
			throw new IllegalArgumentException();
		}
		
		int low = fromIdx;
		int high = toIdx - 1;
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
	
	public static int binarySearch(final RStore data,
			final int[] fromIdxs, final int length, final int[] values) {
		if (fromIdxs.length > values.length) {
			throw new IllegalArgumentException();
		}
		if (length < 0 || length > data.getLength()) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < fromIdxs.length; i++) {
			if (fromIdxs[i] < 0 || fromIdxs[i] + length > data.getLength()) {
				throw new IndexOutOfBoundsException();
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
	
	public static int compare(final int[] values1, final int[] values2) {
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
