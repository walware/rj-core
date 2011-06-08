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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;


/**
 * Client-to-Server list with {@link MainCmdItem}s.
 */
public final class CtrlCmdItem implements RjsComObject, Externalizable {
	
	
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
	public CtrlCmdItem(final RJIO io) throws IOException {
		readExternal(io.in);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.ctrl);
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		this.ctrl = in.readInt();
	}
	
	
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
		final StringBuilder sb = new StringBuilder(100);
		sb.append("CtrlCmdItem (");
		sb.append(this.ctrl);
		sb.append(")");
		return sb.toString();
	}
	
}
