/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractIntegerData extends AbstractRData
		implements RIntegerStore {
	
	
	public final byte getStoreType() {
		return RStore.INTEGER;
	}
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_INTEGER;
	}
	
	@Override
	public final double getNum(final int idx) {
		return getInt(idx);
	}
	
	@Override
	public final void setNum(final int idx, final double real) {
		setInt(idx, (int) real);
	}
	
	@Override
	public final String getChar(final int idx) {
		return Integer.toString(getInt(idx));
	}
	
	@Override
	public final boolean getLogi(final int idx) {
		return (getInt(idx) != 0);
	}
	
	@Override
	public final void setLogi(final int idx, final boolean logi) {
		setInt(idx, logi ? 1 : 0);
	}
	
	public Integer[] toArray() {
		final Integer[] array = new Integer[this.length];
		for (int i = 0; i < this.length; i++) {
			if (!isNA(i)) {
				array[i] = Integer.valueOf(getInt(i));
			}
		}
		return array;
	}
	
	public boolean allEqual(final RStore other) {
		if (other.getStoreType() != INTEGER || other.getLength() != this.length) {
			return false;
		}
		for (int i = 0; i < this.length; i++) {
			if (!(other.isNA(i) ? isNA(i) :
					other.getInt(i) == getInt(i) )) {
				return false;
			}
		}
		return true;
	}
	
	
	@Override
	public int indexOf(final String value, final int fromIdx) {
		if (value == null) {
			throw new NullPointerException();
		}
		try {
			return indexOf(Integer.parseInt(value), fromIdx);
		}
		catch (final NumberFormatException e) {
			return -1;
		}
	}
	
}
