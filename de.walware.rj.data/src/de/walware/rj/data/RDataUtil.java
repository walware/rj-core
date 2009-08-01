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
	
	public static int getDataIdx(final int[] dims, final int[] idxs) {
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
	
}
