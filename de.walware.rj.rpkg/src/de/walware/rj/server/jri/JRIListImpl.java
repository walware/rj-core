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

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RListImpl;


public class JRIListImpl extends RListImpl {
	
	
	public JRIListImpl(final RObject[] initialComponents, final String className1, final String[] initialNames) {
		super(initialComponents, (className1 != null) ? className1 : CLASSNAME_LIST, initialNames, initialComponents.length);
	}
	
	@Override
	protected RCharacterDataImpl createNamesStore(final String[] names) {
		return new JRICharacterDataImpl(names);
	}
	
	public JRIListImpl(final RJIO io, final RObjectFactory factory, final int options) throws IOException {
		super(io, factory, options);
	}
	
	
}
