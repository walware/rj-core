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

package de.walware.rj.server.jriImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;
import de.walware.rj.data.defaultImpl.AbstractRObject;
import de.walware.rj.data.defaultImpl.ExternalizableRObject;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;


public class JRIVectorImpl<DataType extends RStore> extends AbstractRObject
		implements RVector<DataType>, ExternalizableRObject {
	
	
	private DataType data;
	private int length;
	
	private String className1;
	private RStore namesAttribute;
	
	
	public JRIVectorImpl(final DataType data, final String className1, final String[] initialNames) {
		this(data, data.getLength(), className1, initialNames);
	}
	
	public JRIVectorImpl(final DataType data, final int length, final String className1, final String[] initialNames) {
		if (data == null) {
			throw new NullPointerException();
		}
		if (initialNames != null && data.getLength() >= 0 && initialNames.length != data.getLength()) {
			throw new IllegalArgumentException();
		}
		this.data = data;
		this.length = length;
		this.className1 = className1;
		if (initialNames != null) {
			this.namesAttribute = new RCharacterDataImpl(initialNames);
		}
	}
	
	public JRIVectorImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		//-- options
		final int options = in.readInt();
		final boolean customClass = ((options & RObjectFactory.O_CLASS_NAME) != 0);
		//-- special attributes
		if (customClass) {
			this.className1 = in.readUTF();
		}
		this.length = in.readInt();
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			this.namesAttribute = factory.readNames(in, flags);
		}
		//-- data
		this.data = (DataType) factory.readStore(in, flags);
		if (!customClass) {
			this.className1 = this.data.getBaseVectorRClassName();
		}
		// attributes
		if ((options & RObjectFactory.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(in, flags));
		}
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = this.className1 != null
				&& !this.className1.equals(this.data.getBaseVectorRClassName());
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		if ((flags & RObjectFactory.F_ONLY_STRUCT) == 0 && this.namesAttribute != null) {
			options |= RObjectFactory.O_WITH_NAMES;
		}
		final RList attributes = ((flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		out.writeInt(options);
		//-- special attributes
		if (customClass) {
			out.writeUTF(this.className1);
		}
		out.writeInt(this.length);
		if ((options & RObjectFactory.O_WITH_NAMES) != 0) {
			factory.writeNames(this.namesAttribute, out, flags);
		}
		//-- data
		factory.writeStore(this.data, out, flags);
		// attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			factory.writeAttributeList(attributes, out, flags);
		}
	}
	
	
	public byte getRObjectType() {
		return TYPE_VECTOR;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public RStore getNames() {
		return this.namesAttribute;
	}
	
	
	public DataType getData() {
		return this.data;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=vector, class=").append(getRClassName());
		sb.append("\n\tlength=").append(getLength());
		sb.append("\n\tdata: ");
		sb.append(this.data.toString());
		return sb.toString();
	}
	
}
