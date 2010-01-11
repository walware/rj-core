/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractFactorData extends AbstractRData
		implements RFactorStore {
	
	
	protected boolean isOrdered;
	
	
	public final byte getStoreType() {
		return RStore.FACTOR;
	}
	
	public final String getBaseVectorRClassName() {
		return (this.isOrdered) ? RObject.CLASSNAME_ORDERED : RObject.CLASSNAME_FACTOR;
	}
	
	public final boolean isOrdered() {
		return this.isOrdered;
	}
	
	
	@Override
	public final boolean getLogi(final int idx) {
		return (getInt(idx) != 0);
	}
	
	@Override
	public final void setLogi(final int idx, final boolean logi) {
		setInt(idx, logi ? 1 : 0);
	}
	
	
	public abstract Integer[] toArray();
	
}
