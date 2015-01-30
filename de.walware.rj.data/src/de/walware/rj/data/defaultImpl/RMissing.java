/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
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


/**
 * Default implementation of an R object of type {@link RObject#TYPE_MISSING MISSING}.
 * 
 * @since 0.5.0
 */
public class RMissing implements RObject {
	
	
	public static final RMissing INSTANCE = new RMissing();
	
	
	public RMissing() {
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_MISSING;
	}
	
	@Override
	public String getRClassName() {
		return "<missing>";
	}
	
	/**
	 * Returns the length of the object. The length of {@link RObject#TYPE_MISSING MISSING}
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
		return 68462137;
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (this == obj || (
				(obj instanceof RObject) && ((RObject) obj).getRObjectType() == RObject.TYPE_MISSING) );
	}
	
	@Override
	public String toString() {
		return "RObject type=MISSING";
	}
	
}
