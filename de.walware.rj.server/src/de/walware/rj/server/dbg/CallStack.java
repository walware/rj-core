/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
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
import java.util.ArrayList;
import java.util.List;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class CallStack implements RJIOExternalizable {
	
	
	public static final int FLAG_TOPFRAME=                  0x00001000;
	
	public static final int FLAG_NOSTEPPING=                0x00000100;
	
	public static final int FLAG_SOURCE=                    0x00000010;
	public static final int FLAG_COMMAND=                   0x00000020;
	
	
	private final List<? extends Frame> frames;
	
	
	public CallStack(final List<? extends Frame> list, final boolean setDefaultFlags) {
		this.frames= list;
		if (setDefaultFlags) {
			setDefaultFlags();
		}
	}
	
	public CallStack(final RJIO io) throws IOException {
		final int l= io.readInt();
		final ArrayList<Frame> list= new ArrayList<Frame>(l);
		for (int i= 0; i < l; i++) {
			final Frame frame= new Frame(i, io.readString());
			frame.handle= io.readLong();
			frame.fileName= io.readString();
			frame.fileTimestamp= io.readLong();
			frame.exprSrcref= io.readIntArray();
			frame.flags= io.readInt();
			list.add(frame);
		}
		this.frames= list;
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		final int l= this.frames.size();
		io.writeInt(l);
		for (int i= 0; i < l; i++) {
			final Frame frame= this.frames.get(i);
			io.writeString(frame.getCall());
			io.writeLong(frame.handle);
			io.writeString(frame.fileName);
			io.writeLong(frame.fileTimestamp);
			io.writeIntArray(frame.exprSrcref, (frame.exprSrcref != null) ? 6 : -1);
			io.writeInt(frame.flags);
		}
	}
	
	protected void setDefaultFlags() {
		final int n= this.frames.size();
		for (int i= 0; i < n; i++) {
			final Frame frame0= this.frames.get(i);
			if (frame0.getCall() != null) {
				if (frame0.getCall().startsWith("source(")) { //$NON-NLS-1$
					Frame frame1;
					Frame frame2;
					if (i+1 < n && (frame1= this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("eval.with.vis(") ) { //$NON-NLS-1$
						frame0.addFlags((FLAG_SOURCE | 0));
						frame1.addFlags((FLAG_SOURCE | 1));
						i++;
						if (i+1 < n && (frame2= this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval.with.vis(") ) { //$NON-NLS-1$
							frame2.addFlags((FLAG_SOURCE | 2));
							i++;
						}
					}
					else if (i+2 < n && (frame1= this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("withVisible(") //$NON-NLS-1$
							&& (frame2= this.frames.get(i+2)).getCall() != null
							&& frame2.getCall().startsWith("eval(") ) { //$NON-NLS-1$
						frame0.addFlags((FLAG_SOURCE | 0));
						frame1.addFlags((FLAG_SOURCE | 1));
						frame2.addFlags((FLAG_SOURCE | 2));
						i += 2;
						if (i+1 < n && (frame2= this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval(") ) { //$NON-NLS-1$
							frame2.addFlags((FLAG_SOURCE | 3));
							i++;
						}
					}
				}
				else if (frame0.getCall().startsWith("rj:::.statet.evalCommand(") //$NON-NLS-1$
						|| frame0.getCall().startsWith(".statet.evalCommand(") ) { //$NON-NLS-1$
					frame0.addFlags((FLAG_COMMAND | 0));
					final Frame frame1;
					if (i+1 < n && (frame1= this.frames.get(i+1)).getCall() != null
							&& frame1.getCall().startsWith("eval(") ) { //$NON-NLS-1$
						frame1.addFlags((FLAG_COMMAND | 1));
						i++;
						final Frame frame2;
						if (i+1 < n && (frame2= this.frames.get(i+1)).getCall() != null
								&& frame2.getCall().startsWith("eval(") ) { //$NON-NLS-1$
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
	
	public Frame findFrame(final long handle) {
		for (int i= 0; i < this.frames.size(); i++) {
			final Frame frame= this.frames.get(i);
			if (frame.getHandle() == handle) {
				return frame;
			}
		}
		return null;
	}
	
}
