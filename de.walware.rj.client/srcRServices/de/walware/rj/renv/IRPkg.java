/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.renv;


/**
 * R package.
 * 
 * Basic interface for representation of an R package.
 * 
 * @since de.walware.rj.renv 2.0
 */
public interface IRPkg {
	
	
	/**
	 * The name of the package.
	 * 
	 * @return the name
	 */
	String getName();
	
	/**
	 * The version of the package.
	 * 
	 * @return the version
	 */
	RNumVersion getVersion();
	
}
