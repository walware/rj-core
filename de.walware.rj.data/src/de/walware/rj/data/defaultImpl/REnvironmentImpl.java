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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.REnvironment;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class REnvironmentImpl extends AbstractRObject
		implements REnvironment, ExternalizableRObject {
	
	
	public static REnvironmentImpl createForServer(final String name, final long handle, 
			final RObject[] initialComponents, final String[] initialNames, final int length,
			final String className1) {
		return new REnvironmentImpl(name, handle, initialComponents, initialNames, length, className1);
	}
	
	
	private String className1;
	
	private String id;
	private long handle;
	private RObject[] components;
	private int length;
	private RCharacterDataImpl namesAttribute;
	
	
	private REnvironmentImpl(final String name, final long handle, final RObject[] initialComponents, String[] initialNames, final int length, final String className1) {
		this.id = name;
		this.handle = handle;
		this.components = initialComponents;
		this.length = length;
		if (initialNames == null) {
			initialNames = new String[length];
		}
		this.namesAttribute = new RCharacterDataImpl(initialNames, length);
		this.className1 = className1;
	}
	
	public REnvironmentImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		readExternal(in, flags, factory);
	}
	
	public void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		//-- options
		final int options = in.readInt();
		//-- special attributes
		this.className1 = ((options & RObjectFactoryImpl.O_CLASS_NAME) != 0) ?
				in.readUTF() : RObject.CLASSNAME_ENV;
		//-- data
		this.handle = in.readLong();
		this.id = in.readUTF();
		this.length = in.readInt();
		this.namesAttribute = new RUniqueCharacterDataWithHashImpl(in);
		this.components = new RObject[this.length];
		for (int i = 0; i < this.length; i++) {
			this.components[i] = factory.readObject(in, flags);
		}
		//-- attributes
		if ((options & RObjectFactoryImpl.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(in, flags));
		}
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = !this.className1.equals(RObject.CLASSNAME_ENV);
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		final RList attributes = ((flags & RObjectFactoryImpl.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		out.writeInt(options);
		//-- special attributes
		if (customClass) {
			out.writeUTF(this.className1);
		}
		
		out.writeLong(this.handle);
		out.writeUTF(this.id);
		out.writeInt(this.length);
		this.namesAttribute.writeExternal(out);
		for (int i = 0; i < this.length; i++) {
			factory.writeObject(this.components[i], out, flags);
		}
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, out, flags);
		}
	}
	
	
	public final int getRObjectType() {
		return TYPE_ENV;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	
	public int getSpecialType() {
		// TODO implement including transfer
		return 0;
	}
	
	public String getEnvironmentName() {
		return this.id;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public RCharacterStore getNames() {
		return this.namesAttribute;
	}
	
	public String getName(final int idx) {
		return this.namesAttribute.getChar(idx);
	}
	
	public RStore getData() {
		return null;
	}
	
	public RObject get(final int idx) {
		return this.components[idx];
	}
	
	public boolean set(final int idx, final RObject component) {
		this.components[idx] = component;
		return true;
	}
	
	public boolean set(final String name, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		final int idx = this.namesAttribute.getIdx(name);
		if (idx >= 0) {
			this.components[idx] = component;
			return true;
		}
		return false;
	}
	
	public void insert(final int idx, final String name, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		final int[] idxs = new int[] { idx };
		this.components = prepareInsert(this.components, this.length, idxs);
		this.length++;
		if (name == null) {
			this.namesAttribute.insertNA(idxs);
		}
		else {
			this.namesAttribute.insertChar(idx, name);
		}
	}
	
	public void add(final String name, final RObject component) {
		insert(this.length, name, component);
	}
	
	public void remove(final int idx) {
		final int[] idxs = new int[] { idx };
		this.components = remove(this.components, this.length, idxs);
		this.length--;
		this.namesAttribute.remove(idxs);
	}
	
	
	public RObject get(final String name) {
		final int idx = this.namesAttribute.getIdx(name);
		if (idx >= 0) {
			return this.components[idx];
		}
		return null;
	}
	
	public boolean containsName(final String name) {
		return (this.namesAttribute.getIdx(name) >= 0);
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=environment, class=").append(getRClassName());
		sb.append("\n\tlength=").append(this.length);
		sb.append("\n\tdata: ");
		for (int i = 0; i < this.length; i++) {
			sb.append("\n$").append(this.namesAttribute.getChar(i)).append("\n");
			sb.append(this.components[i]);
		}
		return sb.toString();
	}
	
	public RObject[] toArray() {
		throw new UnsupportedOperationException();
	}
	
}
