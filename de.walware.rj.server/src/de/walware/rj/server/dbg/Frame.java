/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
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

package de.walware.rj.server.dbg;


public class Frame {
	
	
	private final int position;
	private final String call;
	
	protected long handle;
	
	protected String fileName;
	protected long fileTimestamp;
	
	protected int[] exprSrcref;
	
	protected int flags;
	
	
	public Frame(final int position, final String call, final long handle,
			final String fileName, final long fileTimestamp, final int[] exprSrcref) {
		this.position= position;
		this.call= call;
		this.handle= handle;
		this.fileName= fileName;
		this.fileTimestamp= fileTimestamp;
		this.exprSrcref= exprSrcref;
	}
	
	protected Frame(final int position, final String call) {
		this.position= position;
		this.call= call;
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
		return ((this.flags & CallStack.FLAG_TOPFRAME) != 0);
	}
	
	/** frame of top level command */
	public boolean isTopLevelCommand() {
		return (this.position == 3 && (this.flags & 0xff) == (CallStack.FLAG_COMMAND | 2));
	}
	
}
