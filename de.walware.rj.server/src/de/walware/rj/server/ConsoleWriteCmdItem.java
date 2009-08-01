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
 * Command for main loop console output.
 */
public final class ConsoleWriteCmdItem extends MainCmdItem implements Externalizable {
	
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OM_TEXTANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHTEXT;
	
	
	private String text;
	
	
	/**
	 * Constructor for automatic deserialization
	 */
	
	public ConsoleWriteCmdItem() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ConsoleWriteCmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public ConsoleWriteCmdItem(final int options, final String text) {
		assert (text != null);
		this.options = (options | OV_WITHTEXT);
		this.text = text;
	}
	
	@Override
	public final void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.options);
		if ((this.options & OV_WITHTEXT) != 0) {
			out.writeUTF(this.text);
		}
	}
	
	public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.options = in.readInt();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = in.readUTF();
		}
	}
	
	
	@Override
	public final byte getCmdType() {
		return T_CONSOLE_WRITE_ITEM;
	}
	
	
	@Override
	public final void setAnswer(final int status) {
		this.options = (this.options & OM_CLEARFORANSWER) | (status << OS_STATUS);
	}
	
	@Override
	public final void setAnswer(final String text) {
		assert (text != null);
		this.options = (this.options & OM_CLEARFORANSWER) | OM_TEXTANSWER;
		this.text = text;
	}
	
	
	@Override
	public final Object getData() {
		return this.text;
	}
	
	@Override
	public final String getDataText() {
		return this.text;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof ConsoleWriteCmdItem)) {
			return false;
		}
		final ConsoleWriteCmdItem otherItem = (ConsoleWriteCmdItem) other;
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
	public final String toString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("ConsoleCmdItem (type=");
		sb.append("CONSOLE_WRITE");
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
