/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.defaultImpl.RNumericDataBImpl;


public class JRINumericDataImpl extends RNumericDataBImpl {
	
	
	public JRINumericDataImpl(final double[] values) {
		super(values);
	}
	
	public JRINumericDataImpl(final RJIO io, final int length) throws IOException {
		super(io, length);
	}
	
	
	public double[] getJRIValueArray() {
		final int l = length();
		if (this.realValues.length == l) {
			return this.realValues;
		}
		final double[] array = new double[l];
		System.arraycopy(this.realValues, 0, array, 0, l);
		return array;
	}
	
}
