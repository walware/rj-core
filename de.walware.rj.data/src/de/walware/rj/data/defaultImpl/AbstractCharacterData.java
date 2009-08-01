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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public abstract class AbstractCharacterData extends AbstractRData
		implements RCharacterStore {
	
	
	public final byte getStoreType() {
		return RStore.CHARACTER;
	}
	
	public final String getBaseVectorRClassName() {
		return RObject.CLASSNAME_CHARACTER;
	}
	
	public boolean allEqual(final RStore other) {
		if (other.getStoreType() != CHARACTER || other.getLength() != this.length) {
			return false;
		}
		for (int i = 0; i < this.length; i++) {
			if (!(other.isNA(i) ? isNA(i) :
					other.getChar(i).equals(getChar(i)) )) {
				return false;
			}
		}
		return true;
	}
	
}
