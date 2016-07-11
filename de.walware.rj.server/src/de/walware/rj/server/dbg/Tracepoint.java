/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.dbg;


public interface Tracepoint {
	
	/** all breakpoint types */
	byte TYPE_BREAKPOINT=                                   0x0_0000_000F;
	/** function breakpoint */
	byte TYPE_FB=                                           0x0_0000_0001;
	/** line breakpoint */
	byte TYPE_LB=                                           0x0_0000_0002;
	/** toplevel line breakpoint */
	byte TYPE_TB=                                           0x0_0000_0003;
	/** exception breakpoint */
	byte TYPE_EB=                                           0x0_0000_0005;
	
	int TYPE_DELETED=                                       0x0_0100_0000;
	
	
	int getType();
	
}
