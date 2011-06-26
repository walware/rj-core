/*******************************************************************************
 * Copyright (c) 2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.gd;


/**
 * Graphic operations
 */
public interface GraOp {
	
	
	// -- C2S sync
	byte OP_CLOSE =                     0x01;
	
	byte OP_REQUEST_RESIZE =            0x08;
	
}
