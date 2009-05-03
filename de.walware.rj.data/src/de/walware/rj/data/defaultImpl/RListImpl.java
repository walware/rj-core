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
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RListImpl extends AbstractRObject
		implements RList, ExternalizableRObject {
	
	
	private RObject[] components;
	private int length;
	
	private String className1;
	private RCharacterDataImpl namesAttribute;
	
	
	public RListImpl(final RObject[] initialComponents, final String className1, final String[] initialNames) {
		this(initialComponents, className1, initialNames, initialComponents.length);
	}
	
	public RListImpl(final RObject[] initialComponents, final String className1, String[] initialNames, final int length) {
		this.length = length;
		this.components = initialComponents;
		this.className1 = (className1 != null) ? className1 : RObject.CLASSNAME_LIST;
		if (initialNames == null) {
			initialNames = new String[length];
		}
		this.namesAttribute = new RCharacterDataImpl(initialNames, length);
	}
	
	public RListImpl(final RObject[] initialComponents, final RCharacterDataImpl initialNames) {
		this.components = initialComponents;
		this.length = this.components.length;
		this.namesAttribute = initialNames;
	}
	
	public RListImpl(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		doReadExternal(in, flags, factory);
	}
	
	public final void readExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		doReadExternal(in, flags, factory);
	}
	protected int doReadExternal(final ObjectInput in, final int flags, final RObjectFactory factory) throws IOException, ClassNotFoundException {
		//-- options
		final int options = in.readInt();
		//-- special attributes
		this.className1 = ((options & RObjectFactoryImpl.O_CLASS_NAME) != 0) ?
				in.readUTF() : ((getRObjectType() == RObject.TYPE_DATAFRAME) ?
						RObject.CLASSNAME_DATAFRAME : RObject.CLASSNAME_LIST);
		this.namesAttribute = new RCharacterDataImpl(in);
		//-- data
		this.length = in.readInt();
		this.components = new RObject[this.length];
		for (int i = 0; i < this.length; i++) {
			this.components[i] = factory.readObject(in, flags);
		}
		//-- attributes
		if ((options & RObjectFactoryImpl.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(in, flags));
		}
		return options;
	}
	
	public void writeExternal(final ObjectOutput out, final int flags, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = !((getRObjectType() == TYPE_DATAFRAME) ?
				this.className1.equals(RObject.CLASSNAME_DATAFRAME) : this.className1.equals(RObject.CLASSNAME_LIST));
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
		this.namesAttribute.writeExternal(out);
		//-- data
		out.writeInt(this.length);
		for (int i = 0; i < this.length; i++) {
			factory.writeObject(this.components[i], out, flags);
		}
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, out, flags);
		}
	}
	
	
	public int getRObjectType() {
		return TYPE_LIST;
	}
	
	public final String getRClassName() {
		return this.className1;
	}
	
	
	public int getLength() {
		return this.length;
	}
	
	public final RCharacterStore getNames() {
		return this.namesAttribute;
	}
	
	public final String getName(final int idx) {
		return this.namesAttribute.getChar(idx);
	}
	
	public final RStore getData() {
		return null;
	}
	
	public final RObject get(final int idx) {
		return this.components[idx];
	}
	
	public final RObject get(final String name) {
		final int idx = this.namesAttribute.getIdx(name);
		if (idx >= 0) {
			return this.components[idx];
		}
		return null;
	}
	
	public boolean set(final int idx, final RObject component) {
		this.components[idx] = component;
		return true;
	}
	
	public final boolean set(final String name, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		final int idx = this.namesAttribute.getIdx(name);
		if (idx >= 0) {
			set(idx, component);
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
	
	
	public RObject[] toArray() {
		final RObject[] array = new RObject[this.length];
		System.arraycopy(this.components, 0, array, 0, this.length);
		return array;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=list, class=").append(getRClassName());
		sb.append("\n\tlength=").append(this.length);
		sb.append("\n\tdata: ");
		for (int i = 0; i < this.length; i++) {
			if (this.namesAttribute.isNA(i)) {
				sb.append("\n[[").append(i).append("]]\n");
			}
			else {
				sb.append("\n$").append(this.namesAttribute.getChar(i)).append("\n");
			}
			sb.append(this.components[i]);
		}
		return sb.toString();
	}
	
}
