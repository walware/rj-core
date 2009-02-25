/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
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


/**
 * Command usually reports status of last command.
 */
public final class RjsStatus implements RjsComObject, Externalizable {
	
	
	public static final RjsStatus OK_STATUS = new RjsStatus(V_OK, 0);
	
	
	private int severity;
	private int code;
	
	
	public RjsStatus() {
	}
	
	public RjsStatus(final int severity, final int code) {
		this.severity = severity;
		this.code = code;
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.severity);
		out.writeInt(this.code);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.severity = in.readInt();
		this.code = in.readInt();
	}
	
	
	public int getComType() {
		return RjsComObject.T_STATUS;
	}
	
	public int getSeverity() {
		return this.severity;
	}
	
	public int getCode() {
		return this.code;
	}
	
	
	@Override
	public int hashCode() {
		return this.severity+this.code;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof RjsStatus)) {
			return false;
		}
		final RjsStatus other = (RjsStatus) obj;
		return (other.code == this.code && other.severity == this.severity);
	}
	
}
