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


/**
 * An R object is of the type {@link RObject#TYPE_ARRAY array}, if it is an R
 * data object of an "atomic" mode with a dimension attribute (<code>dim</code>).
 * Such an R array object is represented by an instance of this interface.
 * <p>
 * The real data is stored in a {@link RStore} accessible by {@link #getData()}.
 * An index for the one-dimensional data store can be computed from an
 * index tuple relative to the dimension of the object using the methods
 * {@link RDataUtil#getDataIdx(int[], int[])}.</p>
 * <p>
 * Also an S3 object based on a such a data object is of the type {@link RObject#TYPE_ARRAY array}.
 * Whereas a S4 object is never directly of this type even the object simulates an
 * array in R. Such an object is of the type {@link RObject#TYPE_S4OBJECT S4 object},
 * and implements {@link RS4Object} with an object of the type {@link RObject#TYPE_ARRAY array}
 * as data slot.</p>
 * <p>
 * The complementary type for objects without dimension attribute is the type
 * {@link RObject#TYPE_VECTOR vector} and the interface {@link RVector}.</p>
 * 
 * @see RDataUtil#getDataIdx(int[], int[])
 * @see RDataUtil#getDataIdxs(int[], int, int)
 * @param <DataType> the type of the data store
 */
public interface RArray<DataType extends RStore> extends RObject {
	
	/**
	 * Returns the length of the object. The length of an {@link RObject#TYPE_ARRAY array}
	 * is the count of all data values, the product of its dimensions.
	 * 
	 * @return the length
	 */
	long getLength();
	
	/**
	 * Returns the dimension of this array. This corresponds to the R dimension
	 * attribute (<code>dim</dim>) respectively the R <code>dim</code> function.
	 * 
	 * @return the dimension of this array
	 */
	RIntegerStore getDim();
	
	/**
	 * Returns the names for the dimensions of the array. This corresponds to 
	 * the names of the R attribute <code>dimnames</code> respectively of the R function 
	 * <code>dimnames(object)</code>. That means it is equivalent to the R command
	 * <code>names(dimnames(object))</code>. The names for the indexes in each dimension,
	 * the values of the R attribute <code>dimnames</code>, are accessible by {@link #getNames(int)}.
	 * 
	 * The returned character data has the same length as the dimension data {@link #getDim()}. 
	 * If the R element does not have names, the names are invalid, or names are disabled, the 
	 * method returns <code>null</code>.
	 * 
	 * @return a charater data store with the names of the dimensions or <code>null</code>
	 * @since 0.5
	 */
	RCharacterStore getDimNames();
	
	/**
	 * Returns the names for the indexes in the given dimension of the array. This corresponds to 
	 * the values of the R attribute <code>dimnames</code> respectively of the R function 
	 * <code>dimnames(object)</code>. The names for the dimensions itself are accessible by
	 * {@link #getDimNames()}.
	 * 
	 * @param dim the dimension index
	 * @return a data store with the names of the indexes in the given dimensions or <code>null</code>
	 * @throws IndexOutOfBoundsException if dim &lt; 0 or dim &ge; getDim().getLength()
	 * @since 0.5
	 */
	RStore getNames(int dim);
	
	
	DataType getData();
	
//	void setDim(int[] dim);
//	void setDimNames(RList list);
//	void insert(int dim, int idx);
//	void remove(int dim, int idx);
	
}
