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

import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractNumericData extends AbstractRData<Double>
		implements RNumericStore {
	
	
	protected static final String toChar(final double num) {
		return Double.toString(num); // not exactly like R
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.NUMERIC;
	}
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_NUMERIC;
	}
	
	
	@Override
	public boolean getLogi(final int idx) {
		return getNum(idx) != 0.0;
	}
	
	@Override
	public final boolean getLogi(final long idx) {
		return getNum(idx) != 0.0;
	}
	
	@Override
	public final void setLogi(final int idx, final boolean logi) {
		setNum(idx, (logi) ? 1.0 : 0.0);
	}
	
	@Override
	public final void setLogi(final long idx, final boolean logi) {
		setNum(idx, (logi) ? 1.0 : 0.0);
	}
	
	@Override
	public final int getInt(final int idx) {
		return (int) getNum(idx);
	}
	
	@Override
	public final int getInt(final long idx) {
		return (int) getNum(idx);
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		setNum(idx, integer);
	}
	
	@Override
	public final void setInt(final long idx, final int integer) {
		setNum(idx, integer);
	}
	
	@Override
	public final double getCplxRe(final int idx) {
		return getNum(idx);
	}
	
	@Override
	public final double getCplxRe(final long idx) {
		return getNum(idx);
	}
	
	@Override
	public final double getCplxIm(final int idx) {
		return 0.0;
	}
	
	@Override
	public final double getCplxIm(final long idx) {
		return 0.0;
	}
	
	@Override
	public final String getChar(final int idx) {
		return toChar(getNum(idx));
	}
	
	@Override
	public String getChar(final long idx) {
		return toChar(getNum(idx));
	}
	
	
	@Override
	public abstract Double[] toArray();
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		final long length = getLength();
		if (NUMERIC != other.getStoreType() || length != other.getLength()) {
			return false;
		}
		if (length < 0) {
			return true;
		}
		else if (length <= Integer.MAX_VALUE) {
			final int ilength = (int) length;
			for (int idx = 0; idx < ilength; idx++) {
				if (!(isMissing(idx) ?
						(isNA(idx) ? other.isNA(idx) : other.isMissing(idx)) :
						(Math.abs(getNum(idx) - other.getNum(idx)) < 2.220446e-16 )) ) {
					return false;
				}
			}
		}
		else {
			for (long idx = 0; idx < length; idx++) {
				if (!(isMissing(idx) ?
						(isNA(idx) ? other.isNA(idx) : other.isMissing(idx)) :
						(Math.abs(getNum(idx) - other.getNum(idx)) < 2.220446e-16 )) ) {
					return false;
				}
			}
		}
		return true;
	}
	
}
