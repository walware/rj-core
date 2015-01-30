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
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RListFixLongImpl extends AbstractRObject
		implements RList, ExternalizableRObject {
	
	
	public static final int SEGMENT_LENGTH = AbstractRData.DEFAULT_LONG_DATA_SEGMENT_LENGTH;
	
	
	private final long length;
	private final RObject[][] components; // null of RObject.F_NOCHILDREN
	
	private final String className1;
	private final RCharacterStore namesAttribute;
	
	
	public RListFixLongImpl(final RObject[][] initialComponents, final String[][] initialNames) {
		this(initialComponents, RObject.CLASSNAME_LIST, initialNames);
	}
	
	public RListFixLongImpl(final RObject[][] initialComponents, final String className1, final String[][] initialNames) {
		this.length = check2dArrayLength(initialComponents, SEGMENT_LENGTH);
		this.components = initialComponents;
		this.className1 = className1;
		if (initialNames != null) {
			this.namesAttribute = new RCharacterDataFixLongImpl(initialNames);
			if (this.namesAttribute.getLength() != this.length) {
				throw new IllegalArgumentException("Different length of components and names.");
			}
		}
		else {
			this.namesAttribute = null;
		}
	}
	
	public RListFixLongImpl(final RObject[][] initialComponents, final RCharacterDataImpl initialNames) {
		this.components = initialComponents;
		this.length = this.components.length;
		this.className1 = RObject.CLASSNAME_LIST;
		this.namesAttribute = initialNames;
	}
	
	public RListFixLongImpl(final long length, final String className1) {
		this.length = length;
		this.className1 = className1;
		this.components = null;
		this.namesAttribute = null;
	}
	
	public RListFixLongImpl(final RJIO io, final RObjectFactory factory, final int options) throws IOException {
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ?
				io.readString() : ((getRObjectType() == RObject.TYPE_DATAFRAME) ?
						RObject.CLASSNAME_DATAFRAME : RObject.CLASSNAME_LIST);
		final long l = this.length = io.readVULong((byte) (options & RObjectFactory.O_LENGTHGRADE_MASK));
		
		if ((options & RObjectFactory.O_NO_CHILDREN) != 0) {
			this.namesAttribute = null;
			this.components = null;
		}
		else {
			this.namesAttribute = (RCharacterStore) factory.readNames(io, l);
			//-- data
			this.components = new2dRObjectArray(options, SEGMENT_LENGTH);
			for (int i = 0; i < this.components.length; i++) {
				final RObject[] segment = this.components[i];
				for (int j = 0; j < segment.length; j++) {
					segment[j] = factory.readObject(io);
				}
			}
		}
		//-- attributes
		if ((options & RObjectFactory.O_WITH_ATTR) != 0) {
			setAttributes(factory.readAttributeList(io));
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		doWriteExternal(io, 0, factory);
	}
	protected final void doWriteExternal(final RJIO io, int options, final RObjectFactory factory) throws IOException {
		final long l = this.length;
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
			for (int i = 0; i < this.components.length; i++) {
				final RObject[] segment = this.components[i];
				for (int j = 0; j < segment.length; j++) {
					factory.writeObject(segment[j], io);
				}
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
		return this.components[idx / SEGMENT_LENGTH][idx % SEGMENT_LENGTH];
	}
	
	@Override
	public final RObject get(final long idx) {
		return this.components[(int) (idx / SEGMENT_LENGTH)][(int) (idx % SEGMENT_LENGTH)];
	}
	
	@Override
	public final RObject get(final String name) {
		if (this.namesAttribute != null) {
			final long idx = this.namesAttribute.indexOf(name);
			if (idx >= 0) {
				return get(idx);
			}
		}
		return null;
	}
	
	@Override
	public final RStore<?> getData() {
		return null;
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
