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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;
import de.walware.rj.server.dbg.CallStack;
import de.walware.rj.server.dbg.CtrlReport;
import de.walware.rj.server.dbg.DbgEnablement;
import de.walware.rj.server.dbg.DbgFilterState;
import de.walware.rj.server.dbg.DbgRequest;
import de.walware.rj.server.dbg.ElementTracepointInstallationRequest;
import de.walware.rj.server.dbg.FlagTracepointInstallationRequest;
import de.walware.rj.server.dbg.FrameContext;
import de.walware.rj.server.dbg.FrameContextDetailRequest;
import de.walware.rj.server.dbg.SetDebugReport;
import de.walware.rj.server.dbg.SetDebugRequest;
import de.walware.rj.server.dbg.TracepointEvent;
import de.walware.rj.server.dbg.TracepointInstallationReport;
import de.walware.rj.server.dbg.TracepointStatesUpdate;


/**
 * Command item for main loop dbg operations (="op")
 */
public final class DbgCmdItem extends MainCmdItem implements RjsComObject, Externalizable {
	
	// -- C2S sync
	public static final byte OP_LOAD_FRAME_LIST=            0001;
	public static final byte OP_LOAD_FRAME_CONTEXT=         0002;
	public static final byte OP_SET_DEBUG=                  0003;
	public static final byte OP_REQUEST_SUSPEND=            0004;
//	public static final byte OP_CTRL_SUSPEND=               0010;
	public static final byte OP_CTRL_RESUME=                0011;
	public static final byte OP_CTRL_STEP_INTO=             0012;
	public static final byte OP_CTRL_STEP_OVER=             0014;
	public static final byte OP_CTRL_STEP_RETURN=           0016;
	public static final byte OP_INSTALL_TP_FLAGS=           0020;
	public static final byte OP_INSTALL_TP_POSITIONS=       0021;
	
	// -- C2S sync + async
	public static final byte OP_SET_ENABLEMENT=             0031;
	public static final byte OP_RESET_FILTER_STATE=         0032;
	public static final byte OP_UPDATE_TP_STATES=           0034;
	
	public static final byte OP_C2S_S2C=                    0040;
	
	public static final byte OP_NOTIFY_TP_EVENT=            0x41;
	
	
	private static final int OV_WITHDATA=                   0x01000000;
	private static final int OV_WITHSTATUS=                 0x08000000;
	
	
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
			case OP_CTRL_RESUME:
				return new DbgRequest.Resume(io);
			case OP_CTRL_STEP_INTO:
				return new DbgRequest.StepInto(io);
			case OP_CTRL_STEP_OVER:
				return new DbgRequest.StepOver(io);
			case OP_CTRL_STEP_RETURN:
				return new DbgRequest.StepReturn(io);
			case OP_SET_ENABLEMENT:
				return new DbgEnablement(io);
			case OP_RESET_FILTER_STATE:
				return new DbgFilterState(io);
			case OP_INSTALL_TP_FLAGS:
				return new FlagTracepointInstallationRequest(io);
			case OP_INSTALL_TP_POSITIONS:
				return new ElementTracepointInstallationRequest(io);
			case OP_UPDATE_TP_STATES:
				return new TracepointStatesUpdate(io);
			case OP_NOTIFY_TP_EVENT:
				return new TracepointEvent(io);
			default:
				break;
			}
			throw new IOException("Unsupported type= " + type); //$NON-NLS-1$
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
			case OP_CTRL_RESUME:
			case OP_CTRL_STEP_INTO:
			case OP_CTRL_STEP_OVER:
			case OP_CTRL_STEP_RETURN:
				return new CtrlReport(io);
			case OP_SET_ENABLEMENT:
			case OP_RESET_FILTER_STATE:
				break; // status
			case OP_INSTALL_TP_FLAGS:
			case OP_INSTALL_TP_POSITIONS:
				return new TracepointInstallationReport(io);
			case OP_UPDATE_TP_STATES:
				break; // status
			default:
				break;
			}
			throw new IOException("Unsupported type= " + type); //$NON-NLS-1$
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
		this.op= op;
		this.options= (options & OM_CUSTOM);
		if (data != null) {
			this.options |= OV_WITHDATA;
		}
		if (op < OP_NOTIFY_TP_EVENT) {
			this.options |= OV_WAITFORCLIENT;
		}
		this.data= data;
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
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		final RJIO io= RJIO.get(out);
		final int check= io.writeCheck1();
		writeExternal(io);
		io.writeCheck2(check);
		io.disconnect(out);
	}
	
	private void readExternal(final RJIO io, final CustomDataReader reader) throws IOException {
		this.options= io.readInt();
		this.op= io.readByte();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status= new RjsStatus(io);
		}
		else if ((this.options & OV_WITHDATA) != 0) {
			final boolean answer= ((this.options & OV_ANSWER) != 0);
			if (reader != null) {
				this.data= reader.read(this.op, answer, io);
				if (this.data == null) {
					this.data= readData(io, this.op, answer);
				}
			}
			else {
				this.data= readData(io, this.op, answer);
			}
		}
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		final RJIO io= RJIO.get(in);
		final int check= io.readCheck1();
		readExternal(io, null);
		io.readCheck2(check);
		io.disconnect(in);
	}
	
	
	@Override
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
			this.options= (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.status= null;
			this.data= null;
		}
		else {
			this.options= (this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS);
			this.status= status;
			this.data= null;
		}
	}
	
	public void setAnswer(final RJIOExternalizable data) {
		this.options= (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
		if (data != null) {
			this.options |= OV_WITHDATA;
		}
		this.status= null;
		this.data= data;
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
		final DbgCmdItem otherItem= (DbgCmdItem) other;
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
		final StringBuffer sb= new StringBuffer(100);
		sb.append("DbgCmdItem "); //$NON-NLS-1$
		switch (this.op) {
		case OP_LOAD_FRAME_LIST:
			sb.append("LOAD_FRAME_LIST"); //$NON-NLS-1$
			break;
		case OP_LOAD_FRAME_CONTEXT:
			sb.append("LOAD_FRAME_CONTEXT"); //$NON-NLS-1$
			break;
		case OP_SET_DEBUG:
			sb.append("SET_DEBUG"); //$NON-NLS-1$
			break;
		case OP_REQUEST_SUSPEND:
			sb.append("REQUEST_SUSPEND"); //$NON-NLS-1$
			break;
		case OP_CTRL_RESUME:
			sb.append("CTRL_RESUME"); //$NON-NLS-1$
			break;
		case OP_CTRL_STEP_INTO:
			sb.append("CTRL_STEP_INTO"); //$NON-NLS-1$
			break;
		case OP_CTRL_STEP_OVER:
			sb.append("CTRL_STEP_OVER"); //$NON-NLS-1$
			break;
		case OP_CTRL_STEP_RETURN:
			sb.append("CTRL_STEP_RETURN"); //$NON-NLS-1$
			break;
		case OP_INSTALL_TP_FLAGS:
			sb.append("INSTALL_TP_FLAGS"); //$NON-NLS-1$
			break;
		case OP_INSTALL_TP_POSITIONS:
			sb.append("INSTALL_TP_POSITIONS"); //$NON-NLS-1$
			break;
		case OP_SET_ENABLEMENT:
			sb.append("SET_ENABLEMENT"); //$NON-NLS-1$
			break;
		case OP_RESET_FILTER_STATE:
			sb.append("RESET_FILTER_STATE"); //$NON-NLS-1$
			break;
		case OP_UPDATE_TP_STATES:
			sb.append("UPDATE_TRACEPOINTS_STATES"); //$NON-NLS-1$
			break;
		case OP_NOTIFY_TP_EVENT:
			sb.append("NOTIFY_TP_EVENT"); //$NON-NLS-1$
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t" + "options= 0x").append(Integer.toHexString(this.options)); //$NON-NLS-1$ //$NON-NLS-2$
		if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n"); //$NON-NLS-1$
			sb.append(this.data);
			sb.append("\n</DATA>"); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
}
