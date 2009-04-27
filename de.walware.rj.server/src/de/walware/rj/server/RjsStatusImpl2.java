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
public final class RjsStatusImpl2 implements RjsStatus, RjsComObject, Externalizable {
	
	
	private int severity;
	private int code;
	private String text;
	
	
	/**
	 * Constructor for automatic deserialization
	 */
	public RjsStatusImpl2() {
	}
	
	public RjsStatusImpl2(final int severity, final int code, final String s) {
		this.severity = severity;
		this.code = code;
		this.text = (s != null) ? s : "";
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.severity);
		out.writeInt(this.code);
		out.writeUTF(this.text);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.severity = in.readInt();
		this.code = in.readInt();
		this.text = in.readUTF();
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
	
	public String getTextDetail() {
		return this.text;
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
		return (this.code == other.getCode() && this.severity == other.getSeverity()
				&& this.text.equals(other.getTextDetail()));
	}
	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(100);
		sb.append("RjsStatus (severity=");
		switch (this.severity) {
		case OK:
			sb.append("OK");
			break;
		case V_INFO:
			sb.append("INFO");
			break;
		case V_WARNING:
			sb.append("WARNING");
			break;
		case V_ERROR:
			sb.append("ERROR");
			break;
		case V_CANCEL:
			sb.append("CANCEL");
			break;
		default:
			sb.append(this.severity);
			break;
		}
		sb.append(", code=0x");
		sb.append(Integer.toHexString(this.code));
		sb.append(")");
		sb.append("\n\t)");
		sb.append(this.text);
		return sb.toString();
	}
	
}
