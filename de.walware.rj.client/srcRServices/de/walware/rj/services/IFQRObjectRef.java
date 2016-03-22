/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services;

import de.walware.rj.data.RObject;


/**
 * Fully qualified reference to an R object.
 * 
 * @since 2.1 (provisional)
 */
public interface IFQRObjectRef {
	
	
	/**
	 * Handle to the R instance.
	 * 
	 * @return handle to R.
	 */
	Object getRHandle();
	
	/**
	 * The environment in R, specified by a call or reference.
	 * 
	 * @return a reference to the environment
	 */
	RObject getEnv();
	
	/**
	 * Name, relative to the environment, specified by a symbol or call.
	 * 
	 * @return the name
	 */
	RObject getName();
	
}
