/*******************************************************************************
 * Copyright (c) 2008-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
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
 * Command item for main loop data exchange/evaluation
 */
public final class DataCmdItem extends MainCmdItem implements Externalizable {
	
	
	public static final byte EVAL_VOID = 0x01;
	public static final byte EVAL_DATA = 0x02;
	public static final byte EVAL_STRUCT = 0x03;
	public static final byte RESOLVE_DATA = 0x12;
	public static final byte RESOLVE_STRUCT = 0x13;
	public static final byte ASSIGN_DATA = 0x22;
	
	private static final int OV_USEFACTORY =        0x00100000;
	
	private static final int OV_WITHTEXT =          0x10000000;
	private static final int OV_WITHDATA =          0x20000000;
	private static final int OV_WITHSTATUS =        0x40000000;
	private static final int OM_DATAANSWER =        OV_WITHDATA | OV_USEFACTORY;
	private static final int OM_STATUSANSWER =      OV_WITHSTATUS;
	
	
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
	
	/**
	 * Registers an additional RObject factory
	 * 
	 * Factory registration is valid for the current VM.
	 */
	public static final void registerRObjectFactory(final String id, final RObjectFactory factory) {
		if (id == null || factory == null) {
			throw new NullPointerException();
		}
		if (id.equals(DEFAULT_FACTORY_ID)) {
			throw new IllegalArgumentException();
		}
		gFactories.put(id, factory);
	}
	
	/**
	 * Sets the default RObject factory
	 * 
	 * Factory registration is valid for the current VM.
	 */
	public static final void setDefaultRObjectFactory(final RObjectFactory factory) {
		gDefaultFactory = factory;
		gFactories.put(DEFAULT_FACTORY_ID, factory);
	}
	
	static {
		setDefaultRObjectFactory(RObjectFactoryImpl.INSTANCE);
	}
	
	
	private byte type;
	
	private byte depth;
	private String text;
	private RObject rdata;
	
	private String factoryId;
	
	private RjsStatus status;
	
	
	/**
	 * Constructor for evalData operation (send text, load data)
	 */
	public DataCmdItem(final byte type, final int options, final byte depth, final String text, final String factoryId) {
		assert (text != null);
		assert (factoryId == null || gFactories.containsKey(factoryId));
		this.type = type;
		this.text = text;
		this.rdata = null;
		this.options = (OV_WITHTEXT | options);
		this.depth = depth;
		this.factoryId = (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
	}
	
//	public DataCmdItem(final byte type, final int options, final int depth, final RObject input, final String factoryId) {
//		assert (input != null);
//		this.type = type;
//		this.text = null;
//		this.rdata = input;
//		this.options = (OV_WITHDATA | options);
//		this.depth = depth;
//		this.factoryId = (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
//		assert (gFactories.containsKey(this.factoryId));
//	}
	
	/**
	 * Constructor for assignData operation (send text and data)
	 */
	public DataCmdItem(final byte type, final int options, final String text, final RObject data) {
		assert (text != null);
		assert (data != null);
		this.type = type;
		this.text = text;
		this.rdata = data;
		this.options = ((OV_WITHTEXT | OV_WITHDATA) | options);
		this.factoryId = "";
	}
	
	/**
	 * Constructor for evalVoid operation (send text)
	 */
	public DataCmdItem(final byte type, final int options, final String text) {
		assert (text != null);
		this.type = type;
		this.text = text;
		this.rdata = null;
		this.options = ((OV_WITHTEXT) | options);
		this.factoryId = "";
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public DataCmdItem() {
	}
	
	/**
	 * Constructor for deserialization
	 */
	public DataCmdItem(final ObjectInput in) throws IOException, ClassNotFoundException {
		readExternal(in);
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeByte(this.type);
		out.writeInt(this.options);
		out.writeByte(this.requestId);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(out);
			return;
		}
		out.writeByte(this.depth);
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
		this.requestId = in.readByte();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status = new RjsStatus(in);
			return;
		}
		this.depth = in.readByte();
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
	public void setAnswer(final RjsStatus status) {
		if (status == RjsStatus.OK_STATUS) {
			this.options = (this.options & OM_CLEARFORANSWER);
			this.status = null;
		}
		else {
			this.options = ((this.options & OM_CLEARFORANSWER) | OM_STATUSANSWER);
			this.status = status;
		}
	}
	
	@Override
	public void setAnswer(final String dataText) {
		throw new UnsupportedOperationException(); 
	}
	
	public void setAnswer(final RObject rdata) {
		this.options = (rdata != null) ? 
				((this.options & OM_CLEARFORANSWER) | OM_DATAANSWER) : (this.options & OM_CLEARFORANSWER);
		this.status = null;
		this.rdata = rdata;
	}
	
	
	public byte getEvalType() {
		return this.type;
	}
	
	@Override
	public boolean isOK() {
		return (this.status == null || this.status.getSeverity() == RjsStatus.OK);
	}
	
	@Override
	public RjsStatus getStatus() {
		return this.status;
	}
	
	@Override
	public RObject getData() {
		return this.rdata;
	}
	
	@Override
	public String getDataText() {
		return this.text;
	}
	
	public byte getDepth() {
		return this.depth;
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
		switch (this.type) {
		case EVAL_VOID:
			sb.append("EVAL_VOID");
			break;
		case EVAL_DATA:
			sb.append("EVAL_DATA");
			break;
		case EVAL_STRUCT:
			sb.append("EVAL_STRUCT");
			break;
		case RESOLVE_DATA:
			sb.append("RESOLVE_DATA");
			break;
		case RESOLVE_STRUCT:
			sb.append("RESOLVE_STRUCT");
			break;
		case ASSIGN_DATA:
			sb.append("ASSIGN_DATA");
			break;
		default:
			sb.append(this.type);
		}
		sb.append(", options=0x");
		sb.append(Integer.toHexString(this.options));
		sb.append(")");
		if ((this.options & OV_WITHTEXT) != 0) {
			sb.append("\n<TEXT>\n");
			sb.append(this.text);
			sb.append("\n</TEXT>");
		}
		else {
			sb.append("\n<TEXT />");
		}
		if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n");
			sb.append(this.rdata.toString());
			sb.append("\n</DATA>");
		}
		return sb.toString();
	}
	
}
