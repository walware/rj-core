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

package de.walware.rj.services.utils.dataaccess;

import de.walware.rj.data.RStore;


public class RDataAssignment extends RDataSubset {
	
	
	private final RStore<?> data;
	
	
	public RDataAssignment(final long rowIdx, final long columnIdx, final RStore<?> data) {
		super(rowIdx, 1, columnIdx, 1);
		
		if (data == null) {
			throw new NullPointerException("data"); //$NON-NLS-1$
		}
		if (data.getLength() > getLength()) {
			throw new IllegalArgumentException("data.length"); //$NON-NLS-1$
		}
		this.data= data;
	}
	
	
	public RStore<?> getData() {
		return this.data;
	}
	
	
}
