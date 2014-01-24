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
import de.walware.rj.data.defaultImpl.RRawDataImpl;


public class JRIRawDataImpl extends RRawDataImpl {
	
	
	public JRIRawDataImpl(final byte[] values) {
		super(values);
	}
	
	public JRIRawDataImpl(final RJIO io, final int length) throws IOException {
		super(io, length);
	}
	
	
	public byte[] getJRIValueArray() {
		final int l = length();
		if (this.byteValues.length == l) {
			return this.byteValues;
		}
		final byte[] array = new byte[length()];
		System.arraycopy(this.byteValues, 0, array, 0, l);
		return array;
	}
	
}
