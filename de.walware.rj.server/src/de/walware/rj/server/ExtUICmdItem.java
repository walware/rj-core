/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * Command for mainloop UI commands.
 */
public final class ExtUICmdItem implements MainCmdItem, Externalizable {
	
	public static final String C_CHOOSE_FILE = "choose_file";
	public static final String C_HISTORY_SAVE = "history.save";
	public static final String C_HISTORY_LOAD = "history.load";
	
	public static final int O_NEW = 8;
	
	private static final int OM_STATUS =            0x0f000000; // 0xf << OS_STATUS
	private static final int OS_STATUS =            24;
	private static final int OM_HASTEXT =           0x10000000;
	private static final int OM_WAITFORCLIENT =     0x20000000;
	private static final int OM_CLEARFORANSWER =    ~(OM_STATUS | OM_HASTEXT);
	private static final int OM_TEXTANSWER =        (V_OK << OS_STATUS) | OM_HASTEXT;
	private static final int OM_CUSTOM =            0x0000ffff;
	
	
	private String command;
	private int options;
	private String data;
	
	
	public ExtUICmdItem() {
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		assert (in.readInt() == T_EXTENDEDUI_ITEM);
		this.command = in.readUTF();
		this.options = in.readInt();
		if ((this.options & OM_HASTEXT) != 0) {
			this.data = in.readUTF();
		}
	}
	
	public ExtUICmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.command = in.readUTF();
		this.options = in.readInt();
		if ((this.options & OM_HASTEXT) != 0) {
			this.data = in.readUTF();
		}
	}
	
	public ExtUICmdItem(final String command, final int options, final boolean waitForClient) {
		this.command = command;
		this.options = (waitForClient) ?
				(options | OM_WAITFORCLIENT) : (options);
	}
	
	public ExtUICmdItem(final String command, final int options, final boolean waitForClient, final String text) {
		this.command = command;
		this.options = (waitForClient) ?
				(options | OM_HASTEXT | OM_WAITFORCLIENT) : (options | OM_HASTEXT);
		this.data = text;
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(T_EXTENDEDUI_ITEM);
		out.writeUTF(this.command);
		out.writeInt(this.options);
		if ((this.options & OM_HASTEXT) != 0) {
			out.writeUTF(this.data);
		}
	}
	
	
	public boolean waitForClient() {
		return ((this.options & OM_WAITFORCLIENT) != 0);
	}
	
	public int getStatus() {
		return ((this.options & OM_STATUS) >> OS_STATUS);
	}
	
	public void setAnswer(final int status) {
		this.data = null;
		this.options = (this.options & OM_CLEARFORANSWER) | (status << OS_STATUS);
	}
	
	public void setAnswer(final String text) {
		this.data = text;
		this.options = (this.options & OM_CLEARFORANSWER) | OM_TEXTANSWER;
	}
	
	
	public int getComType() {
		return RjsComObject.T_EXTENDEDUI_ITEM;
	}
	
	public String getCommand() {
		return this.command;
	}
	
	public int getOption() {
		return (this.options & OM_CUSTOM);
	}
	
	public Object getData() {
		return this.data;
	}
	
	public String getDataText() {
		if (this.data instanceof String) {
			return this.data;
		}
		return null;
	}
	
}
