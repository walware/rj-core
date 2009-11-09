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

import java.io.IOException;
import java.io.ObjectOutput;


public abstract class MainCmdItem {
	
	
	public static final byte T_NONE = 0;
	
	/**
	 * {@link ConsoleReadCmdItem}
	 *      options = ADD_TO_HISTORY (TRUE, FALSE)
	 *      in = prompt
	 *      answer = (text) readed input
	 */
	public static final byte T_CONSOLE_READ_ITEM =    1;
	
	/**
	 * {@link ConsoleReadCmdItem}
	 *     options = STATUS (OK, WARNING)
	 *     in = output
	 *     answer = -
	 */
	public static final byte T_CONSOLE_WRITE_ITEM =   2;
	
	public static final byte T_MESSAGE_ITEM =         3;
	
	/**
	 * {@link ExtUICmdItem}
	 * Detail depends on the concrete command
	 * {@link ExtUICmdItem#getCommand()}
	 */
	public static final byte T_EXTENDEDUI_ITEM =      5;
	
	/**
	 * Not yet implemented
	 */
	public static final byte T_GRAPH_ITEM =           7;
	
	public static final byte T_S2C_C2S = 9;
	
	public static final byte T_DATA_ITEM =           10;
	
	
	protected static final int OM_STATUS =            0x0f000000; // 0xf << OS_STATUS
	protected static final int OS_STATUS =            24;
	
	protected static final int OM_WITH =              0x70000000;
	
	protected static final int OM_WAITFORCLIENT =     0x80000000;
	protected static final int OV_WAITFORCLIENT =     0x80000000;
	protected static final int OC_WAITFORCLIENT =     ~(OM_WAITFORCLIENT);
	
	private static final int OM_CUSTOM =            0x0000ffff;
	
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
	public abstract void setAnswer(String dataText);
	
	public abstract boolean isOK();
	public abstract RjsStatus getStatus();
	public abstract Object getData();
	public abstract String getDataText();
	
	public abstract void writeExternal(ObjectOutput out) throws IOException;
	
	public abstract boolean testEquals(MainCmdItem other);
	
}
