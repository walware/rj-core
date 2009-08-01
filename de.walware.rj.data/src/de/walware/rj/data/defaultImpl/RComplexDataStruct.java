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

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RStore;


public class RComplexDataStruct extends AbstractComplexData {
	
	
	public RComplexDataStruct(final int length) {
		this.length = length;
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return true;
	}
	
	
	public boolean isNaN(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public Object get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	public boolean allEqual(final RStore other) {
		return (other.getStoreType() == COMPLEX && other.getLength() == -1);
	}
	
}
