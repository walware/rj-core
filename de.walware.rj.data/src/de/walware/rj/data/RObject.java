/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;

import de.walware.rj.data.defaultImpl.RNull;


/**
 * Basic interface of all R object.
 * <p>
 * To detect the type of an RObject instance, the method {@link #getRObjectType()} 
 * should be used, not the java class type (e.g. instanceof) of the object.</p>
 */
public interface RObject {
	
	/**
	 * Constant indicating the RNull object respectively the R NULL value.
	 * <p>
	 * The object is an instance of {@link RNull}.</p>
	 */
	byte TYPE_NULL =            0x01;
	
	/**
	 * Constant indicating an {@link RVector} object. An R object is of this type
	 * if it is a R data object of an "atomic" mode without a dimension attribute
	 * (<code>dim</dim>).
	 * <p>
	 * The object is an instance of {@link RVector}.</p>
	 */
	byte TYPE_VECTOR =          0x02;
	
	/**
	 * Constant indicating an {@link RArray} object. An R object is of this type
	 * if it is a R data object of an "atomic" mode with a dimension attribute
	 * (<code>dim</dim>).
	 * <p>
	 * The object is an instance of {@link RArray}.</p>
	 */
	byte TYPE_ARRAY =           0x03;
	
	/**
	 * Constant indicating an RDataFrame object. An R object is of this type
	 * if it is an R list object inheriting the R class {@link #CLASSNAME_DATAFRAME data.frame}
	 * and compiling with the R rules for a data frame, especially its children 
	 * are {@link RVector}s of the same length.
	 * <p>
	 * The object is an instance of {@link RDataFrame}.</p>
	 */
	byte TYPE_DATAFRAME =       0x06;
	
	/**
	 * Constant indicating an RList object. An R object is of this type if it is 
	 * a list but not a data frame (see {@link #TYPE_DATAFRAME}).
	 * <p>
	 * The object is an instance of {@link RList}.</p>
	 */
	byte TYPE_LIST =            0x07;
	
	/**
	 * Constant indicating an R environment object.
	 * <p>
	 * The object is an instance of {@link REnvironment}.</p>
	 */
	byte TYPE_ENV =             0x08;
	
	/**
	 * Constant indicating an S4 object. An R object is of this type if the R
	 * command <code>isS4</code> returns true. This is criterion has priority
	 * above the criteria for the other data types. If an S4 object represents 
	 * also a simple data type, this data is accessible by its data slot.
	 * <p>
	 * The object is an instance of {@link RS4Object}.</p>
	 */
	byte TYPE_S4OBJECT =        0x0a;
	
	
	/**
	 * Constant indicating an R language object.
	 * <p>
	 * The object is an instance of {@link RLanguage}.</p>
	 */
	byte TYPE_LANGUAGE =        0x0c;
	
	/**
	 * Constant indicating an R function object.
	 * <p>
	 * The object is an instance of {@link RFunction}.</p>
	 */
	byte TYPE_FUNCTION =        0x0d;
	
	/**
	 * Constant indicating a reference to a R object.
	 * <p>
	 * The object is an instance of {@link RReference}.</p>
	 */
	byte TYPE_REFERENCE =       0x0e;
	
	/**
	 * Constant indicating an R object not matching one of the other types.
	 */
	byte TYPE_OTHER =           0x0f;
	
	/**
	 * Constant indicating an R object is missing (e.g. missing argument, missing slot value).
	 * 
	 * @since 0.5.0
	 */
	byte TYPE_MISSING =         0x11;
	
	/**
	 * Constant indicating an R object is not yet evaluated.
	 * 
	 * @since 0.6.0
	 */
	byte TYPE_PROMISE =         0x12;
	
	
	//-- Common class names --//
	String CLASSNAME_LOGICAL = "logical";
	String CLASSNAME_INTEGER = "integer";
	String CLASSNAME_NUMERIC = "numeric";
	String CLASSNAME_CHARACTER = "character";
	String CLASSNAME_COMPLEX = "complex";
	String CLASSNAME_RAW = "raw";
	String CLASSNAME_FACTOR = "factor";
	String CLASSNAME_ORDERED = "ordered";
	
	String CLASSNAME_ARRAY = "array";
	String CLASSNAME_MATRIX = "matrix";
	String CLASSNAME_DATAFRAME = "data.frame";
	String CLASSNAME_LIST = "list";
	String CLASSNAME_PAIRLIST = "pairlist";
	String CLASSNAME_ENV = "environment";
	String CLASSNAME_NAME = "name";
	String CLASSNAME_EXPRESSION = "expression";
	String CLASSNAME_CALL = "call";
	
	String CLASSNAME_NULL = "NULL";
	
	//-- Common attribute names --//
	String ATTR_ROW_NAMES = "row.names";
	String ATTR_NAMES = "names";
	
	
	/**
	 * Returns the object type constant of this object
	 * <p>
	 * See the object type constants <code>TYPE_</code> defined in {@link RObject}.</p>
	 * 
	 * @return the object type constant
	 */
	byte getRObjectType();
	
	/**
	 * Returns the class name of this object in R.
	 * <p>
	 * If the object has multiple S3 class names in R, it returns the first one.
	 * The analog R command is <code>class(x)[1]</code>.</p>
	 * 
	 * @return the R class name
	 */
	String getRClassName();
	
	/**
	 * Returns the length of the object.
	 * <p>
	 * Its meaning depends on the R object type and is undefined for unknown types.</p>
	 * 
	 * @return the length
	 */
	long getLength();
	
	/**
	 * Returns the data store containing object data in a one-dimensional structure.
	 * <p>
	 * This is supported by objects of {@link #TYPE_VECTOR}, {@link #TYPE_ARRAY} and
	 * of {@link #TYPE_S4OBJECT} with a data slot of a one of these types providing a data
	 * store.</p>
	 * 
	 * @return the data store or <code>null</code>, if not supported by the object
	 */
	RStore<?> getData();
	
	/**
	 * Returns the attribute list of the object
	 * <p>
	 * Note that by default the attributes are not loaded.</p>
	 * 
	 * @return the attribute list or <code>null</code>, if not available
	 */
	RList getAttributes();
	
}
