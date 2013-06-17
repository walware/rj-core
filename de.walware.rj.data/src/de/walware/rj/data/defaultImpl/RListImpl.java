/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RListImpl extends AbstractRObject
		implements RList, ExternalizableRObject {
	
	
	private RObject[] components; // null of RObject.F_NOCHILDREN
	private int length;
	
	private String className1;
	private RCharacterDataImpl namesAttribute;
	
	
	public RListImpl(final RObject[] initialComponents, final String[] initialNames) {
		this(initialComponents, RObject.CLASSNAME_LIST, initialNames, initialComponents.length);
	}
	
	public RListImpl(final RObject[] initialComponents, final String className1, final String[] initialNames) {
		this(initialComponents, className1, initialNames, initialComponents.length);
	}
	
	public RListImpl(final RObject[] initialComponents, final String className1, String[] initialNames, final int length) {
		this.length = length;
		this.components = initialComponents;
		this.className1 = className1;
		if (initialNames == null && initialComponents != null) {
			initialNames = new String[length];
		}
		this.namesAttribute = (initialNames != null) ? createNamesStore(initialNames) : null;
	}
	
	protected RCharacterDataImpl createNamesStore(final String[] names) {
		return new RCharacterDataImpl(names, this.length);
	}
	
	public RListImpl(final RObject[] initialComponents, final RCharacterDataImpl initialNames) {
		this.components = initialComponents;
		this.length = this.components.length;
		this.namesAttribute = initialNames;
	}
	
	public RListImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		doReadExternal(io, factory);
	}
	protected final int doReadExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ?
				io.readString() : ((getRObjectType() == RObject.TYPE_DATAFRAME) ?
						RObject.CLASSNAME_DATAFRAME : RObject.CLASSNAME_LIST);
		final int length = this.length = io.readInt();
		
		if ((options & RObjectFactory.O_NO_CHILDREN) != 0) {
			this.namesAttribute = null;
			this.components = null;
		}
		else {
			this.namesAttribute = (RCharacterDataImpl) factory.readNames(io);
			//-- data
			this.components = new RObject[length];
			for (int i = 0; i < length; i++) {
				this.components[i] = factory.readObject(io);
			}
		}
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
		return options;
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		doWriteExternal(io, 0, factory);
	}
	protected final void doWriteExternal(final RJIO io, int options, final RObjectFactory factory) throws IOException {
		//-- options
		final boolean customClass = !((getRObjectType() == TYPE_DATAFRAME) ?
				this.className1.equals(RObject.CLASSNAME_DATAFRAME) : this.className1.equals(RObject.CLASSNAME_LIST));
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		final RList attributes = ((io.flags & RObjectFactory.F_WITH_ATTR) != 0) ? getAttributes() : null;
		if (attributes != null) {
			options |= RObjectFactory.O_WITH_ATTR;
		}
		if (this.components == null) {
			options |= RObjectFactory.O_NO_CHILDREN;
		}
		io.writeInt(options);
		//-- special attributes
		if (customClass) {
			io.writeString(this.className1);
		}
		io.writeInt(this.length);
		
		if (this.components != null) {
			factory.writeNames(this.namesAttribute, io);
			//-- data
			for (int i = 0; i < this.length; i++) {
				factory.writeObject(this.components[i], io);
			}
		}
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, io);
		}
	}
	
	
	public byte getRObjectType() {
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
		if (this.namesAttribute != null) {
			return this.namesAttribute.getChar(idx);
		}
		return null;
	}
	
	public final RObject get(final int idx) {
		return this.components[idx];
	}
	
	public final RObject get(final String name) {
		if (this.namesAttribute != null) {
			final int idx = this.namesAttribute.indexOf(name);
			if (idx >= 0) {
				return this.components[idx];
			}
		}
		return null;
	}
	
	public final RObject[] toArray() {
		final RObject[] array = new RObject[this.length];
		System.arraycopy(this.components, 0, array, 0, this.length);
		return array;
	}
	
	public final RStore getData() {
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
		final int idx = this.namesAttribute.indexOf(name);
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
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=list, class=").append(getRClassName());
		sb.append("\n\tlength=").append(this.length);
		if (this.components != null) {
			sb.append("\n\tdata: ");
			for (int i = 0; i < this.length; i++) {
				if (this.namesAttribute == null || this.namesAttribute.isNA(i)) {
					sb.append("\n[[").append(i).append("]]\n");
				}
				else {
					sb.append("\n$").append(this.namesAttribute.getChar(i)).append("\n");
				}
				sb.append(this.components[i]);
			}
		}
		else {
			sb.append("\n<NODATA/>");
		}
		return sb.toString();
	}
	
}
