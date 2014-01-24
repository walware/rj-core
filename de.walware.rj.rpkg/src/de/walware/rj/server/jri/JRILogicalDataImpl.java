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

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.defaultImpl.RLogicalDataIntImpl;


public class JRILogicalDataImpl extends RLogicalDataIntImpl {
	
	
	public JRILogicalDataImpl(final boolean[] values) {
		super(values, null);
	}
	
	public JRILogicalDataImpl(final int[] value) {
		super(value);
	}
	
	public JRILogicalDataImpl(final RJIO io, final int length) throws IOException {
		super(io, length);
	}
	
	
	public int[] getJRIValueArray() {
		final int l = length();
		if (this.boolValues.length == l) {
			return this.boolValues;
		}
		final int[] array = new int[l];
		System.arraycopy(this.boolValues, 0, array, 0, l);
		return array;
	}
	
}
