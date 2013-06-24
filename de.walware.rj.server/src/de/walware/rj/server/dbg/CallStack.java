/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.dbg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class CallStack implements RJIOExternalizable {
	
	
	public static final int FLAG_TOPFRAME =                0x00001000;
	
	public static final int FLAG_NOSTEPPING =              0x00000100;
	
	public static final int FLAG_SOURCE =                  0x00000010;
	public static final int FLAG_COMMAND =                 0x00000020;
	
	
	public static class Frame {
		
		private final int position;
		private final String call;
		
		protected long handle;
		
		protected String fileName;
		protected long fileTimestamp;
		
		protected int[] exprSrcref;
		
		protected int flags;
		
		
		public Frame(final int position, final String call, final long handle,
				final String fileName, final long fileTimestamp, final int[] exprSrcref) {
			this.position = position;
			this.call = call;
			this.handle = handle;
			this.fileName = fileName;
			this.fileTimestamp = fileTimestamp;
			this.exprSrcref = exprSrcref;
		}
		
		protected Frame(final int position, final String call) {
			this.position = position;
			this.call = call;
		}
		
		
		public int getPosition() {
			return this.position;
		}
		
		public String getCall() {
			return this.call;
		}
		
		public long getHandle() {
			return this.handle;
		}
		
		public String getFileName() {
			return this.fileName;
		}
		
		public long getFileTimestamp() {
			return this.fileTimestamp;
		}
		
		public int[] getExprSrcref() {
			return this.exprSrcref;
		}
		
		
		public int getFlags() {
			return this.flags;
		}
		
		public void addFlags(final int flags) {
			this.flags |= flags;
		}
		
		/** top frame */
		public boolean isTopFrame() {
			return ((this.flags & FLAG_TOPFRAME) != 0);
		}
		
		/** frame of top level command */
		public boolean isTopLevelCommand() {
			return (this.position == 3 &&(this.flags & 0xff) == (FLAG_COMMAND | 2));
		}
		
	}
	
	
	private final List<? extends Frame> frames;
	
	
	public CallStack(final List<? extends Frame> list, final boolean setDefaultFlags) {
		this.frames = list;
		if (setDefaultFlags) {
			setDefaultFlags();
		}
	}
	
	public CallStack(final RJIO io) throws IOException {
		final int l = io.readInt();
		final ArrayList<Frame> list = new ArrayList<Frame>(l);
		for (int i = 0; i < l; i++) {
			final Frame frame = new Frame(i, io.readString());
			frame.handle = io.readLong();
			frame.fileName = io.readString();
			frame.fileTimestamp = io.readLong();
			frame.exprSrcref = io.readIntArray();
			frame.flags = io.readInt();
			list.add(frame);
		}
		this.frames = list;
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		final int l = this.frames.size();
		io.writeInt(l);
		for (int i = 0; i < l; i++) {
			final Frame frame = this.frames.get(i);
			io.writeString(frame.call);
			io.writeLong(frame.handle);
			io.writeString(frame.fileName);
			io.writeLong(frame.fileTimestamp);
			io.writeIntArray(frame.exprSrcref, (frame.exprSrcref != null) ? 6 : -1);
			io.writeInt(frame.flags);
		}
	}
	
	protected void setDefaultFlags() {
		final int n = this.frames.size();
		for (int i = 0; i < n; i++) {
			final Frame frame0 = this.frames.get(i);
			if (frame0.getCall() != null) {
				if (frame0.getCall().startsWith("source(")) {
					Frame frame1;
					Frame frame2;
					if (i+1 < n && (frame1 = this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("eval.with.vis(") ) {
						frame0.addFlags((FLAG_SOURCE | 0));
						frame1.addFlags((FLAG_SOURCE | 1));
						i++;
						if (i+1 < n && (frame2 = this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval.with.vis(") ) {
							frame2.addFlags((FLAG_SOURCE | 2));
							i++;
						}
					}
					else if (i+2 < n && (frame1 = this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("withVisible(")
							&& (frame2 = this.frames.get(i+2)).getCall() != null
							&& frame2.getCall().startsWith("eval(") ) {
						frame0.addFlags((FLAG_SOURCE | 0));
						frame1.addFlags((FLAG_SOURCE | 1));
						frame2.addFlags((FLAG_SOURCE | 2));
						i += 2;
						if (i+1 < n && (frame2 = this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval(") ) {
							frame2.addFlags((FLAG_SOURCE | 3));
							i++;
						}
					}
				}
				else if (frame0.getCall().startsWith("rj:::.statet.evalCommand(")
						|| frame0.getCall().startsWith(".statet.evalCommand(") ) {
					frame0.addFlags((FLAG_COMMAND | 0));
					final Frame frame1;
					if (i+1 < n && (frame1 = this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("eval(") ) {
						frame1.addFlags((FLAG_COMMAND | 1));
						i++;
						final Frame frame2;
						if (i+1 < n && (frame2 = this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval(") ) {
							frame2.addFlags((FLAG_COMMAND | 2));
							i++;
						}
					}
				}
			}
		}
		this.frames.get(n-1).addFlags(FLAG_TOPFRAME);
	}
	
	public List<? extends Frame> getFrames() {
		return this.frames;
	}
	
}
