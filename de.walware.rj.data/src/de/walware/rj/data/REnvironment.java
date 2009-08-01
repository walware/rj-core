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
 * An R object is of the type {@link RObject#TYPE_ENV environment}, if the
 * object is an R environment.  Such an R environment object is represented
 * by an instance of this interface.
 * <p>
 * The R function <code>typeof(object)</code> returns 'environment' for objects
 * of this type.</p>
 * <p>
 * Even the interface extends {@link RList}, the objects are not a list in R!
 * The inheritance is only for a uniform API.</p>
 */
public interface REnvironment extends RList {
	
	/**
	 * Environment name of the global environment
	 * (<code>.GlobalEnv</code>)
	 * 
	 * Name: {@value}
	 **/
	static final String ENVNAME_GLOBAL = "R_GlobalEnv";
	
	/**
	 * Environment name of the empty environment
	 * (<code>emptyenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	static final String ENVNAME_EMPTY = "R_EmptyEnv";
	
	/**
	 * Environment name of the base environment
	 * (<code>baseenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	static final String ENVNAME_BASE = "base";
	
	/**
	 * Environment name of the Autoloads environment
	 * (<code>.AutoloadEnv</code>)
	 * 
	 * Name: {@value}
	 **/
	static final String ENVNAME_AUTOLOADS = "Autoloads";
	
	static final int ENVTYPE_BASE = 1;
	static final int ENVTYPE_AUTOLOADS = 2;
	static final int ENVTYPE_PACKAGE = 5;
	static final int ENVTYPE_GLOBAL = 7;
	static final int ENVTYPE_EMTPY = 9;
	
	
	/**
	 * Returns the length of the object. The length of an {@link RObject#TYPE_ENV environment}
	 * is the count the objects in the environment.
	 * 
	 * @return the length
	 */
	int getLength();
	
	/**
	 * Indicates a special environment type (> 0)
	 * see <code>ENVTYPE_</code> constants defined in {@link REnvironment}.
	 * 
	 * @return the type constant or &lt;= 0
	 */
	int getSpecialType();
	
	/**
	 * Returns the environment name of the environment.  This is the return value
	 * of the R command <code>environmentName(object)</code>.
	 * 
	 * @return the environment name
	 */
	String getEnvironmentName();
	
}
