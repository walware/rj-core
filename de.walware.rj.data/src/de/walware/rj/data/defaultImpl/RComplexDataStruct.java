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


public class RComplexDataStruct extends AbstractComplexData {
	
	
	public RComplexDataStruct() {
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return true;
	}
	
	
	@Override
	public final long getLength() {
		return -1;
	}
	
	@Override
	public boolean isNA(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isNA(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isNaN(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isNaN(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isMissing(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Complex get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Complex get(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Complex[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public long indexOf(final String character, final long fromIdx) {
		throw new UnsupportedOperationException();
	}
	
	
	@Override
	public boolean allEqual(final RStore other) {
		return (COMPLEX == other.getStoreType()
				&& -1 == other.getLength() );
	}
	
}
