/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
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
	
	public ROtherImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		//-- options
		final int options = in.readInt();
		//-- special attributes
		this.className1 = in.readUTF();
		//-- attributes
		if ((options & RObjectFactoryImpl.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(in, flags));
		}
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		final RList attributes = ((flags & RObjectFactoryImpl.F_WITH_ATTR) != 0) ? getAttributes() : null;
		out.writeInt((attributes != null) ? RObjectFactoryImpl.F_WITH_ATTR : 0);
		out.writeUTF(this.className1);
		if (attributes != null) {
			factory.writeAttributeList(attributes, out, flags);
		}
	}
	
	
	public int getRObjectType() {
		return TYPE_OTHER;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	public int getLength() {
		return 0;
	}
	
	public RStore getData() {
		return null;
	}
	
}
