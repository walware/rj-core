/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
import java.io.ObjectInput;

import de.walware.rj.data.RJIO;


public class RUniqueCharacterDataImpl extends RCharacterDataImpl {
	
	
	public RUniqueCharacterDataImpl(final String[] initialValues) {
		super(initialValues, initialValues.length);
	}
	
	RUniqueCharacterDataImpl(final RCharacterDataImpl source, final boolean reuse) {
		super(source, reuse);
	}
	
	public RUniqueCharacterDataImpl(final RJIO io) throws IOException {
		super(io);
	}
	
	public RUniqueCharacterDataImpl(final ObjectInput in) throws IOException {
		super(in);
	}
	
	
	@Override
	public void setChar(final int idx, final String value) {
		if (indexOf(value) >= 0) {
			if (indexOf(value) == idx) {
				return;
			}
			throw new IllegalArgumentException();
		}
		super.setChar(idx, value);
	}
	
	@Override
	public void insertChar(final int idx, final String value) {
		if (indexOf(value) >= 0) {
			throw new IllegalArgumentException();
		}
		super.insertChar(idx, value);
	}
	
	@Override
	public void setNA(final int idx) {
	}
	
	@Override
	public void insertNA(final int idx) {
	}
	
	@Override
	public void insertNA(final int[] idxs) {
	}
	
	protected void insertAuto(final int idx) {
		insertChar(idx, createAuto(idx));
	}
	
	protected String createAuto(final int idx) {
		final String nr = Integer.toString(idx+1);
		if (indexOf(nr) < 0) {
			return nr;
		}
		for (int i = 1; ; i++) {
			final String sub = nr+'.'+Integer.toString(i);
			if (indexOf(sub) < 0) {
				return sub;
			}
		}
	}
	
}
