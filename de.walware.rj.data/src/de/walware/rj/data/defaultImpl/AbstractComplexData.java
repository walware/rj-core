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

import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractComplexData extends AbstractRData
		implements RComplexStore {
	
	
	public final byte getStoreType() {
		return RStore.COMPLEX;
	}
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_COMPLEX;
	}
	
	
	@Override
	public final String getChar(final int idx) {
		final StringBuilder sb = new StringBuilder();
		sb.append(getCplxRe(idx));
		sb.append('+');
		sb.append(getCplxIm(idx));
		sb.append('i');
		return sb.toString();
	}
	
}
