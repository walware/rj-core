/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RFunction;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RFunctionImpl extends AbstractRObject
		implements RFunction, ExternalizableRObject {
	
	
	public static RFunctionImpl createForServer(final String header) {
		return new RFunctionImpl(header);
	}
	
	
	private String headerSource;
	private String bodySource;
	
	
	private RFunctionImpl(final String header) {
		this.headerSource = header;
	}
	
	public RFunctionImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		/*final int options =*/ in.readInt();
		this.headerSource = in.readUTF();
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		out.writeInt(/*options*/ 0);
		out.writeUTF((this.headerSource != null) ? this.headerSource : "");
	}
	
	
	public byte getRObjectType() {
		return TYPE_FUNCTION;
	}
	
	public int getLength() {
		return 1;
	}
	
	public String getRClassName() {
		return "function";
	}
	
	public String getHeaderSource() {
		return this.headerSource;
	}
	
	public String getBodySource() {
		return this.bodySource;
	}
	
	public RCharacterStore getNames() {
		return null;
	}
	
	public RStore getData() {
		return null;
	}
	
}
