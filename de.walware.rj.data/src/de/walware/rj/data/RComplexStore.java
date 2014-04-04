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

package de.walware.rj.data;

import de.walware.rj.data.RComplexStore.Complex;


/**
 * Interface for R data stores of type {@link RStore#COMPLEX}.
 * <p>
 * An R data store implements this interface if the R function
 * <code>typeof(object)</code> returns 'complex'.</p>
 */
public interface RComplexStore extends RStore<Complex> {
	
	
	final class Complex {
		
		
		private final double realValue;
		private final double imaginaryValue;
		
		
		public Complex(final double real, final double imaginary) {
			this.realValue = real;
			this.imaginaryValue = imaginary;
		}
		
		
		public double getRe() {
			return this.realValue;
		}
		
		public double getIm() {
			return this.imaginaryValue;
		}
		
		
		@Override
		public int hashCode() {
			final long realBits = Double.doubleToLongBits(this.realValue);
			final long imaginaryBits = Double.doubleToLongBits(this.imaginaryValue) + 1;
			return (int) ((realBits ^ (realBits >>> 32)) | (imaginaryBits ^ (imaginaryBits >>> 32)));
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof Complex)) {
				return false;
			}
			final Complex other = (Complex) obj;
			return (this.realValue == other.realValue && this.imaginaryValue == other.imaginaryValue);
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder(32);
			sb.append(Double.toString(this.realValue));
			if (this.imaginaryValue >= 0.0) {
				sb.append('+');
			}
			sb.append(Double.toString(this.imaginaryValue));
			sb.append('i');
			return sb.toString();
		}
		
	}
	
	
	boolean isNaN(int idx);
	boolean isNaN(long idx);
	
	@Override
	Complex get(int idx);
	@Override
	Complex get(long idx);
	
	@Override
	Complex[] toArray();
	
}
