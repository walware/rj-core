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


/**
 * Data structure with dimension property in R
 * 
 * An object is an arrays if it is a R data type objects with a valid
 * dimension attribute.
 * 
 * @see RDataUtil#getDataIdx(int[], int[])
 * @see RDataUtil#getDataIdxs(int[], int, int)
 * @param <DataType> the type of the data store
 */
public interface RArray<DataType extends RStore> extends RObject {
	
	/**
	 * @return the dimension of this object (at no time <code>null</code>)
	 */
	int[] getDim();
	
	/**
	 * @return the names for the dimensions and its indices (may be <code>null</code>)
	 */
	RList getDimNames();
	
	DataType getData();
	
	void setDim(int[] dim);
	void setDimNames(RList list);
	void insert(int dim, int idx);
	void remove(int dim, int idx);
	
}
