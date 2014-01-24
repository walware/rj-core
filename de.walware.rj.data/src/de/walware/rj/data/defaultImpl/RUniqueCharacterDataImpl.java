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

import java.io.IOException;

import de.walware.rj.data.RJIO;


public class RUniqueCharacterDataImpl extends RCharacterDataImpl {
	
	
	public RUniqueCharacterDataImpl() {
		super();
	}
	
	public RUniqueCharacterDataImpl(final String[] initialValues) {
		super(initialValues, initialValues.length);
	}
	
	RUniqueCharacterDataImpl(final RCharacterDataImpl source, final boolean reuse) {
		super(source, reuse);
	}
	
	public RUniqueCharacterDataImpl(final RJIO io, final int length) throws IOException {
		super(io, length);
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
	public void setChar(final long idx, final String value) {
		if (idx < 0 || idx >= getLength()) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		setChar((int) idx, value);
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
	public void setNA(final long idx) {
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
