/*=============================================================================#
 # Copyright (c) 2010-2016 Stephan Wahlbrink (WalWare.de) and others.
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
import de.walware.rj.data.defaultImpl.RComplexDataBImpl;


public class JRIComplexDataShortImpl extends RComplexDataBImpl {
	
	
	public JRIComplexDataShortImpl(final double[] realValues, final double[] imaginaryValues) {
		super(realValues, imaginaryValues);
		for (int i = 0; i < imaginaryValues.length; i++) {
			if (Double.isNaN(imaginaryValues[i])) {
				if ((int) Double.doubleToRawLongBits(imaginaryValues[i]) == NA_numeric_INT_MATCH) {
					realValues[i] = NA_numeric_DOUBLE;
					imaginaryValues[i] = NA_numeric_DOUBLE;
				}
				else {
					realValues[i] = NaN_numeric_DOUBLE;
					imaginaryValues[i] = NaN_numeric_DOUBLE;
				}
			}
		}
	}
	
	public JRIComplexDataShortImpl(final RJIO io, final int length) throws IOException {
		super(io, length);
	}
	
	
	public double[] getJRIRealValueArray() {
		final int l = length();
		if (this.realValues.length == l) {
			return this.realValues;
		}
		final double[] array = new double[l];
		System.arraycopy(this.realValues, 0, array, 0, l);
		return array;
	}
	
	public double[] getJRIImaginaryValueArray() {
		final int l = length();
		if (this.imaginaryValues.length == l) {
			return this.imaginaryValues;
		}
		final double[] array = new double[l];
		System.arraycopy(this.imaginaryValues, 0, array, 0, l);
		return array;
	}
	
}
