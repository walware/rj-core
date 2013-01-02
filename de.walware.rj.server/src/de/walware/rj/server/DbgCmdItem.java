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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;
import de.walware.rj.server.dbg.CallStack;
import de.walware.rj.server.dbg.DbgEnablement;
import de.walware.rj.server.dbg.DbgFilterState;
import de.walware.rj.server.dbg.ElementTracepointInstallationReport;
import de.walware.rj.server.dbg.ElementTracepointInstallationRequest;
import de.walware.rj.server.dbg.FrameContext;
import de.walware.rj.server.dbg.FrameContextDetailRequest;
import de.walware.rj.server.dbg.SetDebugReport;
import de.walware.rj.server.dbg.SetDebugRequest;
import de.walware.rj.server.dbg.TracepointEvent;
import de.walware.rj.server.dbg.TracepointStatesUpdate;


/**
 * Command item for main loop dbg operations (="op")
 */
public final class DbgCmdItem extends MainCmdItem implements RjsComObject, Externalizable {
	
	// -- C2S sync
	public static final byte OP_LOAD_FRAME_LIST =           0x01;
	public static final byte OP_LOAD_FRAME_CONTEXT =        0x02;
	public static final byte OP_SET_DEBUG =                 0x03;
	public static final byte OP_REQUEST_SUSPEND =           0x04;
	public static final byte OP_INSTALL_TP_POSITIONS =      0x05;
	
	// -- C2S sync + async
	public static final byte OP_SET_ENABLEMENT =            0x08;
	public static final byte OP_RESET_FILTER_STATE =        0x09;
	public static final byte OP_UPDATE_TP_STATES =          0x0A;
	
	public static final byte OP_C2S_S2C =                   0x10;
	
	public static final byte OP_NOTIFY_TP_EVENT =           0x11;
	
	
	private static final int OV_WITHDATA =                  0x01000000;
	private static final int OV_WITHSTATUS =                0x08000000;
	
	
	public static interface CustomDataReader {
		
		Object read(byte type, boolean answer, RJIO io) throws IOException;
		
	}
	
	private static Object readData(final RJIO io, final byte type, final boolean answer) throws IOException {
		if (!answer) {
			switch (type) {
			case OP_LOAD_FRAME_LIST:
				return null;
			case OP_LOAD_FRAME_CONTEXT:
				return new FrameContextDetailRequest(io);
			case OP_SET_DEBUG:
				return new SetDebugRequest(io);
			case OP_REQUEST_SUSPEND:
				return null;
			case OP_SET_ENABLEMENT:
				return new DbgEnablement(io);
			case OP_RESET_FILTER_STATE:
				return new DbgFilterState(io);
			case OP_INSTALL_TP_POSITIONS:
				return new ElementTracepointInstallationRequest(io);
			case OP_UPDATE_TP_STATES:
				return new TracepointStatesUpdate(io);
			case OP_NOTIFY_TP_EVENT:
				return new TracepointEvent(io);
			default:
				break;
			}
			throw new IOException("Unsupported DbgCmdType: " + type);
		}
		else {
			switch (type) {
			case OP_LOAD_FRAME_LIST:
				return new CallStack(io);
			case OP_LOAD_FRAME_CONTEXT:
				return new FrameContext(io);
			case OP_SET_DEBUG:
				return new SetDebugReport(io);
			case OP_REQUEST_SUSPEND:
				break; // status
			case OP_SET_ENABLEMENT:
			case OP_RESET_FILTER_STATE:
				break; // status
			case OP_INSTALL_TP_POSITIONS:
				return new ElementTracepointInstallationReport(io);
			case OP_UPDATE_TP_STATES:
				break; // status
			default:
				break;
			}
			throw new IOException("Unsupported DbgCmdType: " + type);
		}
	}
	
	
	private byte op;
	
	private Object data;
	
	private RjsStatus status;
	
	
	/**
	 * Constructor for new commands
	 */
	public DbgCmdItem(final byte op, final int options,
			final RJIOExternalizable data) {
		this.op = op;
		this.options = (options & OM_CUSTOM);
		if (data != null) {
			this.options |= OV_WITHDATA;
		}
		if (op < OP_NOTIFY_TP_EVENT) {
			this.options |= OV_WAITFORCLIENT;
		}
		this.data = data;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public DbgCmdItem() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public DbgCmdItem(final RJIO io) throws IOException {
		readExternal(io, null);
	}
	
	/**
	 * Constructor for deserialization
	 */
	public DbgCmdItem(final RJIO io, final CustomDataReader reader) throws IOException {
		readExternal(io, reader);
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.options);
		io.writeByte(this.op);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(io);
		}
		else if ((this.options & OV_WITHDATA) != 0) {
			((RJIOExternalizable) this.data).writeExternal(io);
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		final RJIO io = RJIO.get(out);
		final int check = io.writeCheck1();
		writeExternal(io);
		io.writeCheck2(check);
		io.disconnect(out);
	}
	
	private void readExternal(final RJIO io, final CustomDataReader reader) throws IOException {
		this.options = io.readInt();
		this.op = io.readByte();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status = new RjsStatus(io);
		}
		else if ((this.options & OV_WITHDATA) != 0) {
			final boolean answer = ((this.options & OV_ANSWER) != 0);
			if (reader != null) {
				this.data = reader.read(this.op, answer, io);
				if (this.data == null) {
					this.data = readData(io, this.op, answer);
				}
			}
			else {
				this.data = readData(io, this.op, answer);
			}
		}
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		final RJIO io = RJIO.get(in);
		final int check = io.readCheck1();
		readExternal(io, null);
		io.readCheck2(check);
		io.disconnect(in);
	}
	
	
	public int getComType() {
		return T_DBG;
	}
	
	@Override
	public byte getCmdType() {
		return T_DBG_ITEM;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		assert (status != null);
		if (status == RjsStatus.OK_STATUS) {
			this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.status = null;
			this.data = null;
		}
		else {
			this.options = (this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS);
			this.status = status;
			this.data = null;
		}
	}
	
	public void setAnswer(final RJIOExternalizable data) {
		this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
		if (data != null) {
			this.options |= OV_WITHDATA;
		}
		this.status = null;
		this.data = data;
	}
	
	
	@Override
	public byte getOp() {
		return this.op;
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
		return null;
	}
	
	public Object getData() {
		return this.data;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof DbgCmdItem)) {
			return false;
		}
		final DbgCmdItem otherItem = (DbgCmdItem) other;
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
		sb.append("DbgCmdItem ");
		switch (this.op) {
		case OP_LOAD_FRAME_LIST:
			sb.append("LOAD_FRAME_LIST");
			break;
		case OP_LOAD_FRAME_CONTEXT:
			sb.append("LOAD_FRAME_CONTEXT");
			break;
		case OP_SET_DEBUG:
			sb.append("SET_DEBUG");
			break;
		case OP_REQUEST_SUSPEND:
			sb.append("REQUEST_SUSPEND");
			break;
		case OP_INSTALL_TP_POSITIONS:
			sb.append("INSTALL_TRACEPOINTS");
			break;
		case OP_SET_ENABLEMENT:
			sb.append("SET_ENABLEMENT");
			break;
		case OP_RESET_FILTER_STATE:
			sb.append("RESET_FILTER_STATE");
			break;
		case OP_UPDATE_TP_STATES:
			sb.append("UPDATE_TRACEPOINTS_STATES");
			break;
		case OP_NOTIFY_TP_EVENT:
			sb.append("NOTIFY_TP_EVENT");
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n");
			sb.append(this.data);
			sb.append("\n</DATA>");
		}
		return sb.toString();
	}
	
}
