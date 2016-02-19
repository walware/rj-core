/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeInt(/*options*/ 0);
		io.writeString(this.headerSource);
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_FUNCTION;
	}
	
	@Override
	public String getRClassName() {
		return "function";
	}
	
	
	@Override
	public long getLength() {
		return 0;
	}
	
	@Override
	public String getHeaderSource() {
		return this.headerSource;
	}
	
	@Override
	public String getBodySource() {
		return this.bodySource;
	}
	
	@Override
	public RStore<?> getData() {
		return null;
	}
	
}
