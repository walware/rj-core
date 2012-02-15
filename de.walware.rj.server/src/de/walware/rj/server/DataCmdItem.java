/*******************************************************************************
 * Copyright (c) 2008-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;


/**
 * Command item for main loop data exchange/evaluation
 */
public final class DataCmdItem extends MainCmdItem {
	
	
	public static final byte EVAL_VOID = 0x01;
	public static final byte EVAL_DATA = 0x02;
	public static final byte RESOLVE_DATA = 0x04;
	public static final byte ASSIGN_DATA = 0x06;
	public static final byte FIND_DATA = 0x08;
	
	private static final int OV_WITHTEXT =                  0x01000000;
	private static final int OV_WITHDATA =                  0x02000000;
	private static final int OV_WITHRHO =                   0x04000000;
	private static final int OV_WITHSTATUS =                0x08000000;
	
	
	public static final String DEFAULT_FACTORY_ID = "default"; //$NON-NLS-1$
	
	
	static RObjectFactory gDefaultFactory;
	
	static final Map<String, RObjectFactory> gFactories = new ConcurrentHashMap<String, RObjectFactory>();
	
	private static final RObjectFactory getFactory(final String id) {
		final RObjectFactory factory = gFactories.get(id);
		if (factory != null) {
			return factory;
		}
		return gDefaultFactory;
	}
	
	static {
		RjsComConfig.setDefaultRObjectFactory(RObjectFactoryImpl.INSTANCE);
	}
	
	
	private byte op;
	
	private byte depth;
	private String text;
	private RObject rdata;
	private RObject rho;
	
	private String factoryId;
	
	private RjsStatus status;
	
	
	/**
	 * Constructor for operations with returned data
	 * <ul>
	 *     <li>evalData operation (send text, load data)</li>
	 *     <li>evalData (send call, load data)</li>
	 * </ul>
	 */
	public DataCmdItem(final byte type, final int options, final byte depth,
			final String text, final RObject data, final RObject rho,
			final String factoryId) {
		assert (text != null);
		assert (factoryId == null || gFactories.containsKey(factoryId));
		this.op = type;
		this.text = text;
		this.options = (OV_WITHTEXT | OV_WAITFORCLIENT | (options & OM_CUSTOM));
		if (data != null) {
			this.rdata = data;
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.rho = rho;
			this.options |= OV_WITHRHO;
		}
		this.depth = depth;
		this.factoryId = (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
	}
	
	/**
	 * Constructor for operations without returned data:
	 * <ul>
	 *     <li>assignData (send text and data)</li>
	 *     <li>evalVoid (send call)</li>
	 *     <li>evalVoid (send expr, data=null)</li>
	 * </ul>
	 */
	public DataCmdItem(final byte type, final int options,
			final String text, final RObject data, final RObject rho) {
		assert (text != null);
		this.op = type;
		this.text = text;
		this.options = ((OV_WITHTEXT | OV_WAITFORCLIENT) | (options & OM_CUSTOM));
		if (data != null) {
			this.rdata = data;
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.rho = rho;
			this.options |= OV_WITHRHO;
		}
		this.factoryId = "";
	}
	
	
	/**
	 * Constructor for deserialization
	 */
	public DataCmdItem(final RJIO in) throws IOException {
		this.requestId = in.readInt();
		this.op = in.readByte();
		this.options = in.readInt();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status = new RjsStatus(in);
			return;
		}
		this.depth = in.readByte();
		this.factoryId = in.readString();
		if ((this.options & OV_WITHTEXT) != 0) {
			this.text = in.readString();
		}
		if ((this.options & OV_WITHDATA) != 0) {
			if ((this.options & OV_ANSWER) != 0) {
				in.flags = (this.options & 0xff);
				this.rdata = getFactory(this.factoryId).readObject(in);
			}
			else {
				in.flags = 0;
				this.rdata = gDefaultFactory.readObject(in);
			}
		}
		if ((this.options & OV_WITHRHO) != 0) {
			if ((this.options & OV_ANSWER) != 0) {
				in.flags = 0;
				this.rho = getFactory(this.factoryId).readObject(in);
			}
			else {
				in.flags = 0;
				this.rho = gDefaultFactory.readObject(in);
			}
		}
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeByte(this.op);
		out.writeInt(this.options);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(out);
			return;
		}
		out.writeByte(this.depth);
		out.writeString(this.factoryId);
		if ((this.options & OV_WITHTEXT) != 0) {
			out.writeString(this.text);
		}
		if ((this.options & OV_WITHDATA) != 0) {
			if ((this.options & OV_ANSWER) != 0) {
				out.flags = (this.options & 0xff);
				gDefaultFactory.writeObject(this.rdata, out);
			}
			else {
				out.flags = 0;
				gDefaultFactory.writeObject(this.rdata, out);
			}
		}
		if ((this.options & OV_WITHRHO) != 0) {
			out.flags = 0;
			gDefaultFactory.writeObject(this.rho, out);
		}
	}
	
	
	@Override
	public byte getCmdType() {
		return T_DATA_ITEM;
	}
	
	
	@Override
	public void setAnswer(final RjsStatus status) {
		assert (status != null);
		if (status == RjsStatus.OK_STATUS) {
			this.options = (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.status = null;
			this.text = null;
			this.rdata = null;
			this.rho = null;
		}
		else {
			this.options = ((this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS));
			this.status = status;
			this.text = null;
			this.rdata = null;
			this.rho = null;
		}
	}
	
	public void setAnswer(final RObject rdata, final RObject rho) {
		this.options = ((this.options & OM_CLEARFORANSWER) | OV_ANSWER);
		if (rdata != null) {
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.options |= OV_WITHRHO;
		}
		this.status = null;
		this.text = null;
		this.rdata = rdata;
		this.rho = rho;
	}
	
	
	@Override
	public byte getOp() {
		return this.op;
	}
	
	@Override
	public boolean isOK() {
		return (this.status == null || this.status.getSeverity() == RjsStatus.OK);
	}
	
	@Override
	public RjsStatus getStatus() {
		return this.status;
	}
	
	public RObject getData() {
		return this.rdata;
	}
	
	public RObject getRho() {
		return this.rho;
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
		if (getOp() != otherItem.getOp()) {
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
		sb.append("DataCmdItem ");
		switch (this.op) {
		case EVAL_VOID:
			sb.append("EVAL_VOID");
			break;
		case EVAL_DATA:
			sb.append("EVAL_DATA");
			break;
		case RESOLVE_DATA:
			sb.append("RESOLVE_DATA");
			break;
		case ASSIGN_DATA:
			sb.append("ASSIGN_DATA");
			break;
		case FIND_DATA:
			sb.append("FIND_DATA");
			break;
		default:
			sb.append(this.op);
			break;
		}
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if ((this.options & OV_WITHTEXT) != 0) {
			sb.append("\n<TEXT>\n");
			sb.append(this.text);
			sb.append("\n</TEXT>");
		}
		else {
			sb.append("\n<TEXT/>");
		}
		if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n");
			sb.append(this.rdata.toString());
			sb.append("\n</DATA>");
		}
		else {
			sb.append("\n<DATA/>");
		}
		if ((this.options & OV_WITHRHO) != 0) {
			sb.append("\n<RHO>\n");
			sb.append(this.rho.toString());
			sb.append("\n</RHO>");
		}
		return sb.toString();
	}
	
}
