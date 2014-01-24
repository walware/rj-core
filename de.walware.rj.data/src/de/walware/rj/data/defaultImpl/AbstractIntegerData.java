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

import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractIntegerData extends AbstractRData
		implements RIntegerStore {
	
	
	protected static final byte toRaw(final int integer) {
		if ((integer & 0xffffff00) == 0) {
			return (byte) (integer & 0xff);
		}
		throw new NumberFormatException(Integer.toString(integer));
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.INTEGER;
	}
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_INTEGER;
	}
	
	
	@Override
	public final boolean getLogi(final int idx) {
		return (getInt(idx) != 0);
	}
	
	@Override
	public final boolean getLogi(final long idx) {
		return (getInt(idx) != 0);
	}
	
	@Override
	public final void setLogi(final int idx, final boolean logi) {
		setInt(idx, logi ? 1 : 0);
	}
	
	@Override
	public final void setLogi(final long idx, final boolean logi) {
		setInt(idx, logi ? 1 : 0);
	}
	
	@Override
	public final double getNum(final int idx) {
		return getInt(idx);
	}
	
	@Override
	public final double getNum(final long idx) {
		return getInt(idx);
	}
	
	@Override
	public final void setNum(final int idx, final double real) {
		setInt(idx, (int) real);
	}
	
	@Override
	public final void setNum(final long idx, final double real) {
		setInt(idx, (int) real);
	}
	
	@Override
	public final String getChar(final int idx) {
		return Integer.toString(getInt(idx));
	}
	
	@Override
	public final String getChar(final long idx) {
		return Integer.toString(getInt(idx));
	}
	
	@Override
	public byte getRaw(final int idx) {
		return toRaw(getInt(idx));
	}
	
	@Override
	public byte getRaw(final long idx) {
		return toRaw(getInt(idx));
	}
	
	
	@Override
	public abstract Integer[] toArray();
	
	
	@Override
	public long indexOf(final String character, final long fromIdx) {
		try {
			return indexOf(Integer.parseInt(character), fromIdx);
		}
		catch (final NumberFormatException e) {
			return -1;
		}
	}
	
	
	@Override
	public boolean allEqual(final RStore other) {
		final long length = getLength();
		if (INTEGER != other.getStoreType() || length != other.getLength()) {
			return false;
		}
		if (length < 0) {
			return true;
		}
		else if (length <= Integer.MAX_VALUE) {
			final int ilength = (int) length;
			for (int idx = 0; idx < ilength; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getInt(idx) == other.getInt(idx) )) {
					return false;
				}
			}
		}
		else {
			for (long idx = 0; idx < length; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getInt(idx) == other.getInt(idx) )) {
					return false;
				}
			}
		}
		return true;
	}
	
}
