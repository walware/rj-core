/*******************************************************************************
 * Copyright (c) 2008-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;


/**
 * Interface for all communication exchange objects.
 */
public interface RjsComObject {
	
	/**
	 * {@link RjsStatus}
	 */
	public static final int T_STATUS =               1;
	
	/**
	 * {@link RjsPing}
	 */
	public static final int T_PING =                 2;
	
	/**
	 * {@link MainCmdS2CList}
	 */
	public static final int T_MAIN_LIST =            3;
	
	/**
	 * {@link BinExchange}
	 */
	public static final int T_FILE_EXCHANGE =        4;
	
	/**
	 * {@link CtrlCmdItem}
	 */
	public static final int T_CTRL =                 5;
	
	/**
	 * {@link DbgCmdItem}
	 */
	public static final int T_DBG =                  6;
	
	
	// Same value as in IStatus
	public static final int V_OK =               RjsStatus.OK;
	public static final int V_INFO =             RjsStatus.INFO;
	public static final int V_WARNING =          RjsStatus.WARNING;
	public static final int V_ERROR =            RjsStatus.ERROR;
	public static final int V_CANCEL =           RjsStatus.CANCEL;
	
	public static final int V_FALSE =            0x0;
	public static final int V_TRUE =             0x1;
	
	
	public int getComType();
	
}
