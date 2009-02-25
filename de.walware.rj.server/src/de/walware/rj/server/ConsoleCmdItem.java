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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * Command for mainloop console commands.
 */
public final class ConsoleCmdItem implements MainCmdItem, Externalizable {
	
	
	public static final int O_ADD_TO_HISTORY = 0;
	
	
	private static final int OM_STATUS =            0x0f000000; // 0xf << OS_STATUS
	private static final int OS_STATUS =            24;
	private static final int OM_HASTEXT =           0x10000000;
	private static final int OM_WAITFORCLIENT =     0x20000000;
	private static final int OM_CLEARFORANSWER =    ~(OM_STATUS | OM_HASTEXT);
	private static final int OM_TEXTANSWER =        (V_OK << OS_STATUS) | OM_HASTEXT;
	private static final int OM_CUSTOM =            0x0000ffff;
	
	
	private int cmdType;
	private int options;
	private String text;
	
	
	public ConsoleCmdItem() {
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.cmdType = in.readInt();
		this.options = in.readInt();
		if ((this.options & OM_HASTEXT) != 0) {
			this.text = in.readUTF();
		}
	}
	
	public ConsoleCmdItem(final int cmdType, final ObjectInput in) throws IOException, ClassNotFoundException {
		this.cmdType = cmdType;
		this.options = in.readInt();
		if ((this.options & OM_HASTEXT) != 0) {
			this.text = in.readUTF();
		}
	}
	
	public ConsoleCmdItem(final int cmdType, final int options, final boolean waitForClient) {
		this.cmdType = cmdType;
		this.options = (waitForClient) ?
				(options | OM_WAITFORCLIENT) : (options);
	}
	
	public ConsoleCmdItem(final int cmdType, final int options, final boolean waitForClient, final String text) {
		this.cmdType = cmdType;
		this.options = (waitForClient) ?
				(options | OM_HASTEXT | OM_WAITFORCLIENT) : (options | OM_HASTEXT);
		this.text = text;
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.cmdType);
		out.writeInt(this.options);
		if ((this.options & OM_HASTEXT) != 0) {
			out.writeUTF(this.text);
		}
	}
	
	
	public boolean waitForClient() {
		return ((this.options & OM_WAITFORCLIENT) != 0);
	}
	
	public int getStatus() {
		return ((this.options & OM_STATUS) >> OS_STATUS);
	}
	
	public void setAnswer(final int status) {
		this.text = null;
		this.options = (this.options & OM_CLEARFORANSWER) | (status << OS_STATUS);
	}
	
	public void setAnswer(final String text) {
		this.text = text;
		this.options = (this.options & OM_CLEARFORANSWER) | OM_TEXTANSWER;
	}
	
	
	public int getComType() {
		return this.cmdType;
	}
	
	public int getOption() {
		return (this.options & OM_CUSTOM);
	}
	
	public Object getData() {
		return this.text;
	}
	
	public String getDataText() {
		return this.text;
	}
	
}
