/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class ROtherImpl extends AbstractRObject
		implements ExternalizableRObject {
	
	
	private String className1;
	
	
	public ROtherImpl(final String className1) {
		if (className1 == null) {
			throw new NullPointerException();
		}
		this.className1 = className1;
	}
	
	public ROtherImpl(final String className1, final RList attributes) {
		if (className1 == null) {
			throw new NullPointerException();
		}
		this.className1 = className1;
		setAttributes(attributes);
	}
	
	public ROtherImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		//-- special attributes
		this.className1 = io.readString();
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final RList attributes = ((io.flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		//-- options
		final int options = (attributes != null) ? RObjectFactory.O_WITH_ATTR : 0;
		io.writeInt(options);
		//-- special attributes
		io.writeString(this.className1);
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, io);
		}
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_OTHER;
	}
	
	@Override
	public String getRClassName() {
		return this.className1;
	}
	
	@Override
	public long getLength() {
		return 0;
	}
	
	@Override
	public RStore<?> getData() {
		return null;
	}
	
}
