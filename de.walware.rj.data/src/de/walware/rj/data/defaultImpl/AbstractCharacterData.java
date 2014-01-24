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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractCharacterData extends AbstractRData
		implements RCharacterStore {
	
	
	protected static final boolean toLogi(final String character) {
		switch (character.length()) {
		case 1:
			switch (character.charAt(0)) {
			case 'F':
				return false;
			case 'T':
				return true;
			default:
				break;
			}
			break;
		case 4:
			if ("true".regionMatches(0, character, 0, 4)) {
				return true;
			}
			break;
		case 5:
			if ("false".regionMatches(0, character, 0, 5)) {
				return false;
			}
			break;
		default:
			break;
		}
		throw new NumberFormatException(character);
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.CHARACTER;
	}
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_CHARACTER;
	}
	
	
	@Override
	public void setLogi(final int idx, final boolean logi) {
		setChar(idx, AbstractLogicalData.toChar(logi));
	}
	
	@Override
	public void setLogi(final long idx, final boolean logi) {
		setChar(idx, AbstractLogicalData.toChar(logi));
	}
	
	@Override
	public boolean getLogi(final int idx) {
		return toLogi(getChar(idx));
	}
	
	@Override
	public boolean getLogi(final long idx) {
		return toLogi(getChar(idx));
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		setChar(idx, Integer.toString(integer));
	}
	
	@Override
	public final void setInt(final long idx, final int integer) {
		setChar(idx, Integer.toString(integer));
	}
	
	@Override
	public void setCplx(final int idx, final double real, final double imaginary) {
		setChar(idx, AbstractComplexData.toChar(real, imaginary));
	}
	
	@Override
	public void setCplx(final long idx, final double real, final double imaginary) {
		setChar(idx, AbstractComplexData.toChar(real, imaginary));
	}
	
	@Override
	public final void setRaw(final int idx, final byte raw) {
		setChar(idx, AbstractRawData.toChar(raw));
	}
	
	@Override
	public final void setRaw(final long idx, final byte raw) {
		setChar(idx, AbstractRawData.toChar(raw));
	}
	
	@Override
	public long indexOf(final int integer, final long fromIdx) {
		return indexOf(Integer.toString(integer), fromIdx);
	}
	
	
	@Override
	public abstract String[] toArray();
	
	
	@Override
	public boolean allEqual(final RStore other) {
		final long length = getLength();
		if (CHARACTER != other.getStoreType() || length != other.getLength()) {
			return false;
		}
		if (length < 0) {
			return true;
		}
		else if (length <= Integer.MAX_VALUE) {
			final int ilength = (int) length;
			for (int idx = 0; idx < ilength; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getChar(idx).equals(other.getChar(idx)) )) {
					return false;
				}
			}
		}
		else {
			for (long idx = 0; idx < length; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getChar(idx).equals(other.getChar(idx)) )) {
					return false;
				}
			}
		}
		return true;
	}
	
}
