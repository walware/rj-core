/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;

import javax.naming.OperationNotSupportedException;


/**
 * This is the abstract one-dimensional data store for an R data type.
 * <p>
 * Data stores are not directly R objects but are used in the data objects
 * {@link RVector} and {@link RArray}.</p>
 * <p>
 * There are store types for each atomic type of R (namely the types/modes
 * numeric, complex, logical, character and raw) and a special
 * type for factors.  To get the type of a RStore the method {@link #getStoreType()}
 * should be used.  The available data types are:</p>
 * <ul>
 *   <li>{@link #LOGICAL}</li>
 *   <li>{@link #INTEGER}</li>
 *   <li>{@link #NUMERIC}</li>
 *   <li>{@link #COMPLEX}</li>
 *   <li>{@link #CHARACTER}</li>
 *   <li>{@link #RAW}</li>
 *   <li>{@link #FACTOR}</li>
 * </ul>
 * <p>
 * The Java value can be accessed by the getter methods for the different
 * data types.  The indexes are zero-based like in Java and not one-base like in R.
 * Because Java doesn't provide NA for primitive data types directly,
 * the data store provides the methods {@link #isNA(int)} and {@link #setNA(int)}
 * to detect and set such values.
 * If the value at an index of the store is NA, the return value of the other getter
 * methods is undefined.</p>
 * <p>
 * Indexes are zero-based (as usual in Java) and not one-base like in R.</p>
 */
public interface RStore<P> {
	
	/**
	 * Constant indicating a store for R data type <code>logical<code>.
	 * 
	 * <p>The story object is an instance of {@link RLogicalStore}.
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_LOGICAL logical}</code>.</p>
	 * 
	 * @see #getLogi(int)
	 * @see #setLogi(int, boolean)
	 */
	byte LOGICAL =         0x00000001;
	
	/**
	 * Constant indicating a store for R data type <code>integer<code>.
	 * 
	 * <p>The story object is an instance of {@link RIntegerStore}.
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_INTEGER integer}</code>.</p>
	 * 
	 * @see #getInt(int)
	 * @see #setInt(int, int)
	 */
	byte INTEGER =         0x00000002;
	
	/**
	 * Constant indicating a store for R data type <code>logical<code>.
	 * 
	 * <p>The story object is an instance of {@link RNumericStore}.
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_NUMERIC numeric}</code>.</p>
	 * 
	 * @see #getNum(int)
	 * @see #setNum(int, double)
	 */
	byte NUMERIC =         0x00000003;
	
	/**
	 * Constant indicating a store for R data type <code>complex<code>.
	 * 
	 * <p>The story object is an instance of {@link RComplexStore}.
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_COMPLEX complex}</code>.</p>
	 * 
	 * @see #getCplxRe(int)
	 * @see #getCplxIm(int)
	 * @see #setCplx(int, double, double)
	 */
	byte COMPLEX =         0x00000004;
	
	/**
	 * Constant indicating a store for R data type <code>character<code>.
	 * 
	 * <p>The story object is an instance of {@link RCharacterStore}.  
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_CHARACTER character}</code>.</p>
	 * 
	 * @see #getChar(int)
	 * @see #setChar(int, String)
	 */
	byte CHARACTER =       0x00000005;
	
	/**
	 * Constant indicating a store for R data type <code>raw<code>.
	 * 
	 * <p>Note that raw data doesn't support NAs.</p>
	 * 
	 * <p>The story object is an instance of {@link RRawStore}.  
	 * The {@link #getBaseVectorRClassName() class name} is
	 * <code>{@link RObject#CLASSNAME_RAW raw}</code>.</p>
	 * 
	 * @see #getRaw(int)
	 * @see #setRaw(int, byte)
	 */
	byte RAW =             0x00000006;
	
	/**
	 * Constant indicating a special store extending the integer
	 * data type for R objects extending the R class <code>factor<code>.
	 * 
	 * <p>The story object is an instance of {@link RFactorStore}.
	 * The {@link #getBaseVectorRClassName() class name} is 
	 * <code>{@link RObject#CLASSNAME_FACTOR factor}</code> or 
	 * <code>{@link RObject#CLASSNAME_ORDERED ordered}</code>.
	 * 
	 * @see RFactorStore#getInt(int)
	 * @see RFactorStore#getChar(int)
	 * @see RFactorStore#setInt(int, int)
	 * @see RFactorStore#setChar(int, String)
	 **/
	byte FACTOR =          0x0000000a;
	
	
	/**
	 * Returns the constant indicating the type of this RStore.
	 * 
	 * @return one of the constant defined in the {@link RStore} interface
	 */
	byte getStoreType();
	
	/**
	 * Returns the R class name a vector with this data store and no other
	 * class definitions will have.
	 * 
	 * @return the class name
	 */
	String getBaseVectorRClassName();
	
	/**
	 * Returns the length of this data store.
	 * 
	 * <p>If this is a data store dummy for structure only R objects in Java
	 * ({@link RObjectFactory#F_ONLY_STRUCT}), the value is <code>-1</code>.
	 * 
	 * @return the length or <code>-1</code>
	 */
	long getLength();
	
	/**
	 * Tests if the value at the specified index is NA.
	 * <p>
	 * Note that the analogous method to the R function <code>is.na(x)</code>
	 * is {@link #isMissing(int)}; this method returns <code>false</code> for NaN values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return <code>true</code>, if the value at the specified index is NA, otherwise <code>false</code>
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean isNA(int idx);
	
	/**
	 * Tests if the value at the specified index is NA.
	 * <p>
	 * Note that the analogous method to the R function <code>is.na(x)</code>
	 * is {@link #isMissing(int)}; this method returns <code>false</code> for NaN values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return <code>true</code>, if the value at the specified index is NA, otherwise <code>false</code>
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean isNA(long idx);
	
	/**
	 * Tests if the value at the specified index is NA <b>or</b> NaN (if supported by data type).
	 * It works by analogy with the R function <code>is.na(x)</code>.
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return <code>true</code>, if the value at the specified index is NA or NaN, otherwise <code>false</code>
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean isMissing(int idx);
	
	/**
	 * Tests if the value at the specified index is NA <b>or</b> NaN (if supported by data type).
	 * It works by analogy with the R function <code>is.na(x)</code>.
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return <code>true</code>, if the value at the specified index is NA or NaN, otherwise <code>false</code>
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean isMissing(long idx);
	
	/**
	 * Sets the value at the specified index to NA.
	 * <p>
	 * Note that raw doesn't support NAs.</p>.
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param idx the index of the value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setNA(int idx);
	
	/**
	 * Sets the value at the specified index to NA.
	 * <p>
	 * Note that raw doesn't support NAs.</p>.
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param idx the index of the value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setNA(long idx);
	
	/**
	 * Return the logical/boolean value at the specified index.
	 * <p>
	 * Logical values in R matches exactly the Java boolean values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current logical value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean getLogi(int idx);
	
	/**
	 * Return the logical/boolean value at the specified index.
	 * <p>
	 * Logical values in R matches exactly the Java boolean values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current logical value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	boolean getLogi(long idx);
	
	/**
	 * Sets the logical/boolean value at the specified index.
	 * <p>
	 * Logical values in R matches exactly the Java boolean values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param logical the logical value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setLogi(int idx, boolean logical);
	
	/**
	 * Sets the logical/boolean value at the specified index.
	 * <p>
	 * Logical values in R matches exactly the Java boolean values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param logical the logical value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setLogi(long idx, boolean logical);
	
	/**
	 * Return the integer/int value at the specified index.
	 * <p>
	 * Integer values in R matches exactly the Java int values, except
	 * {@link Integer#MIN_VALUE} which doesn't exists in R and is mapped to NA.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current integer value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	int getInt(int idx);
	
	/**
	 * Return the integer/int value at the specified index.
	 * <p>
	 * Integer values in R matches exactly the Java int values, except
	 * {@link Integer#MIN_VALUE} which doesn't exists in R and is mapped to NA.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current integer value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	int getInt(long idx);
	
	/**
	 * Sets the integer/int value at the specified index.
	 * <p>
	 * Integer values in R matches exactly the Java int values, except
	 * {@link Integer#MIN_VALUE} which doesn't exists in R and is mapped to NA.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param integer the integer value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setInt(int idx, int integer);
	
	/**
	 * Sets the integer/int value at the specified index.
	 * <p>
	 * Integer values in R matches exactly the Java int values, except
	 * {@link Integer#MIN_VALUE} which doesn't exists in R and is mapped to NA.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param integer the integer value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setInt(long idx, int integer);
	
	/**
	 * Return the numeric/real/double value at the specified index.
	 * <p>
	 * Numeric values in R matches exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current real value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getNum(int idx);
	
	/**
	 * Return the numeric/real/double value at the specified index.
	 * <p>
	 * Numeric values in R matches exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current real value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getNum(long idx);
	
	/**
	 * Sets the numeric/real/double value at the specified index.
	 * <p>
	 * Numeric values in R matches exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#NaN}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param numeric the real value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setNum(int idx, double numeric);
	
	/**
	 * Sets the numeric/real/double value at the specified index.
	 * <p>
	 * Numeric values in R matches exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#NaN}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param numeric the real value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setNum(long idx, double numeric);
	
	/**
	 * Returns the real part the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current real part of the complex number value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getCplxRe(int idx);
	
	/**
	 * Returns the real part the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current real part of the complex number value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getCplxRe(long idx);
	
	/**
	 * Returns the imaginary part the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current imaginary part of the complex number value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getCplxIm(int idx);
	
	/**
	 * Returns the imaginary part the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {Double#isNaN(double)}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current imaginary part of the complex number value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	double getCplxIm(long idx);
	
	/**
	 * Sets the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#NaN}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param real the real part of the complex number value
	 * @param imaginary the imaginary part of the complex number value
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setCplx(int idx, double real, double imaginary);
	
	/**
	 * Sets the complex number value at the specified index.
	 * <p>
	 * The numeric values of the parts of the complex number in R matches 
	 * exactly the Java double values.
	 * Also NaN, Inf and -Inf respectively {@link Double#NaN}, 
	 * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are 
	 * supported.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param real the real part of the complex number value
	 * @param imaginary the imaginary part of the complex number value
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setCplx(long idx, double real, double imaginary);
	
	/**
	 * Returns the character value at the specified index.
	 * <p>
	 * R character values matches exactly Java string values, if R supports
	 * UTF encoding.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current character value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	String getChar(int idx);
	
	/**
	 * Returns the character value at the specified index.
	 * <p>
	 * R character values matches exactly Java string values, if R supports
	 * UTF encoding.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current character value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	String getChar(long idx);
	
	/**
	 * Sets the character/String value at the specified index.
	 * <p>
	 * R character values matches exactly Java string values, if R supports
	 * UTF encoding.</p>
	 * <p>
	 * No quoting or escaping is required. Quoting or escaping the value
	 * would change the real value.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param character the character value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setChar(int idx, String character);
	
	/**
	 * Sets the character/String value at the specified index.
	 * <p>
	 * R character values matches exactly Java string values, if R supports
	 * UTF encoding.</p>
	 * <p>
	 * No quoting or escaping is required. Quoting or escaping the value
	 * would change the real value.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param character the character value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setChar(long idx, String character);
	
	/**
	 * Returns the raw/byte value at the specified index.
	 * <p>
	 * R raw values matches exactly Java byte values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current raw value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	byte getRaw(int idx);
	
	/**
	 * Returns the raw/byte value at the specified index.
	 * <p>
	 * R raw values matches exactly Java byte values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return the current raw value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	byte getRaw(long idx);
	
	/**
	 * Sets the raw/byte value at the specified index.
	 * <p>
	 * R raw values matches exactly Java byte values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param raw the raw value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setRaw(int idx, byte raw);
	
	/**
	 * Sets the raw/byte value at the specified index.
	 * <p>
	 * R raw values matches exactly Java byte values.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @param raw the raw value to set
	 * @throws OperationNotSupportedException if the store is not modifiable
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	void setRaw(long idx, byte raw);
	
	/**
	 * Returns the value at the specified index as Java object. The subtypes of 
	 * RStore defines more specific array types.
	 * <p>
	 * R NA values are represented as <code>null</code>. Java primitives are converted
	 * into its wrapper classes.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return an object for the current value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	P get(int idx);
	
	/**
	 * Returns the value at the specified index as Java object. The subtypes of 
	 * RStore defines more specific array types.
	 * <p>
	 * R NA values are represented as <code>null</code>. Java primitives are converted
	 * into its wrapper classes.</p>
	 * 
	 * @param idx the index (zero-based) of the value
	 * @return an object for the current value
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *     (idx &lt; 0 || idx &gt;= length()).
	 */
	P get(long idx);
	
	/**
	 * Returns the values of the store as Java object array. The subtypes of 
	 * RStore defines more specific array types.
	 * <p>
	 * The array is newly created for each call of this method. It has the
	 * length {@link #getLength()}. R NA values are represented as <code>null</code>
	 * in the array. Java primitives are converted into its wrapper classes.</p>
	 * 
	 * @return a object array with the values of the store
	 */
	P[] toArray();
	
	boolean allEqual(RStore<?> other);
	
	
	/**
	 * Returns if the store contains the specified character value.
	 * 
	 * @param value the value to search for
	 * @return <code>true</code> if the store contains the value, otherwise false
	 */
	boolean contains(final String value);
	
	/**
	 * Returns the index of the first item in the store equal to the of the specified character value.
	 * 
	 * @param character the value to search for
	 * @return index (zero-base) of first equal value, otherwise <code>-1</code>
	 */
	long indexOf(String character);
	
	/**
	 * Returns the index of the first item in the store equal to the of the specified character value,
	 * starting from the specified index.
	 * 
	 * @param character the value to search for
	 * @param fromIdx index (zero-based) from which to start the search
	 * @return index (zero-based) of first equal value, otherwise <code>-1</code>
	 */
	long indexOf(String character, long fromIdx);
	
	/**
	 * Returns if the store contains the specified integer value.
	 * 
	 * @param value the value to search for
	 * @return <code>true</code> if the store contains the value, otherwise false
	 */
	boolean contains(final int integer);
	
	/**
	 * Returns the index of the first item in the store equal to the of the specified integer value.
	 * 
	 * @param integer the value to search for
	 * @return index (zero-based) of first equal value, otherwise <code>-1</code>
	 */
	long indexOf(int integer);
	
	/**
	 * Returns if the store contains the specified integer value.
	 * 
	 * @param integer the value to search for
	 * @param fromIdx index (zero-based) from which to start the search
	 * @return index (zero-based) of first equal value, otherwise <code>-1</code>
	 */
	long indexOf(int integer, long fromIdx);
	
}
