/*******************************************************************************
 * Copyright (c) 2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jriImpl;

import java.io.IOException;
import java.io.ObjectInput;

import de.walware.rj.data.defaultImpl.RComplexDataBImpl;


public class JRIComplexDataImpl extends RComplexDataBImpl {
	
	
	public JRIComplexDataImpl(final double[] realValues, final double[] imaginaryValues) {
		super(realValues, imaginaryValues);
		for (int i = 0; i < imaginaryValues.length; i++) {
			if (Double.isNaN(imaginaryValues[i])) {
				if (Double.doubleToRawLongBits(imaginaryValues[i]) == NA_numeric_LONG) {
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
	
	public JRIComplexDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		super(in);
	}
	
	
	public double[] getJRIRealValueArray() {
		if (this.realValues.length == this.length) {
			return this.realValues;
		}
		final double[] array = new double[this.length];
		System.arraycopy(this.realValues, 0, array, 0, this.length);
		return array;
	}
	
	public double[] getJRIImaginaryValueArray() {
		if (this.imaginaryValues.length == this.length) {
			return this.imaginaryValues;
		}
		final double[] array = new double[this.length];
		System.arraycopy(this.imaginaryValues, 0, array, 0, this.length);
		return array;
	}
	
}
