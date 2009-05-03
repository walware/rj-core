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
 * Basic interface of all R object
 * 
 * To detect the type of an RObject instance, use always {@link #getRObjectType()},
 * not the java class type (e.g. instanceof).
 */
public interface RObject {
	
	public static final int TYPE_NULL =            0x00000001;
	public static final int TYPE_VECTOR =          0x00000002;
	public static final int TYPE_ARRAY =           0x00000003;
	public static final int TYPE_DATAFRAME =       0x00000006;
	public static final int TYPE_LIST =            0x00000007;
	public static final int TYPE_ENV =             0x00000008;
	public static final int TYPE_S4OBJECT =        0x0000000a;
	public static final int TYPE_FUNCTION =        0x0000000d;
	public static final int TYPE_REFERENCE =       0x0000000e;
	public static final int TYPE_OTHER =           0x0000000f;
	
	public static final String ATTR_ROW_NAMES = "row.names";
	public static final String ATTR_NAMES = "names";
	
	//-- Default class names --//
	public static final String CLASSNAME_LOGICAL = "logical";
	public static final String CLASSNAME_INTEGER = "integer";
	public static final String CLASSNAME_NUMERIC = "numeric";
	public static final String CLASSNAME_CHARACTER = "character";
	public static final String CLASSNAME_COMPLEX = "complex";
	public static final String CLASSNAME_RAW = "raw";
	public static final String CLASSNAME_FACTOR = "factor";
	public static final String CLASSNAME_ORDERED = "ordered";
	
	public static final String CLASSNAME_ARRAY = "array";
	public static final String CLASSNAME_MATRIX = "matrix";
	public static final String CLASSNAME_DATAFRAME = "data.frame";
	public static final String CLASSNAME_LIST = "list";
	public static final String CLASSNAME_ENV = "environment";
	
	public static final String CLASSNAME_NULL = "NULL";
	
	
	/**
	 * The object type of this object
	 * 
	 * See the object type constants <code>TYPE_</code> defined in {@link RObject}.
	 * 
	 * @return the object type constant
	 */
	public int getRObjectType();
	
	/**
	 * The class name of this object in R
	 * 
	 * If the object has multiple S3 class names in R, it returns the first one.
	 *   <code>class(x)[1]</code>
	 * 
	 * @return the R class name
	 */
	public String getRClassName();
	
	/**
	 * The length of the object
	 * 
	 * Depends on the R object type and doesn't return a valid value
	 * for unknown types.
	 * 
	 * @return the length
	 */
	int getLength();
	
	/**
	 * The data store containing object data as 1-dim structure
	 * 
	 * @return the data store or <code>null</code>, if not supported by the object type
	 */
	RStore getData();
	
	/**
	 * The attribute list of the object
	 * 
	 * Note: By default the attributes are not loaded.
	 * 
	 * @return the attribute list or <code>null</code>, if not available
	 */
	RList getAttributes();
	
}
