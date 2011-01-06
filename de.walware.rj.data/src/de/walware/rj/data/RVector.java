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

package de.walware.rj.data;


/**
 * An R object is of the type {@link RObject#TYPE_VECTOR vector}, if it is an R
 * data object of an "atomic" mode without a dimension attribute (<code>dim</code>).
 * Such an R vector object is represented by an instance of this interface.
 * <p>
 * The real data is stored in a {@link RStore} accessible by {@link #getData()}.</p>
 * <p>
 * The type {@link RObject#TYPE_VECTOR vector} equates not the R command 
 * <code>is.vector(object)</code>, mainly because all attributes except the dimension 
 * are allowed (in the R function only the names attribute is allowed).  Especially
 * an R factor object is of the type {@link RObject#TYPE_VECTOR vector}, implements
 * RVector and has a data store of the type {@link RStore#FACTOR}.  Also another S3 
 * object based on a such a data object is of the type {@link RObject#TYPE_VECTOR vector}.
 * Whereas a S4 object is never directly of this type even the object simulates such
 * a data object in R. Such an object is of the type {@link RObject#TYPE_S4OBJECT S4 object},
 * and implements {@link RS4Object} with an object of the type {@link RObject#TYPE_VECTOR vector}
 * as data slot.</p>
 * <p>
 * The complementary type for objects with dimension attribute is the type
 * {@link RObject#TYPE_ARRAY array} and the interface {@link RArray}.
 * The data structure for multiple R objects is represented by the type 
 * {@link RObject#TYPE_LIST list} respectively the interface {@link RList}.</p>
 * 
 * @param <DataType> the type of the data store
 */
public interface RVector<DataType extends RStore> extends RObject {
	
	/**
	 * Returns the length of the object. The length of an {@link RObject#TYPE_VECTOR vector}
	 * is the count of all data values.
	 * 
	 * @return the length
	 */
	int getLength();
	
	/**
	 * Returns the names for the indexes of the vector. This corresponds to the values of the
	 * R attribute (<code>names</code>) respectively the R function <code>names(object)</code>.
	 * 
	 * The returned character data has the same length the vector. If the R element does not have 
	 * names, the names are invalid, or names are disabled, the method returns <code>null</code>.
	 * 
	 * @return a data store with the names of the indexes or <code>null</code>
	 * @since 0.5
	 */
	RStore getNames();
	
	
	DataType getData();
	
//	void insert(int idx);
//	void remove(int idx);
	
}
