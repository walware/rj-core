/*******************************************************************************
 * Copyright (c) 2012-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.RjsStatus;


public final class SrvCmdItem extends MainCmdItem {
	
	
	public static final byte OP_CLEAR_SESSION = 1;
	
	
	private final byte op;
	
	
	public SrvCmdItem(final byte op) {
		this.op = op;
	}
	
	
	@Override
	public byte getCmdType() {
		return T_SRV_ITEM;
	}
	
	@Override
	public byte getOp() {
		return this.op;
	}
	
	@Override
	public void setAnswer(final RjsStatus status) {
	}
	
	@Override
	public boolean isOK() {
		return true;
	}
	
	@Override
	public RjsStatus getStatus() {
		return RjsStatus.OK_STATUS;
	}
	
	@Override
	public String getDataText() {
		return null;
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
	}
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof SrvCmdItem)) {
			return false;
		}
		final SrvCmdItem otherItem = (SrvCmdItem) other;
		if (getOp() != otherItem.getOp()) {
			return false;
		}
		if (this.options != otherItem.options) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("SrvCmdItem ");
		switch (this.op) {
		case OP_CLEAR_SESSION:
			sb.append("CLEAR_SESSION");
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		return sb.toString();
	}
	
}
