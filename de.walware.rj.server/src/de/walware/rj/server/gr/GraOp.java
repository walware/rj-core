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

package de.walware.rj.server.gr;


/**
 * Graphic operations
 */
public interface GraOp {
	
	
	// -- C2S sync
	byte OP_CLOSE =                     0x01;
	
	byte OP_REQUEST_RESIZE =            0x08;
	
	byte OP_CONVERT_DEV2USER =          0x10;
	byte OP_CONVERT_USER2DEV =          0x11;
	
}
