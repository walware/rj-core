/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
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
	
	
	public final int getStoreType() {
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
	
	
	public Double[] toArray() {
		final Double[] array = new Double[this.length];
		for (int i = 0; i < this.length; i++) {
			if (!isNA(i)) {
				array[i] = Double.valueOf(getNum(i));
			}
		}
		return array;
	}
	
}
