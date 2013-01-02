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

import java.io.IOException;
import java.util.HashMap;

import de.walware.rj.data.RJIO;


public class RUniqueCharacterDataWithHashImpl extends RUniqueCharacterDataImpl {
	
	
	private final HashMap<String, Integer> map;
	
	
	public RUniqueCharacterDataWithHashImpl(final String names[]) {
		super(names);
		this.map = new HashMap<String, Integer>();
		initMap();
	}
	
	RUniqueCharacterDataWithHashImpl(final RCharacterDataImpl source, final boolean reuse) {
		super(source, reuse);
		this.map = new HashMap<String, Integer>();
		initMap();
	}
	
	public RUniqueCharacterDataWithHashImpl(final RJIO io) throws IOException {
		super(io);
		this.map = new HashMap<String, Integer>();
		initMap();
	}
	
	
	protected void initMap() {
		for (int idx = 0; idx < this.length; idx++) {
			if (this.charValues[idx] != null) {
				this.map.put(this.charValues[idx], idx);
			}
		}
	}
	
	
	@Override
	public int indexOf(final String name, final int fromIdx) {
		return this.map.get(name);
	}
	
	@Override
	public void setChar(final int idx, final String value) {
		final String previous = getChar(idx);
		super.setChar(idx, value);
		this.map.remove(previous);
		this.map.put(value, idx);
	}
	
	@Override
	public void insertChar(final int idx, final String name) {
		super.insertChar(idx, name);
		this.map.put(name, idx);
	}
	
	@Override
	public void remove(final int idx) {
		this.map.remove(getChar(idx));
		super.remove(idx);
	}
	
	
	@Override
	public boolean contains(final String value) {
		return (value != null && this.map.containsKey(value));
	}
	
}
