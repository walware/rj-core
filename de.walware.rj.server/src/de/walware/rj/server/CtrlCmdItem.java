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


/**
 * Client-to-Server list with {@link MainCmdItem}s.
 */
public final class CtrlCmdItem implements RjsComObject, RJIOExternalizable, Externalizable {
	
	
	public static final int REQUEST_CANCEL = 1;
	public static final int REQUEST_HOT_MODE = 2;
	
	
	private int ctrl;
	
	
	public CtrlCmdItem(final int ctrlId) {
		this.ctrl = ctrlId;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public CtrlCmdItem() {
	}
	
	/**
	 * Constructor for automatic deserialization
	 * @throws IOException 
	 */
	public CtrlCmdItem(final RJIO in) throws IOException {
		this.ctrl = in.readInt();
	}
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException {
		this.ctrl = in.readInt();
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.ctrl);
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.ctrl);
	}
	
	
	@Override
	public int getComType() {
		return RjsComObject.T_CTRL;
	}
	
	public int getCtrlId() {
		return this.ctrl;
	}
	
	
	public boolean testEquals(final CtrlCmdItem other) {
		if (this.ctrl != other.ctrl) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(64);
		sb.append("CtrlCmdItem ");
		switch (this.ctrl) {
		case REQUEST_CANCEL:
			sb.append("REQUEST_CANCEL");
			break;
		case REQUEST_HOT_MODE:
			sb.append("REQUEST_HOT_MODE");
			break;
		default:
			sb.append(this.ctrl);
			break;
		}
		return sb.toString();
	}
	
}
