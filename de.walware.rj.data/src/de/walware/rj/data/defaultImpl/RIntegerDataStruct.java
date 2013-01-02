/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RStore;


public class RIntegerDataStruct extends AbstractIntegerData {
	
	
	public RIntegerDataStruct() {
		this.length = -1;
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return true;
	}
	
	
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public Integer get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final Integer[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean allEqual(final RStore other) {
		return (other.getStoreType() == INTEGER && other.getLength() == -1);
	}
	
}
