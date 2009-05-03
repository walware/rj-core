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
public abstract class ConsoleCmdItem extends MainCmdItem implements Externalizable {
	
	
	public static final int O_ADD_TO_HISTORY = 0;
	
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OM_TEXTANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHTEXT;
	
	
	public final static class Read extends ConsoleCmdItem {
		
		/**
		 * Constructor for automatic deserialization
		 */
		public Read() {
		}
		
		/**
		 * Constructor for manual deserialization
		 */
		public Read(final ObjectInput in) throws IOException, ClassNotFoundException {
			super(in);
		}
		
		public Read(final int options, final String text) {
			super(options, text, true);
		}
		
		@Override
		public final byte getCmdType() {
			return T_CONSOLE_READ_ITEM;
		}
		
	}
	
	public final static class Write extends ConsoleCmdItem {
		
		/**
		 * Constructor for automatic deserialization
		 */
		public Write() {
		}
		
		/**
		 * Constructor for manual deserialization
		 */
		public Write(final ObjectInput in) throws IOException, ClassNotFoundException {
			super(in);
		}
		
		public Write(final int options, final String text) {
			super(options, text, false);
		}
		
		@Override
		public final byte getCmdType() {
			return T_CONSOLE_WRITE_ITEM;
		}
		
	}
	
	public final static class Message extends ConsoleCmdItem {
		
		/**
		 * Constructor for automatic deserialization
		 */
		public Message() {
		}
		
		/**
		 * Constructor for manual deserialization
		 */
		public Message(final ObjectInput in) throws IOException, ClassNotFoundException {
			super(in);
		}
		
		public Message(final int options, final String text) {
			super(options, text, false);
		}
		
		@Override
		public final byte getCmdType() {
			return T_MESSAGE_ITEM;
		}
		
	}
	
	
	private String text;
	
	
	/**
	 * Constructor for automatic serialization
	 */
	private ConsoleCmdItem() {
	}
	
	/**
	 * Constructor for manual deserialization
	 */
	private ConsoleCmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	private ConsoleCmdItem(final int options, final boolean waitForClient) {
		this.options = (waitForClient) ?
				(options | OM_WAITFORCLIENT) : (options);
	}
	
	private ConsoleCmdItem(final int options, final String text, final boolean waitForClient) {
		assert (text != null);
		this.options = (waitForClient) ?
				(options | (OV_WITHTEXT | OM_WAITFORCLIENT)) : (options | OV_WITHTEXT);
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
		if (!(other instanceof ConsoleCmdItem)) {
			return false;
		}
		final ConsoleCmdItem otherItem = (ConsoleCmdItem) other;
		if (getCmdType() != otherItem.getCmdType()) {
			return false;
		}
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
		switch (getCmdType()) {
		case T_CONSOLE_READ_ITEM:
			sb.append("CONSOLE_READ");
			break;
		case T_CONSOLE_WRITE_ITEM:
			sb.append("CONSOLE_WRITE");
			break;
		case T_MESSAGE_ITEM:
			sb.append("MESSAGE");
			break;
		default:
			sb.append(getCmdType());
			break;
		}
		sb.append(", options=0x");
		sb.append(Integer.toHexString(this.options));
		sb.append(")\n\t");
		sb.append(((this.options & OV_WITHTEXT) != 0) ? this.text : "<no text>");
		return sb.toString();
	}
	
}
