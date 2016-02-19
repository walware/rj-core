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

package de.walware.rj.data;


public interface RFunction extends RObject {
	
//	RArgument getArgument(final int idx);
//	RArgument getArgument(final String name);
//	int getArgumentIdx(final String name);
//	Iterator<RArgument> getArgumentIterator();
	
	// source based
	String getHeaderSource();
	String getBodySource();
	
}
