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
 * This is the abstract one dimensional data store for an R data type.
 * 
 * Data stores are not directly R objects but are used in the data objects
 * like {@link RVector} and {@link RArray}.
 * 
 * The available data types:
 * <ul>
 *   <li>{@link RLogicalStore}</li>
 *   <li>{@link RIntegerStore}</li>
 *   <li>{@link RNumericStore}</li>
 *   <li>{@link RComplexStore}</li>
 *   <li>{@link RCharacterStore}</li>
 *   <li>{@link RRawStore}</li>
 *   <li>{@link RFactorStore}</li>
 * </ul>
 */
public interface RStore {
	
	public static final int LOGICAL =         0x00000001;
	public static final int INTEGER =         0x00000002;
	public static final int NUMERIC =         0x00000003;
	public static final int COMPLEX =         0x00000004;
	public static final int CHARACTER =       0x00000005;
	public static final int RAW =             0x00000006;
	public static final int FACTOR =          0x0000000a;
	
	
	int getStoreType();
	String getBaseVectorRClassName();
	
	int getLength();
	
	void setNA(int idx);
	
	/**
	 * @return <code>true</code>, if has any NA values, otherwise <code>false</code>
	 */
	boolean hasNA();
	
	/**
	 * @return <code>true</code>, if value at the given index, otherwise <code>false</code>
	 */
	boolean isNA(int idx);
	
	int getInt(int idx);
	void setInt(int idx, int integer);
	
	double getNum(int idx);
	void setNum(int idx, double real);
	
	double getCplxR(int idx);
	double getCplxI(int idx);
	void setCplx(int idx, double real, double imaginary);
	
	String getChar(int idx);
	void setChar(int idx, String character);
	
	byte getRaw(int idx);
	void setRaw(int idx, byte raw);
	
	boolean getLogi(int idx);
	void setLogi(int idx, boolean logical);
	
	public Object[] toArray();
	
}
