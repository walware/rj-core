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
import de.walware.rj.data.RList;


/**
 * Command item for main loop UI interaction.
 */
public final class ExtUICmdItem extends MainCmdItem {
	
	public static final String C_CHOOSE_FILE = "chooseFile";
	public static final String C_LOAD_HISTORY = "loadHistory";
	public static final String C_SAVE_HISTORY = "saveHistory";
	public static final String C_SHOW_HISTORY = "showHistory";
	public static final String C_ADDTO_HISTORY = "addtoHistory";
	
	private static final int OV_WITHMAP =          0x10000000;
	private static final int OV_WITHSTATUS =        0x40000000;
	private static final int OM_MAPANSWER =        OV_WITHMAP;
	private static final int OM_STATUSANSWER =      OV_WITHSTATUS;
	
	
	private String command;
	private RList data;
	
	private RjsStatus status;
	
	
	public ExtUICmdItem(final String command, final int options, final RList args, final boolean waitForClient) {
		assert (command != null);
		this.command = command;
		this.options = (waitForClient) ?
				(options | OM_WAITFORCLIENT) : (options);
		if (args != null) {
			this.options |= OV_WITHMAP;
			this.data = args;
		}
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public ExtUICmdItem() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ExtUICmdItem(final RJIO io) throws IOException {
		this.command = io.readString();
		this.options = io.in.readInt();
		this.requestId = io.in.readByte();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status = new RjsStatus(io.in);
			return;
		}
		if ((this.options & OV_WITHMAP) != 0) {
			io.flags = 0;
			this.data = (RList) DataCmdItem.gDefaultFactory.readObject(io);
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeString(this.command);
		io.out.writeInt(this.options);
		io.out.writeByte(this.requestId);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(io.out);
			return;
		}
		if ((this.options & OV_WITHMAP) != 0) {
			io.flags = 0;
			DataCmdItem.gDefaultFactory.writeObject(this.data, io);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_EXTENDEDUI_ITEM;
	}
	
	@Override
	public void setAnswer(final RjsStatus status) {
		if (status == RjsStatus.OK_STATUS) {
			this.options = (this.options & OM_CLEARFORANSWER);
			this.status = null;
		}
		else {
			this.options = ((this.options & OM_CLEARFORANSWER) | OM_STATUSANSWER);
			this.status = status;
		}
	}
	
	public void setAnswer(final RList answer) {
		this.options = (answer != null) ? 
				((this.options & OM_CLEARFORANSWER) | OM_MAPANSWER) : (this.options & OM_CLEARFORANSWER);
		this.status = null;
		this.data = answer;
	}
	
	
	@Override
	public boolean isOK() {
		return (this.status == null || this.status.getSeverity() == RjsStatus.OK);
	}
	
	@Override
	public RjsStatus getStatus() {
		return this.status;
	}
	
	@Override
	public String getDataText() {
		return this.command;
	}
	
	public RList getDataArgs() {
		return this.data;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof ExtUICmdItem)) {
			return false;
		}
		final ExtUICmdItem otherItem = (ExtUICmdItem) other;
		if (!getDataText().equals(otherItem.getDataText())) {
			return false;
		}
		if (this.options != otherItem.options) {
			return false;
		}
		if (((this.options & OV_WITHMAP) != 0)
				&& !getDataArgs().equals(otherItem.getDataArgs())) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("ExtUICmdItem (command=");
		sb.append(this.command);
		sb.append(", options=0x");
		sb.append(Integer.toHexString(this.options));
		sb.append(")");
		if ((this.options & OV_WITHMAP) != 0) {
			sb.append("\n<ARGS>\n");
			sb.append(this.data.toString());
			sb.append("\n</ARGS>");
		}
		else {
			sb.append("\n<TEXT />");
		}
		return sb.toString();
	}
	
}
