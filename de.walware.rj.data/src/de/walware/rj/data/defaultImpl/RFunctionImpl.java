/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.data.RFunction;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RFunctionImpl extends AbstractRObject
		implements RFunction, ExternalizableRObject {
	
	
	private String headerSource;
	private String bodySource;
	
	
	public RFunctionImpl(final String header) {
		this.headerSource = header;
	}
	
	public RFunctionImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		/*final int options =*/ io.readInt();
		this.headerSource = io.readString();
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeInt(/*options*/ 0);
		io.writeString(this.headerSource);
	}
	
	
	public byte getRObjectType() {
		return TYPE_FUNCTION;
	}
	
	public String getRClassName() {
		return "function";
	}
	
	
	public int getLength() {
		return 0;
	}
	
	public String getHeaderSource() {
		return this.headerSource;
	}
	
	public String getBodySource() {
		return this.bodySource;
	}
	
	public RStore getData() {
		return null;
	}
	
}
