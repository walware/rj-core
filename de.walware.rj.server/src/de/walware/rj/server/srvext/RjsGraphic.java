/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvext;


public interface RjsGraphic {
	
	
	int STATE_CLOSED = -1;
	int STATE_OPENED = 1;
	int STATE_PAGED = 2;
	
	
	int getDevId();
	
	byte getSlot();
	
	int getState();
	
}
