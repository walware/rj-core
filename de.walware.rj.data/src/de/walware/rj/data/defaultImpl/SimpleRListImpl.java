/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RCharacterStore;


public class SimpleRListImpl<T> {
	
	
	private final RCharacterDataImpl names;
	private final T[] values;
	
	
	public SimpleRListImpl(final T[] values, final RCharacterDataImpl names) {
		this.names = names;
		this.values = values;
	}
	
	
	public int getLength() {
		return this.names.length();
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
		final int idx = this.names.indexOf(name, 0);
		if (idx >= 0) {
			return this.values[idx];
		}
		return null;
	}
	
}
