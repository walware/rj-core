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

import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractLogicalData extends AbstractRData
		implements RLogicalStore {
	
	
	public final int getStoreType() {
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
	public void setInt(final int idx, final int integer) {
		setLogi(idx, integer != 0);
	}
	
}
