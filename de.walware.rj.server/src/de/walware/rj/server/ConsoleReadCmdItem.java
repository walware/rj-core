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


/**
 * Command for main loop console prompt/input.
 */
public final class ConsoleReadCmdItem extends MainCmdItem {
	
	
	public static final int O_ADD_TO_HISTORY = 0;
	
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OM_TEXTANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHTEXT;
	
	
	private String text;
	
	
	public ConsoleReadCmdItem(final int options, final String text) {
		assert (text != null);
		this.options = (options | (OV_WITHTEXT | OM_WAITFORCLIENT));
		this.text = text;
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ConsoleReadCmdItem(final RJIO io) throws IOException {
		this.options = io.in.readInt();
		this.requestId = io.in.readByte();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = io.readString();
		}
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.out.writeInt(this.options);
		io.out.writeByte(this.requestId);
		if ((this.options & OV_WITHTEXT) != 0) {
			io.writeString(this.text);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_CONSOLE_READ_ITEM;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
	}
	
	public void setAnswer(final String text) {
		assert (text != null);
		this.options = (this.options & OM_CLEARFORANSWER) | OM_TEXTANSWER;
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
		final StringBuffer sb = new StringBuffer(100);
		sb.append("ConsoleCmdItem (type=");
		sb.append("CONSOLE_READ");
		sb.append(", options=0x");
		sb.append(Integer.toHexString(this.options));
		sb.append(")");
		if ((this.options & OV_WITHTEXT) != 0) {
			sb.append("\n<TEXT>\n");
			sb.append(this.text);
			sb.append("\n</TEXT>");
		}
		else {
			sb.append("\n<TEXT />");
		}
		return sb.toString();
	}
	
}
