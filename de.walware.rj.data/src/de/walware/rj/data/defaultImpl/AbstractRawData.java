/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RObject;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RStore;


public abstract class AbstractRawData extends AbstractRData<Byte>
		implements RRawStore {
	
	
	private static final char[] HEX_CHARS = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	protected static final String toChar(final byte raw) {
		return new String(new char[] { HEX_CHARS[(raw & 0xf0) >> 4], HEX_CHARS[(raw & 0x0f)] });
	}
	
	
	@Override
	public final byte getStoreType() {
		return RStore.RAW;
	}
	
	@Override
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_RAW;
	}
	
	
	@Override
	public final boolean isNA(final int idx) {
		return false;
	}
	
	@Override
	public final boolean isNA(final long idx) {
		return false;
	}
	
	@Override
	public final boolean isMissing(final int idx) {
		return false;
	}
	
	@Override
	public final boolean isMissing(final long idx) {
		return false;
	}
	
	@Override
	public int getInt(final int idx) {
		return (getRaw(idx) & 0xff);
	}
	
	@Override
	public int getInt(final long idx) {
		return (getRaw(idx) & 0xff);
	}
	
	@Override
	public void setInt(final long idx, final int integer) {
		setRaw(idx, AbstractIntegerData.toRaw(integer));
	}
	
	@Override
	public void setInt(final int idx, final int integer) {
		setRaw(idx, AbstractIntegerData.toRaw(integer));
	}
	
	@Override
	public final String getChar(final int idx) {
		return toChar(getRaw(idx));
	}
	
	@Override
	public final String getChar(final long idx) {
		return toChar(getRaw(idx));
	}
	
}
