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


public class RCharacterDataStruct extends AbstractCharacterData {
	
	
	public RCharacterDataStruct() {
		this.length = -1;
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return true;
	}
	
	
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public String get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public final String[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean allEqual(final RStore other) {
		return (other.getStoreType() == CHARACTER && other.getLength() == -1);
	}
	
	@Override
	public int indexOf(final String value, final int fromIdx) {
		throw new UnsupportedOperationException();
	}
	
}
