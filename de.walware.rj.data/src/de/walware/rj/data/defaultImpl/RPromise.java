/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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


public class RPromise implements RObject {
	
	
	public static final RPromise INSTANCE = new RPromise();
	
	
	public RPromise() {
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_PROMISE;
	}
	
	@Override
	public String getRClassName() {
		return "<promise>";
	}
	
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
	
}
