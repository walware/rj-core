/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import de.walware.rj.data.defaultImpl.RListFixLongImpl;


public class JRIListLongImpl extends RListFixLongImpl {
	
	
	public JRIListLongImpl(final long length, final String className1) {
		super(length, (className1 != null) ? className1 : CLASSNAME_LIST);
	}
	
	
}
