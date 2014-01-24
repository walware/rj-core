/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;


/**
 * An R object is of the type {@link RObject#TYPE_DATAFRAME vector}, if it is an R list object 
 * inheriting the R class {@link #CLASSNAME_DATAFRAME data.frame} and compiling with the R rules for 
 * a data frame, especially its children are {@link RVector}s of the same length.
 * <p>
 * The methods {@link #getLength()}, {@link #getName(int)}, {@link #get(int)} and
 * {@link #get(String)}, inherited by the RList interface, provides access to the columns using the
 * uniform RList API. The methods {@link #getColumnCount()}, {@link #getColumnNames()},
 * {@link #getColumn(int)} and {@link #getColumn(String)} are data frame specific methods with
 * similar functionality with direct access to the data stores of the columns, bypassing the RVector
 * objects.</p>
 * <p>
 * The row information of the data frame are accessible by {@link #getRowCount()} and 
 * {@link #getRowNames()}.</p>
 */
public interface RDataFrame extends RList, RObject {
	
	/**
	 * Returns the number of columns of the data frame.
	 * <p>
	 * This method is synonymous to {@link #getColumnCount()}.</p>
	 * 
	 * @return the number of columns
	 */
	@Override
	long getLength();
	
	/**
	 * Returns the names of the columns of the data frame.
	 * <p>
	 * This method is synonymous to {@link #getColumnNames()}.</p>
	 * 
	 * @return the column names
	 */
	@Override
	RCharacterStore getNames();
	
	/**
	 * Returns the name of the specified column of the data frame.
	 * 
	 * @param idx the index (zero-based) of the column
	 * @return the column name
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; column count
	 */
	@Override
	String getName(long idx);
	
	/**
	 * Returns the RVector object of the specified column of the data frame.
	 * 
	 * @param idx the index (zero-based) of the column
	 * @return the column vector
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; column count
	 */
	@Override
	RObject get(long idx);
	
	/**
	 * Returns the RVector object of the column with the specified name of the data frame.
	 * 
	 * @param name the name of the column
	 * @return the column vector or <code>null</code> (if no column with the specified name exists)
	 */
	@Override
	RObject get(String name);
	
	/**
	 * For a data frame this method always returns <code>null</code>.
	 * 
	 * @return <code>null</code>
	 */
	@Override
	RStore getData();
	
	
	/**
	 * Returns the number of columns of the data frame.
	 * <p>
	 * This method is synonymous to {@link #getLength()}.</p>
	 * 
	 * @return the number of columns
	 */
	long getColumnCount();
	
	/**
	 * Returns the names of the columns of the data frame.
	 * <p>
	 * This method is synonymous to {@link #getNames()}.</p>
	 * 
	 * @return the column names
	 */
	public RCharacterStore getColumnNames();
	
//	String getColumnName(int idx);
	
	/**
	 * Returns the data store for the specified column of the data frame.
	 * <p>
	 * This method is equivalent to <code>get(idx).getData()</code>.</p>
	 * <p>
	 * Each column data store has the length of {@link #getRowCount()}.</p>
	 * 
	 * @param idx the index (zero-based) of the column
	 * @return the data store of the column
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; column count
	 */
	public RStore getColumn(int idx);
	
	/**
	 * Returns the data store for the specified column of the data frame.
	 * <p>
	 * This method is equivalent to <code>get(idx).getData()</code>.</p>
	 * <p>
	 * Each column data store has the length of {@link #getRowCount()}.</p>
	 * 
	 * @param idx the index (zero-based) of the column
	 * @return the data store of the column
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; column count
	 */
	public RStore getColumn(long idx);
	
	/**
	 * Returns the data store for the column with the specified name of the data frame.
	 * <p>
	 * This method is equivalent to <code>get(name).getData()</code> (with additional <code>null</code>
	 * check, if no column with that name exists).</p>
	 * <p>
	 * Each column data store has the length of {@link #getRowCount()}.</p>
	 * 
	 * @param name the name of the column
	 * @return the data store of the column or <code>null</code> (if no column with the specified name exists)
	 */
	public RStore getColumn(String name);
	
	/**
	 * Returns the number of rows of the data frame.
	 * <p>
	 * Each data store of a column ({@link #getColumn(int)} has this length.
	 * 
	 * @return the number of rows
	 */
	long getRowCount();
	
	/**
	 * Returns the names of the rows of the data frame.
	 * <p>
	 * This method is equivalent to the R command <code>row.names(obj)</code>.</p>
	 * 
	 * @return the row names
	 */
	RStore getRowNames();
	
	
//	void setColumn(int idx, RStore column);
//	void insertColumn(int idx, RStore column);
//	void removeColumn(int idx);
	
//	void insertRow(int idx);
//	void removeRow(int idx);
	
}
