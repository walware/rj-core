/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractNumericData extends AbstractRData
		implements RNumericStore {
	
	
	public final byte getStoreType() {
		return RStore.NUMERIC;
	}
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_NUMERIC;
	}
	
	@Override
	public final int getInt(final int idx) {
		return (int) getNum(idx);
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		setNum(idx, integer);
	}
	
	@Override
	public String getChar(final int idx) {
		return Double.toString(getNum(idx));
	}
	
	
	public Double[] toArray() {
		final Double[] array = new Double[this.length];
		for (int i = 0; i < this.length; i++) {
			if (!isNA(i)) {
				array[i] = Double.valueOf(getNum(i));
			}
		}
		return array;
	}
	
	public boolean allEqual(final RStore other) {
		if (other.getStoreType() != NUMERIC || other.getLength() != this.length) {
			return false;
		}
		for (int i = 0; i < this.length; i++) {
			if (!(other.isNA(i) ? isNA(i) :
					(other.isMissing(i) ? isMissing(i) :
							(Math.abs(other.getNum(i) - getNum(i)) < 2.220446e-16 )) )) {
				return false;
			}
		}
		return true;
	}
	
}
