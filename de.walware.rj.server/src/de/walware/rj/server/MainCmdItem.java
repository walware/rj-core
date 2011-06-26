/*******************************************************************************
 * Copyright (c) 2008-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
import de.walware.rj.data.RJIOExternalizable;


public abstract class MainCmdItem implements RJIOExternalizable {
	
	
	public static final byte T_NONE = 0;
	
	/**
	 * {@link ConsoleReadCmdItem}
	 */
	public static final byte T_CONSOLE_READ_ITEM =          0x01;
	
	/**
	 * {@link ConsoleWriteOutCmdItem}
	 */
	public static final byte T_CONSOLE_WRITE_OUT_ITEM =     0x02;
	
	/**
	 * {@link ConsoleWriteErrCmdItem}
	 */
	public static final byte T_CONSOLE_WRITE_ERR_ITEM =     0x03;
	
	/**
	 * {@link ConsoleMessageCmdItem}
	 */
	public static final byte T_MESSAGE_ITEM =               0x04;
	
	/**
	 * {@link ExtUICmdItem}
	 */
	public static final byte T_EXTENDEDUI_ITEM =            0x05;
	
	/**
	 * {@link GDCmdItem}
	 */
	public static final byte T_GRAPH_ITEM =                 0x07;
	
	/**
	 * T_id <  => initiated by server
	 * T_id >  => initiated by client
	 */
	public static final byte T_S2C_C2S = 9;
	
	/**
	 * {@link DataCmdItem}
	 */
	public static final byte T_DATA_ITEM =                  0x10;
	
	/**
	 * {@link GraOpCmdItem}
	 */
	public static final byte T_GRAPHICS_OP_ITEM =           0x12;
	
	
	protected static final int OM_STATUS =                  0x00f00000; // 0xf << OS_STATUS
	protected static final int OS_STATUS =                  20;
	
	protected static final int OM_WITH =                    0x0f000000;
	
	public static final int OV_ANSWER =                     0x40000000;
	protected static final int OM_ANSWER =                  OV_ANSWER;
	public static final int OV_WAITFORCLIENT =              0x80000000;
	protected static final int OM_WAITFORCLIENT =           OV_WAITFORCLIENT;
	protected static final int OC_WAITFORCLIENT =           ~(OM_WAITFORCLIENT);
	
	public static final int OM_CUSTOM =                     0x0000ffff;
	
	protected static final int OM_CLEARFORANSWER =          ~(OM_STATUS | OM_WITH);
	
	
	protected int options;
	
	public MainCmdItem next;
	
	public int requestId;
	public byte slot = 0;
	
	
	public abstract byte getCmdType();
	
	public abstract byte getOp();
	
	public final boolean waitForClient() {
		return ((this.options & OM_WAITFORCLIENT) != 0);
	}
	
	public final boolean isAnswer() {
		return ((this.options & OM_ANSWER) != 0);
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
