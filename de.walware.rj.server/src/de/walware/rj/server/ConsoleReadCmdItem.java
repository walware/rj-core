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
 * Command for main loop console prompt/input.
 */
public final class ConsoleReadCmdItem extends MainCmdItem implements Externalizable {
	
	
	public static final int O_ADD_TO_HISTORY = 0;
	
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OM_TEXTANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHTEXT;
	
	
	private String text;
	
	
	/**
	 * Constructor for automatic deserialization
	 */
	public ConsoleReadCmdItem() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ConsoleReadCmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public ConsoleReadCmdItem(final int options, final String text) {
		assert (text != null);
		this.options = (options | (OV_WITHTEXT | OM_WAITFORCLIENT));
		this.text = text;
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.options);
		out.writeByte(this.requestId);
		if ((this.options & OV_WITHTEXT) != 0) {
			out.writeUTF(this.text);
		}
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.options = in.readInt();
		this.requestId = in.readByte();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = in.readUTF();
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
	
	@Override
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
	public Object getData() {
		return this.text;
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
