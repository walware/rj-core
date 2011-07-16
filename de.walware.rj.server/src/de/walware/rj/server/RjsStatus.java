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
import de.walware.rj.data.RJIOExternalizable;


public final class RjsStatus implements RjsComObject, Externalizable, RJIOExternalizable {
	
	
	public static final int OK =               0x0;
	public static final int INFO =             0x1;
	public static final int WARNING =          0x2;
	public static final int ERROR =            0x4;
	public static final int CANCEL =           0x8;
	
	
	public static final RjsStatus OK_STATUS = new RjsStatus(OK, 0);
	
	public static final RjsStatus CANCEL_STATUS = new RjsStatus(CANCEL, 0);
	
	
	private int severity;
	private int code;
	private String text;
	
	
	/**
	 * Constructor to create a status object without a message
	 */
	public RjsStatus(final int severity, final int code) {
		this.severity = severity;
		this.code = code;
		this.text = null;
	}
	
	/**
	 * Constructor to create a status object with a message
	 */
	public RjsStatus(final int severity, final int code, final String s) {
		this.severity = severity;
		this.code = code;
		this.text = (s != null && s.length() > 0) ? s : null;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public RjsStatus() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public RjsStatus(final ObjectInput in) throws IOException {
		readExternal(in);
	}
	
	public RjsStatus(final RJIO io) throws IOException {
		if (io.readBoolean()) {
			this.severity = io.readByte();
			this.code = io.readInt();
			this.text = io.readString();
		}
		else {
			this.severity = io.readByte();
			this.code = io.readInt();
			this.text = null;
		}
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		if (this.text != null) {
			io.writeBoolean(true);
			io.writeByte(this.severity);
			io.writeInt(this.code);
			io.writeString(this.text);
		}
		else {
			io.writeBoolean(false);
			io.writeByte(this.severity);
			io.writeInt(this.code);
		}
	}
	
	public void readExternal(final ObjectInput in) throws IOException {
		if (in.readBoolean()) {
			this.severity = in.readByte();
			this.code = in.readInt();
			this.text = in.readUTF();
		}
		else {
			this.severity = in.readByte();
			this.code = in.readInt();
			this.text = null;
		}
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		if (this.text != null) {
			out.writeBoolean(true);
			out.writeByte(this.severity);
			out.writeInt(this.code);
			out.writeUTF(this.text);
		}
		else {
			out.writeBoolean(false);
			out.writeByte(this.severity);
			out.writeInt(this.code);
		}
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
	
	public String getMessage() {
		return (this.text != null) ? this.text : "";
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
				&& ((this.text != null) ? this.text.equals(other.text) : (null == other.text)) );
	}
	
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("RjsStatus (severity=");
		switch (this.severity) {
		case OK:
			sb.append("OK");
			break;
		case INFO:
			sb.append("INFO");
			break;
		case WARNING:
			sb.append("WARNING");
			break;
		case ERROR:
			sb.append("ERROR");
			break;
		case CANCEL:
			sb.append("CANCEL");
			break;
		default:
			sb.append(this.severity);
			break;
		}
		sb.append(", code=0x");
		sb.append(Integer.toHexString(this.code));
		sb.append(")");
		if (this.text != null) {
			sb.append("\n<TEXT>\n");
			sb.append(this.text);
			sb.append("\n</TEXT>");
		}
		else {
			sb.append("\n<TEXT />");
		}
		return sb.toString();
	}
	
}
