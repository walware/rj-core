/*******************************************************************************
 * Copyright (c) 2008-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
	
	
	private static final int OV_WITHMAP =                   0x01000000;
	private static final int OV_WITHSTATUS =                0x08000000;
	
	
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
	 * Constructor for deserialization
	 */
	public ExtUICmdItem(final RJIO io) throws IOException {
		this.requestId = io.readInt();
		this.command = io.readString();
		this.options = io.readInt();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status = new RjsStatus(io);
			return;
		}
		if ((this.options & OV_WITHMAP) != 0) {
			io.flags = 0;
			this.data = (RList) DataCmdItem.gDefaultFactory.readObject(io);
		}
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeString(this.command);
		out.writeInt(this.options);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(out);
			return;
		}
		if ((this.options & OV_WITHMAP) != 0) {
			out.flags = 0;
			DataCmdItem.gDefaultFactory.writeObject(this.data, out);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_EXTENDEDUI_ITEM;
	}
	
	@Override
	public byte getOp() {
		return 0;
	}
	
	@Override
	public void setAnswer(final RjsStatus status) {
		assert (status != null);
		if (status == RjsStatus.OK_STATUS) {
			this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.status = null;
		}
		else {
			this.options = (this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS);
			this.status = status;
		}
	}
	
	public void setAnswer(final RList answer) {
		this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
		if (answer != null) {
			this.options |= OV_WITHMAP;
		}
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
		sb.append("ExtUICmdItem ");
		sb.append(this.command);
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if ((this.options & OV_WITHMAP) != 0) {
			sb.append("\n<ARGS>\n");
			sb.append(this.data.toString());
			sb.append("\n</ARGS>");
		}
		else {
			sb.append("\n<ARGS/>");
		}
		return sb.toString();
	}
	
}
