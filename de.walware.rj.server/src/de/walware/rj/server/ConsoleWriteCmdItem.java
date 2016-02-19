/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.io.IOException;

import de.walware.rj.data.RJIO;


/**
 * Command for main loop console output.
 */
public final class ConsoleWriteCmdItem extends MainCmdItem {
	
	
	public static final byte R_OUTPUT=                      1;
	public static final byte R_ERROR=                       2;
	public static final byte SYS_OUTPUT=                    5;
	
	
	private final byte streamId;
	
	private final String text;
	
	
	public ConsoleWriteCmdItem(final byte streamId, final String text) {
		assert (text != null);
		this.streamId= streamId;
		this.text= text;
	}
	
	/**
	 * Constructor for deserialization
	 */
	public ConsoleWriteCmdItem(final RJIO in) throws IOException {
		this.streamId= in.readByte();
		this.text= in.readString();
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeByte(this.streamId);
		out.writeString(this.text);
	}
	
	
	@Override
	public byte getCmdType() {
		return T_CONSOLE_WRITE_ITEM;
	}
	
	@Override
	public byte getOp() {
		return this.streamId;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		throw new UnsupportedOperationException();
	}
	
	
	@Override
	public boolean isOK() {
		return true;
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
		if (!(other instanceof ConsoleWriteCmdItem)) {
			return false;
		}
		final ConsoleWriteCmdItem otherItem= (ConsoleWriteCmdItem) other;
		return (this.options == otherItem.options
				&& this.streamId == otherItem.streamId
				&& this.text.equals(otherItem.getDataText()) );
	}
	
	@Override
	public String toString() {
		final StringBuilder sb= new StringBuilder(128);
		sb.append("ConsoleWriteCmdItem (").append(this.streamId).append(")");
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		sb.append("\n<TEXT>\n");
		sb.append(this.text);
		sb.append("\n</TEXT>");
		return sb.toString();
	}
	
}
