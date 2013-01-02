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

import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractLogicalData extends AbstractRData
		implements RLogicalStore {
	
	
	public final byte getStoreType() {
		return RStore.LOGICAL;
	} 
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_LOGICAL;
	}
	
	@Override
	public final int getInt(final int idx) {
		return getLogi(idx) ? 1 : 0;
	}
	
	@Override
	public final void setInt(final int idx, final int integer) {
		setLogi(idx, integer != 0);
	}
	
	@Override
	public final String getChar(final int idx) {
		return getLogi(idx) ? "TRUE" : "FALSE";
	}
	
	public boolean allEqual(final RStore other) {
		if (other.getStoreType() != LOGICAL || other.getLength() != this.length) {
			return false;
		}
		for (int i = 0; i < this.length; i++) {
			if (!(other.isNA(i) ? isNA(i) :
					other.getLogi(i) == getLogi(i) )) {
				return false;
			}
		}
		return true;
	}
	
}
