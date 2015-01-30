/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.dbg;


public interface Tracepoint {
	
	/** all breakpoint types */
	int TYPE_BREAKPOINT=                                   0x00000003;
	/** function breakpoint */
	int TYPE_FB=                                           0x00000001;
	/** line breakpoint */
	int TYPE_LB=                                           0x00000002;
	
	int TYPE_DELETED=                                      0x01000000;
	
	
	int getType();
	
}
