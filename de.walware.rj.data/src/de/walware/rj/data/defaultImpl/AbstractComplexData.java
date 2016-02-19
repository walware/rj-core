/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractComplexData extends AbstractRData<RComplexStore.Complex>
		implements RComplexStore {
	
	
	protected static final String toChar(final double real, final double imaginary) {
		final StringBuilder sb = new StringBuilder();
		sb.append(real);
		sb.append('+');
		sb.append(imaginary);
		sb.append('i');
		return sb.toString();
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.COMPLEX;
	}
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_COMPLEX;
	}
	
	
	@Override
	public final String getChar(final int idx) {
		return toChar(getCplxRe(idx), getCplxIm(idx));
	}
	
	@Override
	public final String getChar(final long idx) {
		return toChar(getCplxRe(idx), getCplxIm(idx));
	}
	
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		throw new UnsupportedOperationException();
	}
	
	
	@Override
	public abstract Complex[] toArray();
	
}
