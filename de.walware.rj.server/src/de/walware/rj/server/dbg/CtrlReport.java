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


public class CtrlReport implements RJIOExternalizable {
	
	
	private final static int REQUEST_EXECUTED=              0x01000000;
	
	private final static int ENGINE_EXECUTING=              0;
	private final static int ENGINE_SUSPENDED=              0x02000000;
	
	private final static int RESET_PROMPT=                  0x00100000;
	
	
	public static CtrlReport createRequestExecuted(final byte type) {
		switch (type) {
		case DbgRequest.RESUME:
		case DbgRequest.STEP_INTO:
		case DbgRequest.STEP_OVER:
		case DbgRequest.STEP_RETURN:
			return new CtrlReport(type | REQUEST_EXECUTED | ENGINE_EXECUTING);
		default:
			throw new IllegalArgumentException("type= " + type); //$NON-NLS-1$
		}
	}
	
	public static CtrlReport createRequestNotApplicable(final boolean isEngineSuspended) {
		return new CtrlReport((isEngineSuspended) ? ENGINE_SUSPENDED : ENGINE_EXECUTING);
	}
	
	public static CtrlReport createRequestNotSupported(final boolean isEngineSuspended) {
		return new CtrlReport((isEngineSuspended) ? ENGINE_SUSPENDED : ENGINE_EXECUTING);
	}
	
	
	private final int code;
	
	
	private CtrlReport(final int code) {
		this.code= code;
	}
	
	public CtrlReport(final RJIO io) throws IOException {
		this.code= io.readInt();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.code);
	}
	
	
	public byte getOp() {
		return (byte) (this.code & 0xff);
	}
	
	public boolean isRequestExecuted() {
		return ((this.code & REQUEST_EXECUTED) != 0);
	}
	
	public boolean isEngineSuspended() {
		return ((this.code & ENGINE_SUSPENDED) != 0);
	}
	
}
