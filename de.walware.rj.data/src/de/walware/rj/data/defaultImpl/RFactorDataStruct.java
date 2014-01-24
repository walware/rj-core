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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RStore;


public class RFactorDataStruct extends AbstractFactorData {
	
	
	public static RFactorDataStruct addLevels(final RFactorStore store, final RCharacterStore levels) {
		final long levelCount = levels.getLength();
		if (levelCount > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("levelCount > 2^31-1");
		}
		return new RFactorDataStruct(store.isOrdered(), (int) levels.getLength()) {
			@Override
			public RCharacterStore getLevels() {
				return levels;
			}
		};
	}
	
	
	private final int levelCount;
	
	
	public RFactorDataStruct(final boolean isOrdered, final int levelCount) {
		this.isOrdered = isOrdered;
		this.levelCount = levelCount;
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
	public final int getLevelCount() {
		return this.levelCount;
	}
	
	@Override
	public RCharacterStore getLevels() {
		throw new UnsupportedOperationException();
	}
	
	public void insertLevel(final int position, final String label) {
		throw new UnsupportedOperationException();
	}
	
	public void removeLevel(final String label) {
		throw new UnsupportedOperationException();
	}
	
	public void renameLevel(final String oldLabel, final String newLabel) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public RCharacterStore toCharacterData() {
		return new RCharacterDataStruct();
	}
	
	@Override
	public Integer get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Integer get(final long idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final Integer[] toArray() {
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
		return (FACTOR == other.getStoreType()
				&& this.isOrdered == ((RFactorStore) other).isOrdered()
				&& this.levelCount == ((RFactorStore) other).getLevelCount()
				&& -1 == other.getLength() );
	}
	
}
