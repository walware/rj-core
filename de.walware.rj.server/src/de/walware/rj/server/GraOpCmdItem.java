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
import de.walware.rj.data.RJIOExternalizable;
import de.walware.rj.server.gd.GraOp;


/**
 * Command item for main loop graphic operations (="op")
 */
public final class GraOpCmdItem extends MainCmdItem implements GraOp {
	
	
	private static final int OV_WITHDATA =                  0x01000000;
	private static final int OV_WITHSTATUS =                0x08000000;
	
	
	private final int devId;
	private final byte op;
	
	private RJIOExternalizable data;
	
	
	/**
	 * Constructor for new commands
	 */
	public GraOpCmdItem(final int devId, final byte op) {
		this.devId = devId;
		this.op = op;
	}
	
	/**
	 * Constructor for new commands
	 */
	public GraOpCmdItem(final int devId, final byte op, final RJIOExternalizable data) {
		this.devId = devId;
		this.op = op;
		if (data != null) {
			this.data = data;
			this.options |= OV_WITHDATA;
		}
	}
	
	/**
	 * Constructor for deserialization
	 */
	public GraOpCmdItem(final RJIO in) throws IOException {
		this.requestId = in.readInt();
		this.options = in.readInt();
		this.devId = in.readInt();
		this.op = in.readByte();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.data = new RjsStatus(in);
		}
		else if ((this.options & OV_WITHDATA) != 0) {
		}
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeInt(this.options);
		out.writeInt(this.devId);
		out.writeByte(this.op);
		if ((this.options & (OV_WITHSTATUS | OV_WITHDATA)) != 0) {
			this.data.writeExternal(out);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_GRAPHICS_OP_ITEM;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		assert (status != null);
		if (status.getSeverity() == RjsStatus.OK) {
			this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.data = null;
		}
		else {
			this.options = (this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS);
			this.data = status;
		}
	}
	
	public void setAnswer(final RJIOExternalizable data) {
		this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
		if (data != null) {
			this.options |= OV_WITHDATA;
		}
		this.data = data;
	}
	
	
	public int getDevId() {
		return this.devId;
	}
	
	@Override
	public byte getOp() {
		return this.op;
	}
	
	@Override
	public boolean isOK() {
		return ((this.options & OV_WITHSTATUS) == 0 || this.data == null);
	}
	
	@Override
	public RjsStatus getStatus() {
		if ((this.options & OV_ANSWER) != 0) {
			return ((this.options & OV_WITHSTATUS) == 0) ? RjsStatus.OK_STATUS : (RjsStatus) this.data;
		}
		return null;
	}
	
	@Override
	public String getDataText() {
		return null;
	}
	
	public Object getData() {
		return this.data;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof GraOpCmdItem)) {
			return false;
		}
		final GraOpCmdItem otherItem = (GraOpCmdItem) other;
		if (this.devId != otherItem.devId) {
			return false;
		}
		if (this.op != otherItem.op) {
			return false;
		}
		if (this.options != otherItem.options) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append("GDOpCmdItem ");
		switch (this.op) {
		case OP_CLOSE:
			sb.append("CLOSE");
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if ((this.options & OV_WITHSTATUS) != 0) {
			sb.append("\nwith status: ");
			sb.append((this.data == null) ? RjsStatus.OK_STATUS : this.data);
		}
		else if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n");
			sb.append(this.data);
			sb.append("\n</DATA>");
		}
		else {
			sb.append("\n<DATA/>");
		}
		return sb.toString();
	}
	
}
