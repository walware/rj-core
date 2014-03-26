/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.dbg;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;
import de.walware.rj.server.DbgCmdItem;
import de.walware.rj.server.Operation;


public abstract class DbgRequest implements Operation, RJIOExternalizable {
	
	
	public static final byte RESUME=                        DbgCmdItem.OP_CTRL_RESUME;
	public static final byte STEP_INTO=                     DbgCmdItem.OP_CTRL_STEP_INTO;
	public static final byte STEP_OVER=                     DbgCmdItem.OP_CTRL_STEP_OVER;
	public static final byte STEP_RETURN=                   DbgCmdItem.OP_CTRL_STEP_RETURN;
	
	
	private static final int FRAME_POSITION=                00000001;
	private static final int FRAME_HANDLE=                  00000002;
	
	
	public static class Resume extends DbgRequest implements SyncOp {
		
		
		public Resume() {
		}
		
		public Resume(final RJIO in) throws IOException {
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
		}
		
		
		@Override
		public byte getOp() {
			return RESUME;
		}
		
	}
	
	public static class StepOver extends DbgRequest implements SyncOp {
		
		
		public StepOver() {
		}
		
		public StepOver(final RJIO in) throws IOException {
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
		}
		
		
		@Override
		public byte getOp() {
			return STEP_OVER;
		}
		
	}
	
	public static class StepInto extends DbgRequest implements SyncOp {
		
		
		public StepInto() {
		}
		
		public StepInto(final RJIO in) throws IOException {
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
		}
		
		
		@Override
		public byte getOp() {
			return STEP_INTO;
		}
		
	}
	
	public static class StepReturn extends DbgRequest implements SyncOp {
		
		
		private final int detail;
		
		private final RJIOExternalizable target;
		
		
		public StepReturn(final FrameRef target) {
			if (target instanceof FrameRef.ByPosition) {
				this.detail= FRAME_POSITION;
			}
			else if (target instanceof FrameRef.ByHandle) {
				this.detail= FRAME_HANDLE;
			}
			else {
				throw new IllegalArgumentException("target"); //$NON-NLS-1$
			}
			this.target= target;
		}
		
		public StepReturn(final RJIO in) throws IOException {
			this.detail= in.readInt();
			switch (this.detail & 0xf) {
			case FRAME_POSITION:
				this.target= new FrameRef.ByPosition(in);
				break;
			case FRAME_HANDLE:
				this.target= new FrameRef.ByHandle(in);
				break;
			default:
				throw new IOException();
			}
		}
		
		@Override
		public void writeExternal(final RJIO out) throws IOException {
			out.writeInt(this.detail);
			this.target.writeExternal(out);
		}
		
		
		@Override
		public byte getOp() {
			return STEP_RETURN;
		}
		
		public Object getTarget() {
			return this.target;
		}
		
	}
	
	
}
