/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
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


public class FrameContext implements RJIOExternalizable {
	
	
	public static final int SOURCETYPE_1_LINES= 1;
	public static final int SOURCETYPE_1_FILE= 2;
	public static final int SOURCETYPE_2_LINES= 3;
	public static final int SOURCETYPE_3_DEPARSE= 4;
	
	
	private final int position;
	private final String call;
	
	protected String fileName;
	protected long fileTimestamp;
	protected String fileEncoding;
	protected String filePath;
	
	protected int sourceType;
	protected String sourceCode;
	protected int[] sourceSrcref;
	
	protected int[] firstSrcref;
	protected int[] lastSrcref;
	protected int[] exprSrcref;
	
	
	public FrameContext(final int position, final String call,
			final String fileName, final long fileTimestamp, final String fileEncoding,
			final String filePath, final int sourceType, final String sourceCode, final int[] sourceSrcref,
			final int[] firstSrcref, final int[] lastSrcref, final int[] exprSrcref) {
		this.position= position;
		this.call= call;
		
		this.fileName= fileName;
		this.fileTimestamp= fileTimestamp;
		this.fileEncoding= fileEncoding;
		this.filePath= filePath;
		
		this.sourceType= sourceType;
		this.sourceCode= sourceCode;
		this.sourceSrcref= sourceSrcref;
		
		this.firstSrcref= firstSrcref;
		this.lastSrcref= lastSrcref;
		this.exprSrcref= exprSrcref;
	}
	
	public FrameContext(final RJIO io) throws IOException {
		this.position= io.readInt();
		this.call= io.readString();
		
		this.fileName= io.readString();
		this.fileTimestamp= io.readLong();
		this.fileEncoding= io.readString();
		this.filePath= io.readString();
		
		this.sourceType= io.readInt();
		this.sourceCode= io.readString();
		this.sourceSrcref= io.readIntArray();
		
		this.firstSrcref= io.readIntArray();
		this.lastSrcref= io.readIntArray();
		this.exprSrcref= io.readIntArray();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.position);
		io.writeString(this.call);
		
		io.writeString(this.fileName);
		io.writeLong(this.fileTimestamp);
		io.writeString(this.fileEncoding);
		io.writeString(this.filePath);
		
		io.writeInt(this.sourceType);
		io.writeString(this.sourceCode);
		io.writeIntArray(this.sourceSrcref, (this.sourceSrcref != null) ? 6 : -1);
		
		io.writeIntArray(this.firstSrcref, (this.firstSrcref != null) ? 6 : -1);
		io.writeIntArray(this.lastSrcref, (this.lastSrcref != null) ? 6 : -1);
		io.writeIntArray(this.exprSrcref, (this.exprSrcref != null) ? 6 : -1);
	}
	
	
	public int getPosition() {
		return this.position;
	}
	
	public String getCall() {
		return this.call;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	public long getFileTimestamp() {
		return this.fileTimestamp;
	}
	
	public String getFileEncoding() {
		return this.fileEncoding;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public int getSourceType() {
		return this.sourceType;
	}
	
	public String getSourceCode() {
		return this.sourceCode;
	}
	
	public int[] getSourceSrcref() {
		return this.sourceSrcref;
	}
	
	public int[] getFirstSrcref() {
		return this.firstSrcref;
	}
	
	public int[] getLastSrcref() {
		return this.lastSrcref;
	}
	
	public int[] getExprSrcref() {
		return this.exprSrcref;
	}
	
}
