/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RStore;


public class RCharacterDataStruct extends AbstractCharacterData {
	
	
	public RCharacterDataStruct() {
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
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isMissing(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String get(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String[] toArray() {
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
	public boolean allEqual(final RStore<?> other) {
		return (CHARACTER == other.getStoreType()
				&& other.getLength() == -1 );
	}
	
}
