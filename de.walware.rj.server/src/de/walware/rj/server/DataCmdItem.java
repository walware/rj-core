/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;


/**
 * Command for data commands.
 */
public final class DataCmdItem extends MainCmdItem implements Externalizable {
	
	
	public static final byte EVAL_VOID = 0x01;
	public static final byte EVAL_DATA = 0x02;
	public static final byte EVAL_STRUCT = 0x03;
	public static final byte RESOLVE_DATA = 0x12;
	public static final byte RESOLVE_STRUCT = 0x13;
	
	private static final int OV_USEFACTORY =        0x00100000;
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OM_TEXTANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHTEXT;
	private static final int OV_WITHDATA =          0x20000000;
	private static final int OM_DATAANSWER =        (RjsStatus.OK << OS_STATUS) | OV_WITHDATA | OV_USEFACTORY;
	
	
	public static final String DEFAULT_FACTORY_ID = "default"; //$NON-NLS-1$
	
	
	private static RObjectFactory gDefaultFactory;
	
	private static final Map<String, RObjectFactory> gFactories = new ConcurrentHashMap<String, RObjectFactory>();
	
	private static final RObjectFactory getFactory(final String id) {
		final RObjectFactory factory = gFactories.get(id);
		if (factory != null) {
			return factory;
		}
		return gDefaultFactory;
	}
	
	public static final void registerRObjectFactory(final String id, final RObjectFactory factory) {
		if (id == null || factory == null) {
			throw new NullPointerException();
		}
		if (id.equals(DEFAULT_FACTORY_ID)) {
			throw new IllegalArgumentException();
		}
		gFactories.put(id, factory);
	}
	
	public static final void setDefaultRObjectFactory(final RObjectFactory factory) {
		gDefaultFactory = factory;
		gFactories.put(DEFAULT_FACTORY_ID, factory);
	}
	
	static {
		setDefaultRObjectFactory(RObjectFactoryImpl.INSTANCE);
	}
	
	
	private byte type;
	private int depth;
	private String text;
	private RObject rdata;
	
	private String factoryId;
	
	
	/**
	 * Constructor for automatic deserialization
	 */
	public DataCmdItem() {
	}
	
	/**
	 * Constructor for manual deserialization
	 */
	public DataCmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	public DataCmdItem(final byte type, final int options, final int depth, final String input, final String factoryId) {
		this.type = type;
		this.text = input;
		this.options = (OV_WITHTEXT | OV_WAITFORCLIENT | options);
		this.depth = depth;
		this.factoryId = (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
		assert (this.text != null);
		assert (gFactories.containsKey(this.factoryId));
	}
	
	public DataCmdItem(final byte type, final int options, final int depth, final RObject input, final String factoryId) {
		assert (input != null);
		this.type = type;
		this.rdata = input;
		this.options = (OV_WITHDATA | OV_WAITFORCLIENT | options);
		this.depth = depth;
		this.factoryId = (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
	}
	
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeByte(this.type);
		out.writeInt(this.options);
		out.writeUTF(this.factoryId);
		if ((this.options & OV_WITHTEXT) != 0) {
			out.writeUTF(this.text);
		}
		if ((this.options & OV_WITHDATA) != 0) {
			final int flags = ((this.type & 0xf) == 0x3) ? RObjectFactory.F_ONLY_STRUCT : 0;
			gDefaultFactory.writeObject(this.rdata, out, flags);
		}
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.type = in.readByte();
		this.options = in.readInt();
		this.factoryId = in.readUTF();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = in.readUTF();
		}
		if ((this.options & OV_WITHDATA) != 0) {
			final int flags = ((this.type & 0xf) == 0x3) ? RObjectFactory.F_ONLY_STRUCT : 0;
			if ((this.options & OV_USEFACTORY) != 0) {
				this.rdata = getFactory(this.factoryId).readObject(in, flags);
			}
			else {
				this.rdata = gDefaultFactory.readObject(in, flags);
			}
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_DATA_ITEM;
	}
	
	@Override
	public void setAnswer(final int status) {
		this.options = (this.options & OM_CLEARFORANSWER) | (status << OS_STATUS);
		this.text = null;
		this.rdata = null;
	}
	
	@Override
	public void setAnswer(final String dataText) {
		throw new UnsupportedOperationException(); 
	}
	
	public void setAnswer(final RObject rdata) {
		this.options = (rdata != null) ? 
				((this.options & OM_CLEARFORANSWER) | OM_DATAANSWER) : (this.options & OM_CLEARFORANSWER);
		this.rdata = rdata;
	}
	
	
	public byte getEvalType() {
		return this.type;
	}
	
	@Override
	public Object getData() {
		return this.rdata;
	}
	
	@Override
	public String getDataText() {
		return this.text;
	}
	
	public int getDepth() {
		return this.depth;
	}
	
	public void appendNext(final DataCmdItem next) {
		this.next = next;
		this.options &= OC_WAITFORCLIENT;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof DataCmdItem)) {
			return false;
		}
		final DataCmdItem otherItem = (DataCmdItem) other;
		if (getEvalType() != otherItem.getEvalType()) {
			return false;
		}
		if (this.options != otherItem.options) {
			return false;
		}
		if (((this.options & OV_WITHTEXT) != 0)
				&& !this.getDataText().equals(otherItem.getDataText())) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("DataCmdItem (type=");
		sb.append(this.type);
		sb.append(", options=0x");
		sb.append(Integer.toHexString(this.options));
		sb.append(")\n\t");
		sb.append(((this.options & OV_WITHTEXT) != 0) ? this.text : "<no text>");
		return sb.toString();
	}
	
}
