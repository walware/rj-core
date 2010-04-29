/*******************************************************************************
 * Copyright (c) 2008-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.IOException;

import de.walware.rj.data.RJIO;


public abstract class MainCmdItem {
	
	
	public static final byte T_NONE = 0;
	
	/**
	 * {@link ConsoleReadCmdItem}
	 * S2C
	 *      options = ADD_TO_HISTORY (TRUE, FALSE)
	 *      text = prompt
	 * C2S
	 *      text = input
	 */
	public static final byte T_CONSOLE_READ_ITEM =    1;
	
	/**
	 * {@link ConsoleWriteOutCmdItem}
	 * S2C
	 *     text = output
	 * C2S
	 *     -
	 */
	public static final byte T_CONSOLE_WRITE_OUT_ITEM =   2;
	
	/**
	 * {@link ConsoleWriteErrCmdItem}
	 * S2C
	 *     text = output
	 * C2S
	 *     -
	 */
	public static final byte T_CONSOLE_WRITE_ERR_ITEM =   3;
	
	/**
	 * {@link ConsoleMessageCmdItem}
	 * S2C
	 *     text = message
	 * C2S
	 *     -
	 */
	public static final byte T_MESSAGE_ITEM =         4;
	
	/**
	 * {@link ExtUICmdItem}
	 * Detail depends on the concrete command {@link ExtUICmdItem#getCommand()}
	 */
	public static final byte T_EXTENDEDUI_ITEM =      5;
	
	/**
	 * {@link GDCmdItem}
	 */
	public static final byte T_GRAPH_ITEM =           7;
	
	/**
	 * T_id <  => initiated by server
	 * T_id >  => initiated by client
	 */
	public static final byte T_S2C_C2S = 9;
	
	/**
	 * {@link DataCmdItem}
	 */
	public static final byte T_DATA_ITEM =           10;
	
	
	protected static final int OM_STATUS =            0x0f000000; // 0xf << OS_STATUS
	protected static final int OS_STATUS =            24;
	
	protected static final int OM_WITH =              0x70000000;
	
	protected static final int OM_WAITFORCLIENT =     0x80000000;
	public static final int OV_WAITFORCLIENT =     0x80000000;
	protected static final int OC_WAITFORCLIENT =     ~(OM_WAITFORCLIENT);
	
	public static final int OM_CUSTOM =            0x0000ffff;
	
	protected static final int OM_CLEARFORANSWER =    ~(OM_STATUS | OM_WITH);
	
	
	protected int options;
	
	public MainCmdItem next;
	
	public byte requestId = -1;
	public byte slot = 0;
	
	
	public abstract byte getCmdType();
	
	public final boolean waitForClient() {
		return ((this.options & OM_WAITFORCLIENT) != 0);
	}
	
	public final int getCmdOption() {
		return ((this.options & OM_CUSTOM));
	}
	
	public abstract void setAnswer(RjsStatus status);
	
	public abstract boolean isOK();
	public abstract RjsStatus getStatus();
	public abstract String getDataText();
	
	public abstract void writeExternal(RJIO io) throws IOException;
	
	public abstract boolean testEquals(MainCmdItem other);
	
}
