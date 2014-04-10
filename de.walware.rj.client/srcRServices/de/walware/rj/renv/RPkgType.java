/*=============================================================================#
 # Copyright (c) 2012-2014 Stephan Wahlbrink (WalWare.de) and others.
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
 * Type ({@link #SOURCE source} or {@link #BINARY binary}) of R packages.
 * 
 * @since 2.0
 */
public enum RPkgType {
	
	
	SOURCE("Source"),
	BINARY("Binary");
	
	
	private final String label;
	
	
	private RPkgType(final String label) {
		if (label == null) {
			throw new NullPointerException("label"); //$NON-NLS-1$
		}
		this.label = label;
	}
	
	
	public String getLabel() {
		return this.label;
	}
	
	
}
