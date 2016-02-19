/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;


public class RNull implements RObject {
	
	
	public static final RNull INSTANCE = new RNull();
	
	
	public RNull() {
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_NULL;
	}
	
	@Override
	public String getRClassName() {
		return CLASSNAME_NULL;
	}
	
	/**
	 * Returns the length of the object. The length of {@link RObject#TYPE_NULL NULL}
	 * is always zero.
	 * 
	 * @return the length
	 */
	@Override
	public long getLength() {
		return 0;
	}
	
	@Override
	public RStore<?> getData() {
		return null;
	}
	
	@Override
	public RList getAttributes() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return 15677;
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (this == obj || (
				(obj instanceof RObject) && ((RObject) obj).getRObjectType() == RObject.TYPE_NULL) );
	}
	
	@Override
	public String toString() {
		return "RObject type=NULL";
	}
	
}
