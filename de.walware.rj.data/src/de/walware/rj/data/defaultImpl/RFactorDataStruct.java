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

import java.util.List;

import de.walware.rj.data.RCharacterStore;


public class RFactorDataStruct extends AbstractFactorData {
	
	
	private int levelCount;
	
	
	public RFactorDataStruct(final int length, final boolean isOrdered, final int levelCount) {
		this.length = length;
		this.isOrdered = isOrdered;
		this.levelCount = levelCount;
	}
	
	
	public void addLevel(final String label) {
		throw new UnsupportedOperationException();
	}
	
	public int getLevelCount() {
		return this.levelCount;
	}
	
	public List<String> getLevels() {
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
	
	public RCharacterStore toCharacterData() {
		return new RCharacterDataStruct(this.length);
	}
	
	public Integer[] toArray() {
		throw new UnsupportedOperationException();
	}
	
}
