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
 * An R object is of the type {@link RObject#TYPE_LIST list}, if the object is
 * an R list but not an R data frame (see {@link #TYPE_DATAFRAME}).  Such an R
 * list object is represented by an instance of this interface.
 * <p>
 * The R function <code>typeof(object)</code> returns 'list' or 'pairlist' for
 * objects of this type.</p>
 * <p>
 * The interfaces for R objects of the type {@value RObject#TYPE_DATAFRAME} -
 * {@link RDataFrame}, {@link RObject#TYPE_S4OBJECT} - {@link RS4Object} and 
 * {@link RObject#TYPE_ENV} - {@link REnvironment} extends this interface for the
 * purpose of a uniform API.
 * Objects of this type does not necessary provide the full functionality and the
 * methods can have special meaning and conditions; see the documentation of these
 * interfaces.</p>
 * <p>
 * Indexes are zero-based (as usual in Java) and not one-base like in R.</p>
 */
public interface RList extends RObject {
	
	/**
	 * Returns the length of the object. The length of a {@link RObject#TYPE_LIST list}
	 * is the count of list items.
	 * 
	 * @return the length
	 */
	int getLength();
	
	/**
	 * Returns the names of the list items.
	 * 
	 * @return the item names
	 */
	RCharacterStore getNames();
	
	/**
	 * Returns the name of the item at the specified index.
	 * <p>
	 * This is equivalent to <code>getNames().getChar(idx)</code>.</p>
	 * 
	 * @param idx the index (zero-based) of the item
	 * @return the slot names
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; length
	 */
	String getName(int idx);
	
	/**
	 * Returns the item at the specified index.
	 * 
	 * @param idx the index (zero-based) of the item
	 * @return the item
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; length
	 */
	RObject get(int idx);
	
	/**
	 * Returns the item with the specified name. If multiple items have that name,
	 * the first item with the given name is picked.
	 * 
	 * @param name the name of the item
	 * @return the item or <code>null</code> (if no item with the specified name exists)
	 */
	RObject get(String name);
	
	/**
	 * Returns an array with the R object items of the list.
	 * <p>
	 * The array is newly created for each call of this method.</p>
	 * 
	 * @return an array with the items of the list
	 */
	RObject[] toArray();
	
//	void insert(int idx, String name, RObject component);
//	void add(String name, RObject component);
//	boolean set(int idx, RObject component);
//	boolean set(String name, RObject component);
//	void remove(int idx);
	
}
