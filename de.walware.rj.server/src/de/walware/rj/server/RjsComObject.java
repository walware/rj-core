/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
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
	 * {@link MainCmdList}
	 */
	public static final int T_MAIN_LIST =            3;
	
	/**
	 * {@link ConsoleCmdItem}
	 *      options = ADD_TO_HISTORY (TRUE, FALSE)
	 *      in = prompt
	 *      answer = (text) readed input
	 */
	public static final int T_CONSOLE_READ_ITEM =    4;
	
	/**
	 * {@link ConsoleCmdItem}
	 *     options = STATUS (OK, WARNING)
	 *     in = output
	 *     answer = -
	 */
	public static final int T_CONSOLE_WRITE_ITEM =   5;
	
	public static final int T_MESSAGE_ITEM =         6;
	
	/**
	 * {@link ExtUICmdItem}
	 * Detail depends on the concrete command
	 * {@link ExtUICmdItem#getCommand()}
	 */
	public static final int T_EXTENDEDUI_ITEM =      7;
	
	/**
	 * Not yet implemented
	 */
	public static final int T_GRAPH_ITEM =           8;
	
	
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
