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

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RListImpl extends AbstractRObject
		implements RList, ExternalizableRObject {
	
	
	private int length;
	private RObject[] components; // null of RObject.F_NOCHILDREN
	
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
	
	public RListImpl(final RJIO io, final RObjectFactory factory, final int options) throws IOException {
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ?
				io.readString() : ((getRObjectType() == RObject.TYPE_DATAFRAME) ?
						RObject.CLASSNAME_DATAFRAME : RObject.CLASSNAME_LIST);
		final int l = this.length = checkShortLength(
				io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK)) );
		
		if ((options & RObjectFactory.O_NO_CHILDREN) != 0) {
			this.namesAttribute = null;
			this.components = null;
		}
		else {
			this.namesAttribute = (RCharacterDataImpl) factory.readNames(io, l);
			//-- data
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
		doWriteExternal(io, factory, 0);
	}
	protected final void doWriteExternal(final RJIO io, final RObjectFactory factory, int options) throws IOException {
		final int l = this.length;
		//-- options
		options |= io.getVULongGrade(l);
		if (!this.className1.equals(getDefaultRClassName())) {
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
		if ((options & RObjectFactory.O_CLASS_NAME) != 0) {
			io.writeString(this.className1);
		}
		io.writeVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK), l);
		
		if ((options & RObjectFactory.O_NO_CHILDREN) == 0) {
			factory.writeNames(this.namesAttribute, io);
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
	public byte getRObjectType() {
		return TYPE_LIST;
	}
	
	protected String getDefaultRClassName() {
		return RObject.CLASSNAME_LIST;
	}
	
	@Override
	public final String getRClassName() {
		return this.className1;
	}
	
	
	protected int length() {
		return this.length;
	}
	
	@Override
	public long getLength() {
		return this.length;
	}
	
	@Override
	public final RCharacterStore getNames() {
		return this.namesAttribute;
	}
	
	@Override
	public final String getName(final int idx) {
		if (this.namesAttribute != null) {
			return this.namesAttribute.getChar(idx);
		}
		return null;
	}
	
	@Override
	public final String getName(final long idx) {
		if (this.namesAttribute != null) {
			return this.namesAttribute.getChar(idx);
		}
		return null;
	}
	
	@Override
	public final RObject get(final int idx) {
		return this.components[idx];
	}
	
	@Override
	public final RObject get(final long idx) {
		if (idx < 0 || idx >= Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException(Long.toString(idx));
		}
		return this.components[(int) idx];
	}
	
	@Override
	public final RObject get(final String name) {
		if (this.namesAttribute != null) {
			final int idx = this.namesAttribute.indexOf(name, 0);
			if (idx >= 0) {
				return this.components[idx];
			}
		}
		return null;
	}
	
	@Override
	public final RStore getData() {
		return null;
	}
	
	
	public boolean set(final int idx, final RObject component) {
		this.components[idx] = component;
		return true;
	}
	
//	public boolean set(final long idx, final RObject component) {
//		if (idx < 0 || idx >= Integer.MAX_VALUE) {
//			throw new IndexOutOfBoundsException(Long.toString(idx));
//		}
//		this.components[(int) idx] = component;
//		return true;
//	}
	
	public final boolean set(final String name, final RObject component) {
		if (component == null) {
			throw new NullPointerException();
		}
		final int idx = this.namesAttribute.indexOf(name, 0);
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
				if (i > 100) {
					sb.append("\n... ");
					break;
				}
				if (this.namesAttribute == null || this.namesAttribute.isNA(i)) {
					sb.append("\n[[").append((i + 1)).append("]]\n");
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
