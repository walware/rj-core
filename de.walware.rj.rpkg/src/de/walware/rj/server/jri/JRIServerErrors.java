/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import java.util.logging.Logger;


public final class JRIServerErrors {
	
	
	public static final int CODE_DATA_COMMON =              0x1000;
	public static final int CODE_DATA_EVAL_DATA =           0x1110;
	public static final int CODE_DATA_RESOLVE_DATA =        0x1120;
	public static final int CODE_DATA_ASSIGN_DATA =         0x1130;
	public static final int CODE_DATA_FIND_DATA =           0x1180;
	
	public static final int CODE_CTRL_COMMON =              0x2000;
	public static final int CODE_CTRL_REQUEST_CANCEL =      0x2110;
	public static final int CODE_CTRL_REQUEST_HOT_MODE =    0x2120;
	
	public static final int CODE_DBG_COMMON =               0x3000;
	public static final int CODE_DBG_CONTEXT =              0x3110;
	public static final int CODE_DBG_DEBUG =                0x3120;
	public static final int CODE_DBG_TRACE =                0x3130;
	
	public static final int CODE_SRV_COMMON =               0x4000;
	public static final int CODE_SRV_EVAL_DATA =            0x4110;
	
	
	public static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.jri");
	
}
