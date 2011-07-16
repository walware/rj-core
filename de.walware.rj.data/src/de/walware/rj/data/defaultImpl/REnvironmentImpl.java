/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
import de.walware.rj.data.REnvironment;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class REnvironmentImpl extends AbstractRObject
		implements REnvironment, ExternalizableRObject {
	
	
	private String id;
	private long handle;
	
	private RObject[] components;
	private int length;
	
	private String className1;
	private RCharacterDataImpl namesAttribute;
	
	
	protected REnvironmentImpl(final String name, final long handle, final RObject[] initialComponents, String[] initialNames, final int length, final String className1) {
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
	
	public REnvironmentImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		final int options = io.readInt();
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ?
				io.readString() : RObject.CLASSNAME_ENV;
		//-- data
		this.handle = io.readLong();
		this.id = io.readString();
		final int length = this.length = io.readInt();
		
		if ((options & RObjectFactory.O_NO_CHILDREN) != 0) {
			this.namesAttribute = null;
			this.components = null;
		}
		else {
			this.namesAttribute = new RUniqueCharacterDataWithHashImpl(io);
			this.components = new RObject[length];
			for (int i = 0; i < length; i++) {
				this.components[i] = factory.readObject(io);
			}
		}
		//-- attributes
		if ((options & RObjectFactory.F_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		//-- options
		int options = 0;
		final boolean customClass = !this.className1.equals(RObject.CLASSNAME_ENV);
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
		
		io.writeLong(this.handle);
		io.writeString(this.id);
		io.writeInt(this.length);
		
		if (this.components != null) {
			this.namesAttribute.writeExternal(io);
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
	
	
	public final byte getRObjectType() {
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
	
	public RObject get(final int idx) {
		return this.components[idx];
	}
	
	public RObject[] toArray() {
		final RObject[] array = new RObject[this.length];
		System.arraycopy(this.components, 0, array, 0, this.length);
		return array;
	}
	
	public RStore getData() {
		return null;
	}
	
	public boolean set(final int idx, final RObject component) {
		this.components[idx] = component;
		return true;
	}
	
	public boolean set(final String name, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		final int idx = this.namesAttribute.indexOf(name);
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
		final int idx = this.namesAttribute.indexOf(name);
		if (idx >= 0) {
			return this.components[idx];
		}
		return null;
	}
	
	public boolean containsName(final String name) {
		return (this.namesAttribute.indexOf(name) >= 0);
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=environment, class=").append(getRClassName());
		sb.append("\n\tlength=").append(this.length);
		if (this.components != null) {
			sb.append("\n\tdata: ");
			for (int i = 0; i < this.length; i++) {
				sb.append("\n$").append(this.namesAttribute.getChar(i)).append("\n");
				sb.append(this.components[i]);
			}
		}
		else {
			sb.append("\n<NODATA/>");
		}
		return sb.toString();
	}
	
}
