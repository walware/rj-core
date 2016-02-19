/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.io.IOException;

import de.walware.rj.data.RJIO;


/**
 * Command for main loop console prompt/input.
 */
public final class MainCtrlCmdItem extends MainCmdItem {
	
	
	public static final byte OP_FINISH_TASK= 02;
	
	
	private final byte op;
	
	
	public MainCtrlCmdItem(final byte op, final int options) {
		this.op= op;
		this.options= (options | (OM_WAITFORCLIENT));
	}
	
	/**
	 * Constructor for deserialization
	 */
	public MainCtrlCmdItem(final RJIO in) throws IOException {
		this.requestId= in.readInt();
		this.options= in.readInt();
		this.op= in.readByte();
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeInt(this.options);
		out.writeByte(this.op);
	}
	
	
	@Override
	public byte getCmdType() {
		return T_MAIN_CTRL_ITEM;
	}
	
	@Override
	public byte getOp() {
		return this.op;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
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
		return null;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof MainCtrlCmdItem)) {
			return false;
		}
		final MainCtrlCmdItem otherItem = (MainCtrlCmdItem) other;
		return (getOp() == otherItem.getOp()
				&& this.options == otherItem.options );
	}
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(128);
		sb.append("MainCtrlCmdItem ");
		switch (this.op) {
		case OP_FINISH_TASK:
			sb.append("FINISH_TASK");
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		return sb.toString();
	}
	
}
