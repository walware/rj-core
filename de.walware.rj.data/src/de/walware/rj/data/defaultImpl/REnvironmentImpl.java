/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.REnvironment;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class REnvironmentImpl extends AbstractRObject
		implements REnvironment, ExternalizableRObject {
	
	
	private String className1;
	
	private String environmentName;
	private long handle;
	
	private int length;
	private RObject[] components;
	
	private RCharacterDataImpl namesAttribute;
	
	
	protected REnvironmentImpl(final String name, final long handle, final RObject[] initialComponents, String[] initialNames, final int length, final String className1) {
		this.environmentName = name;
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
		this.environmentName = io.readString();
		final int l = this.length = (int) io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK));
		
		if ((options & RObjectFactory.O_NO_CHILDREN) != 0) {
			this.namesAttribute = null;
			this.components = null;
		}
		else {
			this.namesAttribute = new RUniqueCharacterDataWithHashImpl(io, l);
			this.components = new RObject[l];
			for (int i = 0; i < l; i++) {
				this.components[i] = factory.readObject(io);
			}
		}
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int l = this.length;
		//-- options
		int options = io.getVULongGrade(l);
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
		io.writeString(this.environmentName);
		io.writeVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK), l);
		
		if (this.components != null) {
			this.namesAttribute.writeExternal(io);
			//-- data
			for (int i = 0; i < l; i++) {
				factory.writeObject(this.components[i], io);
			}
		}
		//-- attributes
		if (attributes != null) {
			factory.writeAttributeList(attributes, io);
		}
	}
	
	
	@Override
	public final byte getRObjectType() {
		return TYPE_ENV;
	}
	
	@Override
	public String getRClassName() {
		return this.className1;
	}
	
	
	@Override
	public int getSpecialType() {
		// TODO implement including transfer
		return 0;
	}
	
	@Override
	public String getEnvironmentName() {
		return this.environmentName;
	}
	
	@Override
	public long getHandle() {
		return this.handle;
	}
	
	
	@Override
	public long getLength() {
		return this.length;
	}
	
	@Override
	public RCharacterStore getNames() {
		return this.namesAttribute;
	}
	
	@Override
	public String getName(final int idx) {
		return this.namesAttribute.getChar(idx);
	}
	
	@Override
	public String getName(final long idx) {
		return this.namesAttribute.getChar(idx);
	}
	
	@Override
	public RObject get(final int idx) {
		return this.components[idx];
	}
	
	@Override
	public RObject get(final long idx) {
		if (idx < 0 || idx >= Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.components[(int) idx];
	}
	
	@Override
	public RObject get(final String name) {
		final int idx = this.namesAttribute.indexOf(name, 0);
		if (idx >= 0) {
			return this.components[idx];
		}
		return null;
	}
	
	public RObject[] toArray() {
		final RObject[] array = new RObject[this.length];
		System.arraycopy(this.components, 0, array, 0, this.length);
		return array;
	}
	
	@Override
	public RStore<?> getData() {
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
		final int idx = this.namesAttribute.indexOf(name, 0);
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
