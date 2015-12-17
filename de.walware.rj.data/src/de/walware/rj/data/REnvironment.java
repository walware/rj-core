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
	String ENVNAME_GLOBAL = "R_GlobalEnv";
	
	/**
	 * Environment name of the empty environment
	 * (<code>emptyenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	String ENVNAME_EMPTY = "R_EmptyEnv";
	
	/**
	 * Environment name of the base environment
	 * (<code>baseenv()</code>)
	 * 
	 * Name: {@value}
	 **/
	String ENVNAME_BASE = "base";
	
	/**
	 * Environment name of the Autoloads environment
	 * (<code>.AutoloadEnv</code>)
	 * 
	 * Name: {@value}
	 **/
	String ENVNAME_AUTOLOADS = "Autoloads";
	
	
	byte ENVTYPE_BASE=                                      0x01;
	byte ENVTYPE_AUTOLOADS=                                 0x02;
	byte ENVTYPE_PACKAGE=                                   0x05;
	byte ENVTYPE_GLOBAL=                                    0x07;
	
	byte ENVTYPE_EMTPY=                                     0x09;
	
	/**
	 * @since de.walware.rj.data 2.1
	 */
	byte ENVTYPE_NAMESPACE=                                 0x0B;
	/**
	 * @since de.walware.rj.data 2.1
	 */
	byte ENVTYPE_NAMESPACE_EXPORTS=                         0x0C;
	
	
	/**
	 * Returns the length of the object. The length of an {@link RObject#TYPE_ENV environment}
	 * is the count the objects in the environment.
	 * 
	 * At moment, the length of an {@link RObject#TYPE_ENV environment} is always &le; 2<sup>31</sup>-1
	 * (representable by Java int).
	 * 
	 * @return the length
	 */
	@Override
	long getLength();
	
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
	
	long getHandle();
	
}
