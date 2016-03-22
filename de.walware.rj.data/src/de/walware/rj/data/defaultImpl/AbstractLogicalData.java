/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractLogicalData extends AbstractRData<Boolean>
		implements RLogicalStore {
	
	
	protected static final String toChar(final boolean logi) {
		return (logi) ? "TRUE" : "FALSE";
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.LOGICAL;
	} 
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_LOGICAL;
	}
	
	
	@Override
	public final int getInt(final int idx) {
		return getLogi(idx) ? 1 : 0;
	}
	
	@Override
	public final int getInt(final long idx) {
		return getLogi(idx) ? 1 : 0;
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		setLogi(idx, integer != 0);
	}
	
	@Override
	public final void setInt(final long idx, final int integer) {
		setLogi(idx, integer != 0);
	}
	
	@Override
	public final String getChar(final int idx) {
		return toChar(getLogi(idx));
	}
	
	@Override
	public final String getChar(final long idx) {
		return toChar(getLogi(idx));
	}
	
	@Override
	public void setChar(final int idx, final String character) {
		setLogi(idx, AbstractCharacterData.toLogi(character));
	}
	
	@Override
	public void setChar(final long idx, final String character) {
		setLogi(idx, AbstractCharacterData.toLogi(character));
	}
	
	@Override
	public long indexOf(final String character, final long fromIdx) {
		if (character == null) {
			throw new NullPointerException();
		}
		try {
			return indexOf(AbstractCharacterData.toLogi(character) ? 1 : 0, fromIdx);
		}
		catch (final NumberFormatException e) {
			return -1;
		}
	}
	
	@Override
	public byte getRaw(final int idx) {
		return getLogi(idx) ? (byte) 1 : (byte) 0;
	}
	
	@Override
	public byte getRaw(final long idx) {
		return getLogi(idx) ? (byte) 1 : (byte) 0;
	}
	
	@Override
	public void setRaw(final int idx, final byte raw) {
		setLogi(idx, raw != 0);
	}
	
	@Override
	public void setRaw(final long idx, final byte raw) {
		setLogi(idx, raw != 0);
	}
	
	
	@Override
	public abstract Boolean[] toArray();
	
	
	@Override
	public boolean allEqual(final RStore<?> other) {
		final long length = getLength();
		if (LOGICAL != other.getStoreType() || length != other.getLength()) {
			return false;
		}
		if (length < 0) {
			return true;
		}
		else if (length <= Integer.MAX_VALUE) {
			final int ilength = (int) length;
			for (int idx = 0; idx < ilength; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getLogi(idx) == other.getLogi(idx) )) {
					return false;
				}
			}
		}
		else {
			for (long idx = 0; idx < length; idx++) {
				if (!(isNA(idx) ? other.isNA(idx) :
						getLogi(idx) == other.getLogi(idx) )) {
					return false;
				}
			}
		}
		return true;
	}
	
}
