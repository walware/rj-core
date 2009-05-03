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
 * RObject of type 'environment'
 */
public interface REnvironment extends RList {
	
	/**
	 * Environment name of the global environment
	 * (<code>.GlobalEnv</code>)
	 * 
	 * Name: {@value}
	 **/
	public static final String ENVNAME_GLOBAL = "R_GlobalEnv";
	
	/**
	 * Environment name of the empty environment
	 * (<code>emptyenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	public static final String ENVNAME_EMPTY = "R_EmptyEnv";
	
	/**
	 * Environment name of the base environment
	 * (<code>baseenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	public static final String ENVNAME_BASE = "base";
	
	/**
	 * Environment name of the Autoloads environment
	 * (<code>.AutoloadEnv</code>)
	 * 
	 * Name: {@value}
	 **/
	public static final String ENVNAME_AUTOLOADS = "Autoloads";
	
	public static final int ENVTYPE_BASE = 1;
	public static final int ENVTYPE_AUTOLOADS = 2;
	public static final int ENVTYPE_PACKAGE = 5;
	public static final int ENVTYPE_GLOBAL = 7;
	public static final int ENVTYPE_EMTPY = 9;
	
	
	/**
	 * Indicating a special type (> 0)
	 * see <code>ENVTYPE_</code> constants above.
	 * 
	 * @return the type constant
	 */
	public int getSpecialType();
	
	/**
	 * Name of the environment 
	 * (<code>environmentName(x)</code>)
	 * 
	 * @return
	 */
	public String getEnvironmentName();
	
}
