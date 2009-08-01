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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.SimpleRList;


public class SimpleRListImpl<T> implements SimpleRList<T> {
	
	
	private final RCharacterStore names;
	private final T[] values;
	
	
	public SimpleRListImpl(final RCharacterStore names, final T[] values) {
		this.names = names;
		this.values = values;
	}
	
	
	public int getLength() {
		return this.names.getLength();
	}
	
	public RCharacterStore getNames() {
		return this.names;
	}
	
	public String getName(final int idx) {
		return this.names.get(idx);
	}
	
	public T get(final int idx) {
		return this.values[idx];
	}
	
	public T get(final String name) {
		final int idx = this.names.indexOf(name);
		if (idx >= 0) {
			return this.values[idx];
		}
		return null;
	}
	
}
