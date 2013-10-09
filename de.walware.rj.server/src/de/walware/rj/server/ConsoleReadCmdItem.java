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


/**
 * Command for main loop console prompt/input.
 */
public final class ConsoleReadCmdItem extends MainCmdItem {
	
	
	public static final int O_ADD_TO_HISTORY = 0;
	
	
	private static final int OV_WITHTEXT =                  0x01000000;
	
	
	private String text;
	
	
	public ConsoleReadCmdItem(final int options, final String text) {
		assert (text != null);
		this.options = (options | (OV_WITHTEXT | OM_WAITFORCLIENT));
		this.text = text;
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ConsoleReadCmdItem(final RJIO in) throws IOException {
		this.requestId = in.readInt();
		this.options = in.readInt();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = in.readString();
		}
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeInt(this.options);
		if ((this.options & OV_WITHTEXT) != 0) {
			out.writeString(this.text);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_CONSOLE_READ_ITEM;
	}
	
	@Override
	public byte getOp() {
		return 0;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
	}
	
	public void setAnswer(final String text) {
		assert (text != null);
		this.options = (this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHTEXT);
		this.text = text;
	}
	
	
	@Override
	public boolean isOK() {
		return ((this.options & OM_STATUS) == RjsStatus.OK);
	}
	
	@Override
	public RjsStatus getStatus() {
		return null;
	}
	
	@Override
	public String getDataText() {
		return this.text;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof ConsoleReadCmdItem)) {
			return false;
		}
		final ConsoleReadCmdItem otherItem = (ConsoleReadCmdItem) other;
		if (this.options != otherItem.options) {
			return false;
		}
		if (((this.options & OV_WITHTEXT) != 0)
				&& !this.text.equals(otherItem.getDataText())) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(128);
		sb.append("ConsoleReadCmdItem");
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if ((this.options & OV_WITHTEXT) != 0) {
			sb.append("\n<TEXT>\n");
			sb.append(this.text);
			sb.append("\n</TEXT>");
		}
		else {
			sb.append("\n<TEXT/>");
		}
		return sb.toString();
	}
	
}
