/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RObject;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RStore;


public abstract class AbstractRawData extends AbstractRData
		implements RRawStore {
	
	
	private static final char[] HEX_CHARS = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	
	public final byte getStoreType() {
		return RStore.RAW;
	}
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_RAW;
	}
	
	
	@Override
	public int getInt(final int idx) {
		return (getRaw(idx) & 0xff);
	}
	
	@Override
	public final String getChar(final int idx) {
		final byte b = getRaw(idx);
		return new String(new char[] {
				HEX_CHARS[(b & 0xf0) >> 4], HEX_CHARS[(b & 0xf)] });
	}
	
	
	@Override
	public void setInt(final int idx, final int integer) {
		setRaw(idx, ((integer & 0xffffff00) == 0) ? (byte) integer : NA_byte_BYTE);
	}
	
	
	public Byte[] toArray() {
		final Byte[] array = new Byte[this.length];
		for (int i = 0; i < this.length; i++) {
			array[i] = Byte.valueOf(getRaw(i));
		}
		return array;
	}
	
}
