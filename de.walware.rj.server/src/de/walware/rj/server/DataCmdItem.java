/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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
	
	
	public static final class Operation {
		
		public static final byte NONE= 0;
		public static final byte EXPR= 1;
		public static final byte POINTER= 2;
		public static final byte FCALL= 3;
		public static final byte RDATA= 4;
		
		public final byte op;
		
		public final String name;
		
		public final byte source;
		public final byte target;
		public final boolean returnData;
		
		private final boolean reqSourceExpr;
		private final boolean reqRData;
		private final boolean reqTargetExpr;
		
		private Operation(final String name, final int op,
				final byte source, final byte target, final boolean returnData) {
			this.op= (byte) op;
			this.name= name;
			this.source= source;
			this.target= target;
			this.returnData= returnData;
			
			this.reqSourceExpr= (source == EXPR || source == POINTER || source == FCALL);
			this.reqRData= (source == FCALL || source == RDATA);
			this.reqTargetExpr= (target == EXPR || target == POINTER);
		}
		
		
		@Override
		public String toString() {
			return this.name;
		}
		
	}
	
	
	public static Operation EVAL_EXPR_VOID= new Operation("EVAL_EXPR_VOID", 1, //$NON-NLS-1$
			Operation.EXPR, Operation.NONE, false);
	public static Operation EVAL_FCALL_VOID= new Operation("EVAL_FCALL_VOID", 2, //$NON-NLS-1$
			Operation.FCALL, Operation.NONE, false);
	
	public static Operation EVAL_EXPR_DATA= new Operation("EVAL_EXPR_DATA", 3, //$NON-NLS-1$
			Operation.EXPR, Operation.NONE, true);
	public static Operation EVAL_FCALL_DATA= new Operation("EVAL_FCALL_DATA", 4, //$NON-NLS-1$
			Operation.FCALL, Operation.NONE, true);
	public static Operation RESOLVE_DATA= new Operation("RESOLVE_DATA", 5, // EVAL_REF_DATA //$NON-NLS-1$
			Operation.POINTER, Operation.NONE, true);
	
	public static Operation ASSIGN_DATA= new Operation("ASSIGN_DATA", 6, //$NON-NLS-1$
			Operation.RDATA, Operation.EXPR, false);
	public static Operation ASSIGN_FCALL= new Operation("ASSIGN_FCALL", 7, //$NON-NLS-1$
			Operation.FCALL, Operation.EXPR, false);
	
	public static Operation FIND_DATA= new Operation("FIND_DATA", 8, //$NON-NLS-1$
			Operation.EXPR, Operation.NONE, true);
	
	
	private static final Operation[] OPERATIONS= new Operation[9];
	
	private static final void addOp(final Operation operation) {
		if (OPERATIONS[operation.op] != null) {
			throw new IllegalArgumentException();
		}
		OPERATIONS[operation.op]= operation;
	}
	
	private static final Operation getOperation(final byte op) {
		if (op <= 0 || op >= OPERATIONS.length) {
			throw new UnsupportedOperationException("data op: " + op); //$NON-NLS-1$
		}
		return OPERATIONS[op];
	}
	
	static {
		addOp(EVAL_EXPR_VOID);
		addOp(EVAL_FCALL_VOID);
		addOp(EVAL_EXPR_DATA);
		addOp(EVAL_FCALL_DATA);
		addOp(RESOLVE_DATA);
		addOp(ASSIGN_DATA);
		addOp(ASSIGN_FCALL);
		addOp(FIND_DATA);
	}
	
	
	private static final int OV_WITHDATA=                  0x02000000;
	private static final int OV_WITHRHO=                   0x04000000;
	private static final int OV_WITHSTATUS=                0x08000000;
	
	
	public static final String DEFAULT_FACTORY_ID= "default"; //$NON-NLS-1$
	
	
	static RObjectFactory gDefaultFactory;
	
	static final Map<String, RObjectFactory> gFactories= new ConcurrentHashMap<String, RObjectFactory>();
	
	private static final RObjectFactory getFactory(final String id) {
		final RObjectFactory factory= gFactories.get(id);
		if (factory != null) {
			return factory;
		}
		return gDefaultFactory;
	}
	
	static {
		RjsComConfig.setDefaultRObjectFactory(RObjectFactoryImpl.INSTANCE);
	}
	
	
	private final Operation operation;
	
	private byte depth;
	private String sourceExpr;
	private String targetExpr;
	private RObject rdata;
	private RObject rho;
	
	private String factoryId;
	
	private RjsStatus status;
	
	
	/**
	 * Constructor for operations with returned data
	 */
	public DataCmdItem(final Operation op, final int options, final byte depth,
			final String sourceExpr, final RObject data, final String targetExpr,
			final RObject rho,
			final String factoryId) {
		assert (op.reqSourceExpr == (sourceExpr != null));
		assert (op.reqRData == (data != null));
		assert (op.reqTargetExpr == (targetExpr != null));
		assert (factoryId == null || gFactories.containsKey(factoryId));
		this.operation= op;
		this.targetExpr= targetExpr;
		this.sourceExpr= sourceExpr;
		this.options= (OV_WAITFORCLIENT | (options & OM_CUSTOM));
		if (data != null) {
			this.rdata= data;
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.rho= rho;
			this.options |= OV_WITHRHO;
		}
		this.depth= depth;
		this.factoryId= (factoryId != null) ? factoryId : DEFAULT_FACTORY_ID;
	}
	
	/**
	 * Constructor for operations without returned data:
	 */
	public DataCmdItem(final Operation op, final int options,
			final String sourceExpr, final RObject data, final String targetExpr,
			final RObject rho) {
		assert (op.reqSourceExpr == (sourceExpr != null));
		assert (op.reqRData == (data != null));
		assert (op.reqTargetExpr == (targetExpr != null));
		this.operation= op;
		this.sourceExpr= sourceExpr;
		this.options= (OV_WAITFORCLIENT | (options & OM_CUSTOM));
		if (data != null) {
			this.rdata= data;
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.rho= rho;
			this.options |= OV_WITHRHO;
		}
		this.factoryId= "";
	}
	
	
	/**
	 * Constructor for deserialization
	 */
	public DataCmdItem(final RJIO in) throws IOException {
		this.requestId= in.readInt();
		this.operation= getOperation(in.readByte());
		this.options= in.readInt();
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status= new RjsStatus(in);
			return;
		}
		this.depth= in.readByte();
		this.factoryId= in.readString();
		if ((this.options & OV_ANSWER) == 0) { // request
			if (this.operation.reqSourceExpr) {
				this.sourceExpr= in.readString();
			}
			if (this.operation.reqRData) {
				in.flags= 0;
				this.rdata= gDefaultFactory.readObject(in);
			}
			if (this.operation.reqTargetExpr) {
				this.targetExpr= in.readString();
			}
			if ((this.options & OV_WITHRHO) != 0) {
				in.flags= 0;
				this.rho= gDefaultFactory.readObject(in);
			}
		}
		else { // answer
			if ((this.options & OV_WITHDATA) != 0) {
				in.flags= (this.options & 0xff);
				this.rdata= getFactory(this.factoryId).readObject(in);
			}
			if ((this.options & OV_WITHRHO) != 0) {
				in.flags= RObjectFactory.F_ONLY_STRUCT;
				this.rho= getFactory(this.factoryId).readObject(in);
			}
		}
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		out.writeInt(this.requestId);
		out.writeByte(this.operation.op);
		out.writeInt(this.options);
		if ((this.options & OV_WITHSTATUS) != 0) {
			this.status.writeExternal(out);
			return;
		}
		out.writeByte(this.depth);
		out.writeString(this.factoryId);
		if ((this.options & OV_ANSWER) == 0) { // request
			if (this.operation.reqSourceExpr) {
				out.writeString(this.sourceExpr);
			}
			if (this.operation.reqRData) {
				out.flags= 0;
				gDefaultFactory.writeObject(this.rdata, out);
			}
			if (this.operation.reqTargetExpr) {
				out.writeString(this.targetExpr);
			}
			if ((this.options & OV_WITHRHO) != 0) {
				out.flags= 0;
				gDefaultFactory.writeObject(this.rho, out);
			}
		}
		else { // anwser
			if ((this.options & OV_WITHDATA) != 0) {
				out.flags= (this.options & 0xff);
				gDefaultFactory.writeObject(this.rdata, out);
			}
			if ((this.options & OV_WITHRHO) != 0) {
				out.flags= RObjectFactory.F_ONLY_STRUCT;
				gDefaultFactory.writeObject(this.rho, out);
			}
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
			this.options= (this.options & OM_CLEARFORANSWER) | OV_ANSWER;
			this.status= null;
			this.sourceExpr= null;
			this.rdata= null;
			this.targetExpr= null;
			this.rho= null;
		}
		else {
			this.options= ((this.options & OM_CLEARFORANSWER) | (OV_ANSWER | OV_WITHSTATUS));
			this.status= status;
			this.sourceExpr= null;
			this.rdata= null;
			this.targetExpr= null;
			this.rho= null;
		}
	}
	
	public void setAnswer(final RObject rdata, final RObject rho) {
		this.options= ((this.options & OM_CLEARFORANSWER) | OV_ANSWER);
		if (rdata != null) {
			this.options |= OV_WITHDATA;
		}
		if (rho != null) {
			this.options |= OV_WITHRHO;
		}
		this.status= null;
		this.sourceExpr= null;
		this.rdata= rdata;
		this.targetExpr= null;
		this.rho= rho;
	}
	
	
	@Override
	public byte getOp() {
		return this.operation.op;
	}
	
	public Operation getOperation() {
		return this.operation;
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
	public String getDataText() {
		return this.sourceExpr;
	}
	
	public RObject getData() {
		return this.rdata;
	}
	
	public String getTargetExpr() {
		return this.targetExpr;
	}
	
	public RObject getRho() {
		return this.rho;
	}
	
	public byte getDepth() {
		return this.depth;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof DataCmdItem)) {
			return false;
		}
		final DataCmdItem otherItem= (DataCmdItem) other;
		if (getOp() != otherItem.getOp()) {
			return false;
		}
		if (this.options != otherItem.options) {
			return false;
		}
		if (!((this.sourceExpr != null) ?
				this.sourceExpr.equals(otherItem.sourceExpr) :
				null == otherItem.sourceExpr )) {
			return false;
		}
		if (!((this.rdata != null) ?
				this.rdata.equals(otherItem.rdata) :
				null == otherItem.rdata )) {
			return false;
		}
		if (!((this.targetExpr != null) ?
				this.targetExpr.equals(otherItem.targetExpr) :
				null == otherItem.targetExpr )) {
			return false;
		}
		if (!((this.rho != null) ?
				this.rho.equals(otherItem.rho) :
				null == otherItem.rho )) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer sb= new StringBuffer(100);
		sb.append("DataCmdItem ");
		sb.append(this.operation.name);
		sb.append("\n\t").append("options= 0x").append(Integer.toHexString(this.options));
		if (this.sourceExpr != null) {
			sb.append("\n<SOURCE-EXPR>\n");
			sb.append(this.sourceExpr);
			sb.append("\n</SOURCE-EXPR>");
		}
		else {
			sb.append("\n<SOURCE-EXPR/>");
		}
		if ((this.options & OV_WITHDATA) != 0) {
			sb.append("\n<DATA>\n");
			sb.append(this.rdata.toString());
			sb.append("\n</DATA>");
		}
		else {
			sb.append("\n<DATA/>");
		}
		if (this.targetExpr != null) {
			sb.append("\n<TARGET-EXPR>\n");
			sb.append(this.targetExpr);
			sb.append("\n</TARGET-EXPR>");
		}
		else {
			sb.append("\n<TARGET-EXPR/>");
		}
		if ((this.options & OV_WITHRHO) != 0) {
			sb.append("\n<RHO>\n");
			sb.append(this.rho.toString());
			sb.append("\n</RHO>");
		}
		return sb.toString();
	}
	
}
