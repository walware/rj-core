/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import static de.walware.rj.data.RObjectFactory.F_ONLY_STRUCT;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_ASSIGN_DATA;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_COMMON;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_EVAL_DATA;
import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.REnvironment;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RLanguage;
import de.walware.rj.data.RList;
import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RS4Object;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RDataFrameImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataStruct;
import de.walware.rj.data.defaultImpl.RFunctionImpl;
import de.walware.rj.data.defaultImpl.RMissing;
import de.walware.rj.data.defaultImpl.RNull;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.data.defaultImpl.ROtherImpl;
import de.walware.rj.data.defaultImpl.RPromise;
import de.walware.rj.data.defaultImpl.RReferenceImpl;
import de.walware.rj.data.defaultImpl.RS4ObjectImpl;
import de.walware.rj.data.defaultImpl.SimpleRListImpl;
import de.walware.rj.server.RjsException;


final class JRIServerRni {
	
	
	public static final byte EVAL_MODE_DEFAULT = 0;
	public static final byte EVAL_MODE_FORCE = 1;
	public static final byte EVAL_MODE_DATASLOT = 2;
	
	public static class RNullPointerException extends RjsException {
		
		private static final long serialVersionUID= 1L;
		
		public RNullPointerException() {
			super(0, "R engine returned unexpected null pointer (out of memory?).");
		}
		
	}
	
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final RObject[] EMPTY_ROBJECT_ARRAY = new RObject[0];
	private static final String[] DATA_NAME_ARRAY = new String[] { ".Data" }; //$NON-NLS-1$
	
	
	private final Rengine rEngine;
	
	public final long NULL_P;
	public final long Unbound_P;
	public final long MissingArg_P;
	
	public final long Base_EnvP;
	public final long BaseNamespace_EnvP;
	public final long Global_EnvP;
	public final long Empty_EnvP;
	public final long Autoload_EnvP;
	
	public final long Assign_SymP;
	public final long Block_SymP;
	public final long Ellipsis_SymP;
	public final long at_SymP;
	public final long edit_SymP;
	public final long encoding_SymP;
	public final long env_SymP;
	public final long expr_SymP;
	public final long error_SymP;
	public final long imaginary_SymP;
	public final long id_SymP;
	public final long isGeneric_SymP;
	public final long fdef_SymP;
	public final long filename_SymP;
	public final long flags_SymP;
	public final long function_SymP;
	public final long from_SymP;
	public final long lengthOut_SymP;
	public final long name_SymP;
	public final long names_SymP;
	public final long ns_SymP;
	public final long on_SymP;
	public final long onExit_SymP;
	public final long original_SymP;
	public final long output_SymP;
	public final long real_SymP;
	public final long signature_SymP;
	public final long value_SymP;
	public final long what_SymP;
	public final long where_SymP;
	public final long which_SymP;
	public final long wd_SymP;
	public final long x_SymP;
	public final long z_SymP;
	
	public final long TRUE_BoolP;
	public final long FALSE_BoolP;
	
	public final long appFilePath_SymP;
	public final long appElementId_SymP;
	public final long dbgElementId_SymP;
	
	private final long classSymP;
	private final long dimnamesSymP;
	private final long rowNamesSymP;
	private final long levelsSymP;
	private final long newSymP;
	private final long newClassSymP;
	private final long parentSymP;
	
	private final long factorClassStringP;
	private final long orderedClassStringP;
	private final long dataframeClassStringP;
	private final long newEnvFunP;
	private final long complexFunP;
	private final long ReFunP;
	private final long ImFunP;
	private final long optionsSymP;
	private final long seqIntFunP;
	
	private final long getNamespaceFunP;
	private final long getNamespaceExportedNamesFunP;
	private final long getNamespaceExportedValueFunP;
	
	private final long tryCatchFunP;
	private final long slotNamesFunP;
	private final long getFHeaderFunP;
	private final long deparseLineXCallP;
	private final long deparseLinesXCallP;
	private final long evalErrorHandlerExprP;
	private final long rniTempEvalClassExprP;
	
	public final long rniSafeBaseExecEnvP;
	public final long rniSafeGlobalExecEnvP;
	
	private final long rniTempEnvP;
	
	private final long rjTmpEnvP;
	
	public final long evalDummy_ExprP;
	
	private int rniProtectedCounter;
	
	private int stackPos = 0;
	
	private int currentDepth;
	private final int[] currentDepthStack = new int[255];
	private int maxDepth;
	private final int[] maxDepthStack = new int[255];
	
	private int maxEnvsLength = 10000;
	private final int[] maxEnvsLengthStack = new int[255];
	private int maxListsLength = 10000;
	private final int[] maxListsLengthStack = new int[255];
	
	private boolean rniTempEvalAssigned;
	
	boolean rniInterrupted;
	
	
	public JRIServerRni(final Rengine rEngine) throws RjsException {
		this.rEngine = rEngine;
		
		final int savedProtected = saveProtected();
		try {
			this.NULL_P= this.rEngine.rniSpecialObject(Rengine.SO_NilValue);
			this.Unbound_P= this.rEngine.rniSpecialObject(Rengine.SO_UnboundValue);
			this.MissingArg_P= this.rEngine.rniSpecialObject(Rengine.SO_MissingArg);
			this.Base_EnvP= this.rEngine.rniSpecialObject(Rengine.SO_BaseEnv);
			this.BaseNamespace_EnvP= this.rEngine.rniSpecialObject(Rengine.SO_BaseNamespaceEnv);
			this.Global_EnvP= this.rEngine.rniSpecialObject(Rengine.SO_GlobalEnv);
			this.Empty_EnvP= this.rEngine.rniSpecialObject(Rengine.SO_EmptyEnv);
			{	final long p= this.rEngine.rniEval(this.rEngine.rniInstallSymbol(".AutoloadEnv"), //$NON-NLS-1$
						this.Base_EnvP );
				this.Autoload_EnvP= (p != 0 && this.rEngine.rniExpType(p) == REXP.ENVSXP) ? p : 0;
			}
			
			this.Assign_SymP= this.rEngine.rniInstallSymbol("<-"); //$NON-NLS-1$
			this.Block_SymP= this.rEngine.rniInstallSymbol("{"); //$NON-NLS-1$
			this.Ellipsis_SymP= this.rEngine.rniInstallSymbol("..."); //$NON-NLS-1$
			this.at_SymP= this.rEngine.rniInstallSymbol("at"); //$NON-NLS-1$
			this.newClassSymP= this.rEngine.rniInstallSymbol("Class"); //$NON-NLS-1$
			this.classSymP= this.rEngine.rniInstallSymbol("class"); //$NON-NLS-1$
			this.dimnamesSymP= this.rEngine.rniInstallSymbol("dimnames"); //$NON-NLS-1$
			this.edit_SymP= this.rEngine.rniInstallSymbol("edit"); //$NON-NLS-1$
			this.encoding_SymP= this.rEngine.rniInstallSymbol("encoding"); //$NON-NLS-1$
			this.env_SymP= this.rEngine.rniInstallSymbol("env"); //$NON-NLS-1$
			this.error_SymP= this.rEngine.rniInstallSymbol("error"); //$NON-NLS-1$
			this.expr_SymP= this.rEngine.rniInstallSymbol("expr"); //$NON-NLS-1$
			this.fdef_SymP= this.rEngine.rniInstallSymbol("fdef"); //$NON-NLS-1$
			this.filename_SymP= this.rEngine.rniInstallSymbol("filename"); //$NON-NLS-1$
			this.flags_SymP= this.rEngine.rniInstallSymbol("flags"); //$NON-NLS-1$
			this.function_SymP= this.rEngine.rniInstallSymbol("function"); //$NON-NLS-1$
			this.from_SymP= this.rEngine.rniInstallSymbol("from"); //$NON-NLS-1$
			this.id_SymP= this.rEngine.rniInstallSymbol("id"); //$NON-NLS-1$
			this.isGeneric_SymP= this.rEngine.rniInstallSymbol("isGeneric"); //$NON-NLS-1$
			this.imaginary_SymP= this.rEngine.rniInstallSymbol("imaginary"); //$NON-NLS-1$
			this.lengthOut_SymP= this.rEngine.rniInstallSymbol("length.out"); //$NON-NLS-1$
			this.levelsSymP= this.rEngine.rniInstallSymbol("levels"); //$NON-NLS-1$
			this.name_SymP= this.rEngine.rniInstallSymbol("name"); //$NON-NLS-1$
			this.names_SymP= this.rEngine.rniInstallSymbol("names"); //$NON-NLS-1$
			this.newSymP= this.rEngine.rniInstallSymbol("new"); //$NON-NLS-1$
			this.ns_SymP= this.rEngine.rniInstallSymbol("ns"); //$NON-NLS-1$
			this.on_SymP= this.rEngine.rniInstallSymbol("on"); //$NON-NLS-1$
			this.onExit_SymP= this.rEngine.rniInstallSymbol("on.exit"); //$NON-NLS-1$
			this.optionsSymP= this.rEngine.rniInstallSymbol("options"); //$NON-NLS-1$
			this.original_SymP= this.rEngine.rniInstallSymbol("original"); //$NON-NLS-1$
			this.output_SymP= this.rEngine.rniInstallSymbol("output"); //$NON-NLS-1$
			this.parentSymP= this.rEngine.rniInstallSymbol("parent"); //$NON-NLS-1$
			this.real_SymP= this.rEngine.rniInstallSymbol("real"); //$NON-NLS-1$
			this.rowNamesSymP= this.rEngine.rniInstallSymbol("row.names"); //$NON-NLS-1$
			this.signature_SymP= this.rEngine.rniInstallSymbol("signature"); //$NON-NLS-1$
			this.value_SymP= this.rEngine.rniInstallSymbol("value"); //$NON-NLS-1$
			this.what_SymP= this.rEngine.rniInstallSymbol("what"); //$NON-NLS-1$
			this.where_SymP= this.rEngine.rniInstallSymbol("where"); //$NON-NLS-1$
			this.which_SymP= this.rEngine.rniInstallSymbol("which"); //$NON-NLS-1$
			this.wd_SymP= this.rEngine.rniInstallSymbol("wd"); //$NON-NLS-1$
			this.x_SymP= this.rEngine.rniInstallSymbol("x"); //$NON-NLS-1$
			this.z_SymP= this.rEngine.rniInstallSymbol("z"); //$NON-NLS-1$
			
			this.appFilePath_SymP= this.rEngine.rniInstallSymbol("statet.Path");
			this.appElementId_SymP= this.rEngine.rniInstallSymbol("statet.ElementId");
			this.dbgElementId_SymP= this.rEngine.rniInstallSymbol("dbg.ElementId");
			
			this.TRUE_BoolP= checkAndPreserve(this.rEngine.rniPutBoolArray(
					new boolean[] { true } ));
			this.FALSE_BoolP= checkAndPreserve(this.rEngine.rniPutBoolArray(
					new boolean[] { false } ));
			
			this.orderedClassStringP= checkAndPreserve(this.rEngine.rniPutStringArray(
					new String[] { "ordered", "factor" } )); //$NON-NLS-1$ //$NON-NLS-2$
			this.factorClassStringP= checkAndPreserve(this.rEngine.rniPutString("factor")); //$NON-NLS-1$
			this.dataframeClassStringP= checkAndPreserve(this.rEngine.rniPutString("data.frame")); //$NON-NLS-1$
			
			this.newEnvFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("new.env"), //$NON-NLS-1$
					this.Base_EnvP ));
			
			this.complexFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("complex"), //$NON-NLS-1$
					this.Base_EnvP ));
			this.ReFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("Re"), //$NON-NLS-1$
					this.Base_EnvP ));
			this.ImFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("Im"), //$NON-NLS-1$
					this.Base_EnvP ));
			
			this.getNamespaceFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("getNamespace"), //$NON-NLS-1$
					this.Base_EnvP ));
			this.getNamespaceExportedNamesFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("getNamespaceExports"), //$NON-NLS-1$
					this.Base_EnvP ));
			this.getNamespaceExportedValueFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("getExportedValue"), //$NON-NLS-1$
					this.Base_EnvP ));
			
			this.seqIntFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("seq.int"), //$NON-NLS-1$
					this.Base_EnvP ));
			
			this.tryCatchFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("tryCatch"), //$NON-NLS-1$
					this.Base_EnvP ));
			this.slotNamesFunP= checkAndPreserve(this.rEngine.rniEval(
					this.rEngine.rniParse("methods::.slotNames", 1), //$NON-NLS-1$
					this.Base_EnvP ));
			
			{	final long pasteFunP= protect(this.rEngine.rniEval(
						this.rEngine.rniInstallSymbol("paste"), //$NON-NLS-1$
						this.Base_EnvP ));
				final long deparseFunP= protect(this.rEngine.rniEval(
						this.rEngine.rniInstallSymbol("deparse"), //$NON-NLS-1$
						this.Base_EnvP ));
				
				final long controlSymP= this.rEngine.rniInstallSymbol("control"); //$NON-NLS-1$
				final long widthCutoffSymP= this.rEngine.rniInstallSymbol("width.cutoff"); //$NON-NLS-1$
				final long collapseSymP= this.rEngine.rniInstallSymbol("collapse"); //$NON-NLS-1$
				
				final long deparseControlValueP= protect(this.rEngine.rniPutStringArray(
						new String[] { "keepInteger", "keepNA" } )); //$NON-NLS-1$ //$NON-NLS-2$
				final long collapseValueP= protect(this.rEngine.rniPutString("")); //$NON-NLS-1$
				
				{	// function(x)paste(deparse(expr=args(name=x),control=c("keepInteger", "keepNA"),width.cutoff=500L),collapse="")
					final long fArgsP= protect(this.rEngine.rniCons(
							this.MissingArg_P, this.NULL_P,
							this.x_SymP, false ));
					final long argsFunP= protect(this.rEngine.rniEval(
							this.rEngine.rniInstallSymbol("args"), //$NON-NLS-1$
							this.Base_EnvP ));
					final long argsCallP= protect(this.rEngine.rniCons(
							argsFunP, this.rEngine.rniCons(
									this.x_SymP, this.NULL_P,
									this.name_SymP, false ),
							0, true ));
					final long deparseCallP= protect(this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									argsCallP, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 500 }), this.NULL_P,
													widthCutoffSymP, false ),
											controlSymP, false ),
									this.expr_SymP, false ),
							0, true ));
					final long fBodyP= this.rEngine.rniCons(
							pasteFunP, this.rEngine.rniCons(
									deparseCallP, this.rEngine.rniCons(
											collapseValueP, this.NULL_P,
											collapseSymP, false ),
									0, false ),
							0, true );
					this.getFHeaderFunP= checkAndPreserve(this.rEngine.rniEval(this.rEngine.rniCons(
							this.function_SymP, this.rEngine.rniCons(
									fArgsP, this.rEngine.rniCons(
											fBodyP, this.NULL_P,
											0, false ),
									0, false ),
							0, true ),
							this.Base_EnvP ));
				}
				
				{	// paste(deparse(expr=x,control=c("keepInteger", "keepNA"),width.cutoff=500L),collapse="")
					final long deparseCallP= protect(this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									this.x_SymP, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 500 }), this.NULL_P,
													widthCutoffSymP, false ),
											controlSymP, false ),
									this.expr_SymP, false ),
							0, true ));
					this.deparseLineXCallP= checkAndPreserve(this.rEngine.rniCons(
							pasteFunP, this.rEngine.rniCons(
									deparseCallP, this.rEngine.rniCons(
											collapseValueP, this.NULL_P,
											collapseSymP, false ),
									0, false ),
							0, true ));
				}
				{	// deparse(expr=x,control=c("keepInteger", "keepNA"),width.cutoff=80L)
					this.deparseLinesXCallP= checkAndPreserve(this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									this.x_SymP, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 80 }), this.NULL_P,
													widthCutoffSymP, false ),
											controlSymP, false ),
									this.expr_SymP, false ),
							0, true ));
				}
			}
			
			this.evalErrorHandlerExprP= checkAndPreserve(this.rEngine.rniCons(
					this.rEngine.rniEval(this.rEngine.rniParse("function(e){" +
							"s<-raw(5);" +
							"class(s)<-\".rj.eval.error\";" +
							"attr(s,\"error\")<-e;" +
							"attr(s,\"output\")<-paste(capture.output(print(e)),collapse=\"\\n\");" +
							"invisible(s);}", 1 ), 0 ), this.NULL_P,
					this.error_SymP, false ));
			
			this.rniTempEvalClassExprP= checkAndPreserve(this.rEngine.rniParse("class(x);", 1));
			
			this.evalDummy_ExprP= checkAndPreserve(this.rEngine.rniParse("1+1;", 1));
			
			this.rniSafeBaseExecEnvP= checkAndPreserve(this.rEngine.rniEval(this.rEngine.rniCons(
							this.newEnvFunP, this.rEngine.rniCons(
									this.Base_EnvP, this.NULL_P,
									this.parentSymP, false ),
							0, true ),
					this.Base_EnvP ));
			this.rniSafeGlobalExecEnvP= checkAndPreserve(createNewEnv(this.Global_EnvP));
			
			this.rniTempEnvP= checkAndPreserve(createNewEnv(this.Global_EnvP));
			
			this.rjTmpEnvP= this.rEngine.rniEval(
					this.rEngine.rniParse("rj:::.rj.tmp", 1),
					this.Base_EnvP );
			
			if (LOGGER.isLoggable(Level.FINER)) {
				final StringBuilder sb= new StringBuilder("Rni Pointers:");
				
				final Field[] fields= getClass().getDeclaredFields();
				for (final Field field : fields) {
					final String name= field.getName();
					if (name.endsWith("P") && Long.TYPE.equals(field.getType())) {
						sb.append("\n\t");
						sb.append(name.substring(0, name.length() - 1));
						sb.append("= ");
						try {
							final long p= field.getLong(this);
							sb.append("0x");
							sb.append(Long.toHexString(p));
						}
						catch (final Exception e) {
							sb.append(e.getMessage());
						}
					}
				}
				
				LOGGER.log(Level.FINER, sb.toString());
			}
			
			if (this.tryCatchFunP == 0) {
				throw new RjsException(0, "Base functions are missing (check 'Renviron')");
			}
		}
		finally {
			looseProtected(savedProtected);
		}
	}
	
	
	public Rengine getREngine() {
		return this.rEngine;
	}
	
	public long protect(final long p) {
		this.rEngine.rniProtect(p);
		this.rniProtectedCounter++;
		return p;
	}
	
	public long checkAndProtect(final long p) throws RNullPointerException {
		if (p == 0) {
			throw new RNullPointerException();
		}
		this.rEngine.rniProtect(p);
		this.rniProtectedCounter++;
		return p;
	}
	
	public int saveProtected() {
		return this.rniProtectedCounter;
	}
	
	public void looseProtected(final int saved) {
		if (this.rniProtectedCounter > saved) {
			this.rEngine.rniUnprotect(this.rniProtectedCounter - saved);
			this.rniProtectedCounter = saved;
		}
	}
	
	public long checkAndPreserve(final long p) throws RNullPointerException {
		if (p == 0) {
			throw new RNullPointerException();
		}
		this.rEngine.rniPreserve(p);
		return p;
	}
	
	public void newDataLevel(final int maxDepth, final int maxEnvLength, final int maxListLength) {
		this.currentDepthStack[this.stackPos] = this.currentDepth;
		this.currentDepth = 0;
		this.maxDepthStack[this.stackPos] = this.maxDepth;
		this.maxDepth = maxDepth;
		this.maxEnvsLengthStack[this.stackPos] = this.maxEnvsLength;
		this.maxEnvsLength = maxEnvLength;
		this.maxListsLengthStack[this.stackPos] = this.maxListsLength;
		this.maxListsLength = maxListLength;
		
		this.stackPos++;
	}
	
	public void exitDataLevel() {
		this.stackPos--;
		this.currentDepth = this.currentDepthStack[this.stackPos];
		this.maxDepth = this.maxDepthStack[this.stackPos];
		this.maxEnvsLength = this.maxEnvsLengthStack[this.stackPos];
		this.maxListsLength = this.maxListsLengthStack[this.stackPos];
		
		if (this.rniTempEvalAssigned) {
			this.rEngine.rniAssignVarBySym(this.x_SymP, this.NULL_P, this.rniTempEnvP);
			this.rniTempEvalAssigned = false;
		}
	}
	
	
	public long createFCall(final String name, final RList args) throws RjsException {
		long argsP = this.NULL_P;
		for (int i = (int) args.getLength() - 1; i >= 0; i--) {
			final String argName = args.getName(i);
			final RObject argValue = args.get(i);
			final long argValueP;
			if (argValue != null) {
				argValueP = assignDataObject(argValue);
			}
			else {
				argValueP = this.MissingArg_P;
			}
			argsP = protect(this.rEngine.rniCons(argValueP, argsP,
					(argName != null) ? this.rEngine.rniInstallSymbol(argName) : 0, false ));
		}
		long funP;
		if (name.indexOf(':') > 0) {
			funP = this.rEngine.rniParse(name, 1);
			long[] list;
			if (funP != 0 && (list = this.rEngine.rniGetVector(funP)) != null && list.length == 1) {
				funP = list[0];
			}
			else {
				throw new RjsException(CODE_DATA_COMMON | 0x4, "The reference to the function is invalid.");
			}
		}
		else {
			funP = this.rEngine.rniInstallSymbol(name);
		}
		return this.rEngine.rniCons(funP, argsP, 0, true);
	}
	
	public long resolveExpression(final String expression) throws RjsException {
		final long exprP = this.rEngine.rniParse(expression, -1);
		if (this.rEngine.rniExpType(exprP) != REXP.EXPRSXP) {
			throw new RjsException((CODE_DATA_COMMON | 0x3),
					"The specified expression is invalid (syntax error)." );
		}
		final long[] expressionsP = this.rEngine.rniGetVector(exprP);
		if (expressionsP == null || expressionsP.length != 1) {
			throw new RjsException((CODE_DATA_COMMON | 0x3),
					"The specified expression is invalid (not a single expression)." );
		}
		return expressionsP[0];
	}
	
	public long resolveEnvironment(final RObject data) throws RjsException {
		if (data == null) {
			return this.Global_EnvP;
		}
		try {
			long envP = 0;
			switch (data.getRObjectType()) {
			case RObject.TYPE_REFERENCE:
				envP = ((RReference) data).getHandle();
				break;
			case RObject.TYPE_LANGUAGE:
				envP = evalExpr(assignDataObject(data),
						0, CODE_DATA_COMMON );
				break;
			default:
				throw new RjsException(CODE_DATA_COMMON, "Unsupported specification.");
			}
			if (envP == 0 || this.rEngine.rniExpType(envP) != REXP.ENVSXP) {
				throw new RjsException(CODE_DATA_COMMON, "Not an environment.");
			}
			return envP;
		}
		catch (final RjsException e) {
			throw new RjsException(CODE_DATA_COMMON | 0xa, "Could not resolve the environment.", e);
		}
	}
	
	public long evalExpr(long exprP, final long envP, final int code) throws RjsException {
		exprP = this.rEngine.rniCons(
				this.tryCatchFunP, this.rEngine.rniCons(
						exprP, this.evalErrorHandlerExprP,
						this.expr_SymP, false ),
				0, true );
		final long objP = this.rEngine.rniEval(exprP, envP);
		if (objP == 0) {
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			throw new IllegalStateException("JRI returned error code " + objP + " (pointer = 0x" + Long.toHexString(exprP) + ")");
		}
		protect(objP);
		if (this.rEngine.rniExpType(objP) == REXP.RAWSXP) {
			final String className1 = this.rEngine.rniGetClassAttrString(objP);
			if (className1 != null && className1.equals(".rj.eval.error")) {
				String message = null;
				final long outputP = this.rEngine.rniGetAttrBySym(objP, this.output_SymP);
				if (outputP != 0) {
					message = this.rEngine.rniGetString(outputP);
				}
				if (message == null) {
					message = "<no information available>";
				}
				switch (code) {
				case (CODE_DATA_EVAL_DATA | 0x3):
					message = "An error occurred when evaluation the specified expression in R " + message + ".";
					break;
				case (CODE_DATA_EVAL_DATA | 0x4):
					message = "An error occurred when evaluation the function in R " + message + ".";
					break;
				case (CODE_DATA_ASSIGN_DATA | 0x3):
					message = "An error occurred when assigning the value to the specified expression in R " + message + ".";
					break;
				case (CODE_DATA_ASSIGN_DATA | 0x8):
					message = "An error occurred when instancing an S4 object in R " + message + ".";
					break;
				default:
					message = message + ".";
					break;
				}
				throw new RjsException(code, message);
			}
		}
		return objP;
	}
	
	/**
	 * Put an {@link RObject RJ R object} into JRI, and get back the pointer to the object
	 * (Java to R).
	 * 
	 * @param obj an R object
	 * @return long protected R pointer
	 * @throws RjsException 
	 */ 
	public long assignDataObject(final RObject obj) throws RjsException {
		RStore<?> names;
		long objP;
		switch(obj.getRObjectType()) {
		case RObject.TYPE_NULL:
		case RObject.TYPE_MISSING:
			return this.NULL_P;
		case RObject.TYPE_VECTOR: {
			objP = assignDataStore(obj.getData());
			names = ((RVector<?>) obj).getNames();
			if (names != null) {
				this.rEngine.rniSetAttrBySym(objP, this.names_SymP, assignDataStore(names));
			}
			return objP; }
		case RObject.TYPE_ARRAY:
			objP = assignDataStore(obj.getData());
			this.rEngine.rniSetAttr(objP, "dim",
					this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
			return objP;
		case RObject.TYPE_DATAFRAME: {
			final RDataFrame list = (RDataFrame) obj;
			final long length = list.getLength();
			if (length > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("long list");
			}
			final long[] itemPs = new long[(int) length];
			for (int i = 0; i < length; i++) {
				itemPs[i] = assignDataStore(list.getColumn(i));
			}
			objP= checkAndProtect(this.rEngine.rniPutVector(itemPs));
			names = list.getNames();
			if (names != null) {
				this.rEngine.rniSetAttrBySym(objP, this.names_SymP, assignDataStore(names));
			}
			names = list.getRowNames();
			this.rEngine.rniSetAttrBySym(objP, this.rowNamesSymP, (names != null) ?
					assignDataStore(names) : seqLength(list.getRowCount()) );
			this.rEngine.rniSetAttrBySym(objP, this.classSymP, this.dataframeClassStringP);
			return objP; }
		case RObject.TYPE_LIST: {
			final RList list = (RList) obj;
			final long length = list.getLength();
			if (length > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("long list");
			}
			final long[] itemPs = new long[(int) length];
			final int savedProtectedCounter = this.rniProtectedCounter;
			for (int i = 0; i < length; i++) {
				itemPs[i] = assignDataObject(list.get(i));
			}
			if (this.rniProtectedCounter > savedProtectedCounter) {
				this.rEngine.rniUnprotect(this.rniProtectedCounter - savedProtectedCounter);
				this.rniProtectedCounter = savedProtectedCounter;
			}
			objP= checkAndProtect(this.rEngine.rniPutVector(itemPs));
			names = list.getNames();
			if (names != null) {
				this.rEngine.rniSetAttrBySym(objP, this.names_SymP, assignDataStore(names));
			}
			return objP; }
		case RObject.TYPE_REFERENCE:
			return ((RReference) obj).getHandle();
		case RObject.TYPE_S4OBJECT: {
			final RS4Object s4obj = (RS4Object) obj;
			objP = this.NULL_P;
			for (int i = (int) s4obj.getLength()-1; i >= 0; i--) {
				final RObject slotObj = s4obj.get(i);
				if (slotObj != null && slotObj.getRObjectType() != RObject.TYPE_MISSING) {
					objP= checkAndProtect(this.rEngine.rniCons(
							assignDataObject(slotObj), objP,
							this.rEngine.rniInstallSymbol(s4obj.getName(i)), false ));
				}
			}
			return protect(evalExpr(this.rEngine.rniCons(
							this.newSymP, this.rEngine.rniCons(
									this.rEngine.rniPutString(s4obj.getRClassName()), objP,
									this.newClassSymP, false ),
							0, true ),
					this.rniSafeGlobalExecEnvP, (CODE_DATA_ASSIGN_DATA | 0x8) )); }
		case RObject.TYPE_LANGUAGE: {
			final RLanguage lang = (RLanguage) obj;
			if (lang.getLanguageType() == RLanguage.NAME) {
				return this.rEngine.rniInstallSymbol(lang.getSource());
			}
			objP = this.rEngine.rniParse(lang.getSource(), -1);
			if (objP == 0) {
				throw new RjsException(CODE_DATA_ASSIGN_DATA | 0x9, "The language data is invalid.");
			}
			switch (lang.getLanguageType()) {
			case RLanguage.EXPRESSION:
				return protect(objP);
			case RLanguage.CALL: {
				final long[] list = this.rEngine.rniGetVector(objP);
				if (list != null && list.length == 1
						&& this.rEngine.rniExpType(list[0]) == REXP.LANGSXP ) {
					return protect(list[0]);
				}
				break; }
			case 0: // auto
				final long[] list = this.rEngine.rniGetVector(objP);
				if (list != null && list.length == 1) {
					return protect(list[0]);
				}
				//$FALL-THROUGH$
			default:
				return protect(objP);
			}
			break; }
		default:
			break;
		}
		throw new RjsException((CODE_DATA_ASSIGN_DATA | 0x7),
				"The instantiation of R objects of type " + RDataUtil.getObjectTypeName(obj.getRObjectType()) + " in R is not yet supported." );
	}
	
	public long assignDataStore(final RStore<?> data) throws RNullPointerException {
		switch (data.getStoreType()) {
		case RStore.LOGICAL:
			return checkAndProtect(this.rEngine.rniPutBoolArrayI(
					((JRILogicalDataImpl) data).getJRIValueArray() ));
		case RStore.INTEGER:
			return checkAndProtect(this.rEngine.rniPutIntArray(
					((JRIIntegerDataImpl) data).getJRIValueArray() ));
		case RStore.NUMERIC:
			return checkAndProtect(this.rEngine.rniPutDoubleArray(
					((JRINumericDataImpl) data).getJRIValueArray() ));
		case RStore.COMPLEX: {
			final JRIComplexDataShortImpl complex = (JRIComplexDataShortImpl) data;
			final long realP= checkAndProtect(this.rEngine.rniPutDoubleArray(
					complex.getJRIRealValueArray() ));
			final long imaginaryP= checkAndProtect(this.rEngine.rniPutDoubleArray(
					complex.getJRIImaginaryValueArray() ));
			return checkAndProtect(this.rEngine.rniEval(this.rEngine.rniCons(
							this.complexFunP, this.rEngine.rniCons(
									realP, this.rEngine.rniCons(
											imaginaryP, this.NULL_P,
											this.imaginary_SymP, false ),
									this.real_SymP, false ),
							0, true ),
					this.rniSafeBaseExecEnvP )); }
		case RStore.CHARACTER:
			return checkAndProtect(this.rEngine.rniPutStringArray(
					((JRICharacterDataImpl) data).getJRIValueArray() ));
		case RStore.RAW:
			return checkAndProtect(this.rEngine.rniPutRawArray(
					((JRIRawDataImpl) data).getJRIValueArray() ));
		case RStore.FACTOR: {
			final JRIFactorDataImpl factor = (JRIFactorDataImpl) data;
			final long objP= checkAndProtect(this.rEngine.rniPutIntArray(
					factor.getJRIValueArray() ));
			this.rEngine.rniSetAttrBySym(objP, this.levelsSymP,
					this.rEngine.rniPutStringArray(factor.getJRILevelsArray()) );
			this.rEngine.rniSetAttrBySym(objP, this.classSymP,
					factor.isOrdered() ? this.orderedClassStringP : this.factorClassStringP );
			return objP; }
		default:
			throw new UnsupportedOperationException("Data storage of type " + data.getStoreType() + " are not yet supported.");
		}
	}
	
	public RObject createDataObject(final long objP, final int flags) {
		if (objP == 0) {
			throw new IllegalArgumentException("objP: 0x0");
		}
		if (this.maxDepth > 0) {
			return createDataObject(objP, flags, EVAL_MODE_FORCE);
		}
		else {
			final RObject rObject = createDataObject(objP, (flags | F_ONLY_STRUCT), EVAL_MODE_FORCE);
			return new RReferenceImpl(objP, rObject.getRObjectType(), rObject.getRClassName());
		}
	}
	
	/**
	 * Returns {@link RObject RJ/R object} for the given R pointer
	 * (R to Java).
	 * 
	 * @param objP a valid pointer to an object in R
	 * @param objTmp an optional R expression pointing to the same object in R
	 * @param flags to configure the data to create
	 * @param force forces the creation of the object (ignoring the depth etc.)
	 * @return new created R object
	 */ 
	public RObject createDataObject(long objP, final int flags, final byte mode) {
		if (mode == EVAL_MODE_DEFAULT && (this.currentDepth >= this.maxDepth)) {
			return null;
		}
		this.currentDepth++;
		try {
			int rType = this.rEngine.rniExpType(objP);
			if (rType == REXP.PROMSXP) {
				objP = this.rEngine.rniGetPromise(objP, 
						((flags & RObjectFactory.F_LOAD_PROMISE) != 0) ? 2 : 1);
				if (objP == 0) {
					return RPromise.INSTANCE;
				}
				rType = this.rEngine.rniExpType(objP);
			}
			switch (rType) {
			case REXP.NILSXP:
				return RNull.INSTANCE;
			
			case REXP.LGLSXP: { // logical vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.LGLSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.INTSXP: { // integer vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.INTSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				if (className1 != null
						&& (className1.equals("factor")
								|| className1.equals("ordered")
								|| this.rEngine.rniInherits(objP, "factor")) ) {
					final String[] levels;
					{	final long levelsP = this.rEngine.rniGetAttrBySym(objP, this.levelsSymP);
						levels = (levelsP != 0) ? this.rEngine.rniGetStringArray(levelsP) : null;
					}
					if (levels != null) {
						final boolean isOrdered = className1.equals("ordered") || this.rEngine.rniInherits(objP, "ordered");
						final RFactorStore factorData = ((flags & F_ONLY_STRUCT) != 0) ?
								new RFactorDataStruct(isOrdered, levels.length) :
								new RFactorDataImpl(this.rEngine.rniGetIntArray(objP), isOrdered, levels);
						return ((flags & F_ONLY_STRUCT) != 0) ?
								new JRIVectorImpl<RIntegerStore>(
										factorData,
										this.rEngine.rniGetVectorLength(objP), className1, null ) :
								new JRIVectorImpl<RIntegerStore>(
										factorData,
										className1, getNames(objP) );
					}
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.REALSXP: { // numeric vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.REALSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.CPLXSXP: { // complex vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.CPLXSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
						new JRIArrayImpl<RComplexStore>(
								RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
								className1, dim ) :
						new JRIArrayImpl<RComplexStore>(
								new JRIComplexDataShortImpl(getComplexRe(objP), getComplexIm(objP)),
								className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RComplexStore>(
									RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RComplexStore>(
									new JRIComplexDataShortImpl(getComplexRe(objP), getComplexIm(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.STRSXP: { // character vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.STRSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.RAWSXP: { // raw/byte vector
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					if (this.rEngine.rniIsS4(objP)) {
						return createS4Obj(objP, REXP.RAWSXP, flags);
					}
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final int[] dim = this.rEngine.rniGetArrayDim(objP);
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									this.rEngine.rniGetVectorLength(objP), className1, null ) :
							new JRIVectorImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, getNames(objP) );
				}
			}
			case REXP.VECSXP: { // generic vector / list
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				
				final long length = this.rEngine.rniGetVectorLength(objP);
				if (length > Integer.MAX_VALUE) {
					if ((flags & F_ONLY_STRUCT) != 0) {
						return new JRIListLongImpl(length, className1);
					}
					throw new UnsupportedOperationException("long list");
				}
				
				final long[] itemP = this.rEngine.rniGetVector(objP);
				DATA_FRAME: if (className1 != null &&
						(className1.equals("data.frame") || this.rEngine.rniInherits(objP, "data.frame")) ) {
					final RObject[] itemObjects = new RObject[itemP.length];
					long rowCount = -1;
					for (int i = 0; i < itemP.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						itemObjects[i] = createDataObject(itemP[i], flags, EVAL_MODE_FORCE);
						if (itemObjects[i] == null || itemObjects[i].getRObjectType() != RObject.TYPE_VECTOR) {
							break DATA_FRAME;
						}
						else if (rowCount == -1) {
							rowCount = itemObjects[i].getLength();
						}
						else if (rowCount != itemObjects[i].getLength()){
							break DATA_FRAME;
						}
					}
					final String[] rowNames = ((flags & F_ONLY_STRUCT) != 0) ? null : getRowNames(objP);
					if (rowNames != null && rowCount != -1 && rowNames.length != rowCount) {
						break DATA_FRAME;
					}
					return new RDataFrameImpl(itemObjects, className1, getNames(objP), rowNames);
				}
				if (((flags & F_ONLY_STRUCT) != 0 && length > this.maxListsLength)
						|| this.currentDepth >= this.maxDepth ) {
					return new JRIListLongImpl(length, className1);
				}
				{	final RObject[] itemObjects = new RObject[itemP.length];
					for (int i = 0; i < itemP.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						itemObjects[i] = createDataObject(itemP[i], flags, EVAL_MODE_DEFAULT);
					}
					return new JRIListImpl(itemObjects, className1, getNames(objP));
				}
			}
			case REXP.LISTSXP:   // pairlist
			/*case REXP.LANGSXP: */{
				String className1;
				if (mode == EVAL_MODE_DATASLOT
						|| (className1 = this.rEngine.rniGetClassAttrString(objP)) == null ) {
					className1 = RObject.CLASSNAME_PAIRLIST;
				}
				
				long cdr = objP;
				final int length = this.rEngine.rniGetLength(objP);
				final String[] itemNames = new String[length];
				final RObject[] itemObjects = new RObject[length];
				for (int i = 0; i < length && cdr != 0; i++) {
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					final long car = this.rEngine.rniCAR(cdr);
					final long tag = this.rEngine.rniTAG(cdr);
					itemNames[i] = (tag != 0) ? this.rEngine.rniGetSymbolName(tag) : null;
					itemObjects[i] = createDataObject(car, flags, EVAL_MODE_DEFAULT);
					cdr = this.rEngine.rniCDR(cdr);
				}
				return new JRIListImpl(itemObjects, className1, itemNames);
			}
			case REXP.ENVSXP: {
				if (this.currentDepth > 1 && (flags & RObjectFactory.F_LOAD_ENVIR) == 0) {
					return new RReferenceImpl(objP, RObject.TYPE_ENV, "environment");
				}
				final long namesStrP= protect(this.rEngine.rniListEnv(objP, true));
				final String[] names = this.rEngine.rniGetStringArray(namesStrP);
				if (names != null) {
					if (objP == this.Autoload_EnvP || objP == this.rjTmpEnvP
							|| names.length > this.maxEnvsLength) {
						return createEnvObject(objP, null, null, names.length,
								(mode != EVAL_MODE_DATASLOT) );
					}
					
					final RObject[] itemObjects = new RObject[names.length];
					for (int i = 0; i < names.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						final long nameSymP= this.rEngine.rniInstallSymbolByStr(namesStrP, i);
						final long itemP= this.rEngine.rniGetVarBySym(nameSymP, objP, Rengine.FLAG_UNBOUND_P);
						if (itemP != 0) {
							protect(itemP);
							itemObjects[i] = createDataObject(itemP, flags, EVAL_MODE_DEFAULT);
							continue;
						}
						else {
							itemObjects[i] = RMissing.INSTANCE;
							continue;
						}
					}
					return createEnvObject(objP, itemObjects, names, names.length,
							(mode != EVAL_MODE_DATASLOT) );
				}
				break;
			}
			case REXP.CLOSXP: {
				if (mode != EVAL_MODE_DATASLOT && this.rEngine.rniIsS4(objP)) {
					return createS4Obj(objP, REXP.CLOSXP, flags);
				}
				
				final String header = getFHeader(objP);
				return new RFunctionImpl(header);
			}
			case REXP.SPECIALSXP:
			case REXP.BUILTINSXP: {
				final String header = getFHeader(objP);
				return new RFunctionImpl(header);
			}
			case REXP.S4SXP: {
				if (mode != EVAL_MODE_DATASLOT) {
					return createS4Obj(objP, REXP.S4SXP, flags);
				}
				break; // invalid
			}
			case REXP.SYMSXP: {
				if (objP == this.MissingArg_P) {
					return RMissing.INSTANCE;
				}
				return ((flags & F_ONLY_STRUCT) != 0) ? 
						new JRILanguageImpl(RLanguage.NAME, null) :
						new JRILanguageImpl(RLanguage.NAME, this.rEngine.rniGetSymbolName(objP), null);
			}
			case REXP.LANGSXP: {
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					className1 = this.rEngine.rniGetClassAttrString(objP);
				}
				else {
					className1 = null;
				}
				return ((flags & F_ONLY_STRUCT) != 0) ? 
						new JRILanguageImpl(RLanguage.CALL, className1) :
						new JRILanguageImpl(RLanguage.CALL, getSourceLine(objP), className1);
			}
			case REXP.EXPRSXP: {
				String className1;
				if (mode == EVAL_MODE_DATASLOT
						|| (className1 = this.rEngine.rniGetClassAttrString(objP)) == null ) {
					className1 = null;
				}
				return ((flags & F_ONLY_STRUCT) != 0) ? 
						new JRILanguageImpl(RLanguage.EXPRESSION, className1) :
						new JRILanguageImpl(RLanguage.EXPRESSION, getSourceLine(objP), className1);
			}
			case REXP.EXTPTRSXP: {
				String className1;
				if (mode == EVAL_MODE_DATASLOT
						|| (className1 = this.rEngine.rniGetClassAttrString(objP)) == null ) {
					className1 = "externalptr";
				}
				return new ROtherImpl(className1);
			}
			}
//				final long classP = this.rEngine.rniEval(this.rEngine.rniCons(this.classFunP,
//						this.rEngine.rniCons(objP, this._NULL_P, , false), 0, true),
//						this._Base_EnvP);
			{	// Other type and fallback
				final String className1 = getClassSave(objP);
				return new ROtherImpl(className1);
			}
		}
		finally {
			this.currentDepth--;
		}
	}
	
	private RObject createS4Obj(final long objP, final int rType, final int flags) {
		final long classP = this.rEngine.rniGetAttrBySym(objP, this.classSymP);
		String className = null;
		if (classP != 0 && classP != this.NULL_P) {
			className = this.rEngine.rniGetString(classP);
			final long slotNamesP = this.rEngine.rniEval(this.rEngine.rniCons(
							this.slotNamesFunP, this.rEngine.rniCons(
									classP, this.NULL_P,
									this.x_SymP, false ),
							0, true ),
					this.rniSafeGlobalExecEnvP );
			if (slotNamesP != 0) {
				final String[] slotNames = this.rEngine.rniGetStringArray(slotNamesP);
				if (slotNames != null && slotNames.length > 0) {
					final RObject[] slotValues = new RObject[slotNames.length];
					for (int i = 0; i < slotNames.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						if (".Data".equals(slotNames[i])) {
							slotValues[i] = createDataObject(objP, flags, EVAL_MODE_DATASLOT);
							if (className == null && slotValues[i] != null) {
								className = slotValues[i].getRClassName();
							}
							continue;
						}
						else {
							final long slotValueP;
							if ((slotValueP = this.rEngine.rniGetAttr(objP, slotNames[i])) != 0) {
								slotValues[i] = createDataObject(slotValueP, flags, EVAL_MODE_FORCE);
								continue;
							}
							else {
								slotValues[i] = RMissing.INSTANCE;
								continue;
							}
						}
					}
					if (className == null) {
						className = getClassSave(objP);
					}
					return new RS4ObjectImpl(className, slotNames, slotValues);
				}
			}
		}
		if (rType != REXP.S4SXP) {
			final RObject dataSlot = createDataObject(objP, flags, EVAL_MODE_DATASLOT);
			if (dataSlot != null) {
				if (className == null) {
					className = dataSlot.getRClassName();
					if (className == null) {
						className = getClassSave(objP);
					}
				}
				return new RS4ObjectImpl(className, DATA_NAME_ARRAY, new RObject[] { dataSlot });
			}
			if (className == null) {
				className = getClassSave(objP);
			}
		}
		else if (className == null) {
			className = "S4";
		}
		return new RS4ObjectImpl(className, EMPTY_STRING_ARRAY, EMPTY_ROBJECT_ARRAY);
	}
	
	private String getClassSave(final long objP) {
		if (objP != 0) {
			final String className1;
			final long classP;
			this.rniTempEvalAssigned = true;
			if (this.rEngine.rniAssignVarBySym(this.x_SymP, objP, this.rniTempEnvP)
					&& (classP = this.rEngine.rniEval(this.rniTempEvalClassExprP, this.rniTempEnvP)) != 0
					&& (className1 = this.rEngine.rniGetString(classP)) != null ) {
				return className1;
			}
		}
		return "<unknown>";
	}
	
	private String[] getNames(final long objP) {
		final long namesP= this.rEngine.rniGetAttrBySym(objP, this.names_SymP);
		return (namesP != 0) ? this.rEngine.rniGetStringArray(namesP) : null;
	}
	
	private SimpleRListImpl<RCharacterStore> getDimNames(final long objP, final int length) {
		final long namesP= this.rEngine.rniGetAttrBySym(objP, this.dimnamesSymP);
		final long[] names1P;
		if (this.rEngine.rniExpType(namesP) == REXP.VECSXP
				&& (names1P= this.rEngine.rniGetVector(namesP)) != null
				&& names1P.length == length) {
			String[] s = getNames(namesP);
			final RCharacterDataImpl names0 = (s != null) ? new RCharacterDataImpl(s) :
				new RCharacterDataImpl(names1P.length);
			final RCharacterStore[] names1 = new RCharacterStore[names1P.length];
			for (int i = 0; i < names1P.length; i++) {
				s = this.rEngine.rniGetStringArray(names1P[i]);
				if (s != null) {
					names1[i] = new RCharacterDataImpl(s);
				}
			}
			return new SimpleRListImpl<>(names1, names0);
		}
		return null;
	}
	
	private String[] getRowNames(final long objP) {
		final long namesP = this.rEngine.rniGetAttrBySym(objP, this.rowNamesSymP);
		return (namesP != 0) ? this.rEngine.rniGetStringArray(namesP) : null;
	}
	
	private double[] getComplexRe(final long objP) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(
						this.ReFunP, this.rEngine.rniCons(
								objP, this.NULL_P,
								this.z_SymP, false ),
						0, true ),
				this.rniSafeBaseExecEnvP );
		final double[] num;
		if (numP != 0
				&& ((num = this.rEngine.rniGetDoubleArray(numP)) != null) ) {
			return num;
		}
		if (this.rniInterrupted) {
			throw new CancellationException();
		}
		throw new RuntimeException("Failed to load values of real part of complex.");
	}
	
	private double[] getComplexIm(final long objP) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(
						this.ImFunP, this.rEngine.rniCons(
								objP, this.NULL_P,
								this.z_SymP, false ),
						0, true ),
				this.rniSafeBaseExecEnvP );
		final double[] num;
		if (numP != 0
				&& ((num = this.rEngine.rniGetDoubleArray(numP)) != null) ) {
			return num;
		}
		if (this.rniInterrupted) {
			throw new CancellationException();
		}
		throw new RuntimeException("Failed to load values of imaginary part of complex.");
	}
	
	private String getFHeader(final long cloP) {
		final long argsP= this.rEngine.rniEval(this.rEngine.rniCons(
						this.getFHeaderFunP, this.rEngine.rniCons(
								cloP, this.NULL_P,
								this.x_SymP, false ),
						0, true ),
				this.rniSafeGlobalExecEnvP );
		final String args;
		if (argsP != 0
				&& (args = this.rEngine.rniGetString(argsP)) != null
				&& args.length() >= 11 ) { // "function ()".length
//			return args.substring(9,);
			return args;
		}
		return null;
	}
	
	
	long createNewEnv(final long parentP) {
		if (parentP == 0) {
			throw new IllegalArgumentException("Missing parent for new environment.");
		}
		return this.rEngine.rniEval(this.rEngine.rniCons(
						this.newEnvFunP, this.rEngine.rniCons(
								parentP, this.NULL_P,
								this.parentSymP, false ),
						0, true ),
				this.rniSafeBaseExecEnvP );
	}
	
	private long getNamespaceEnvP(final String nsName) throws RjsException {
		if (nsName == null) {
			throw new IllegalArgumentException("Missing name for namespace.");
		}
		final long p= this.rEngine.rniEval(this.rEngine.rniCons(
						this.getNamespaceFunP, this.rEngine.rniCons(
								this.rEngine.rniPutString(nsName), this.NULL_P,
								this.name_SymP, false ),
						0, true ),
				this.rniSafeBaseExecEnvP );
		if (p != 0) {
			return p;
		}
		throw new RjsException(0, "Namespace '" + nsName + "' not available.");
	}
	
	public RObject getNamespaceEnv(final String nsName, final int flags) throws RjsException {
		final long envP= getNamespaceEnvP(nsName);
		if (this.maxDepth == 0) {
			return new RReferenceImpl(envP, RObject.TYPE_ENV, RObject.CLASSNAME_ENV);
		}
		
		return createDataObject(envP, flags, EVAL_MODE_FORCE);
	}
	
	public RObject getNamespaceExportsEnv(final String nsName, final int flags) throws RjsException {
		final long envP= getNamespaceEnvP(nsName);
		if (this.maxDepth == 0) {
			final String className1= this.rEngine.rniGetClassAttrString(envP);
			return new RReferenceImpl(0, RObject.TYPE_ENV,
					(className1 != null) ? className1 : RObject.CLASSNAME_ENV );
		}
		
		this.currentDepth++;
		try {
			final long namesStrP= this.rEngine.rniEval(this.rEngine.rniCons(
							this.getNamespaceExportedNamesFunP, this.rEngine.rniCons(
									envP, this.NULL_P,
									this.ns_SymP, false ),
							0, true ),
					this.rniSafeBaseExecEnvP );
			if (namesStrP != 0) {
				protect(namesStrP);
				final String[] names= this.rEngine.rniGetStringArray(namesStrP);
				if (names != null) {
					final RObject[] itemObjects= new RObject[names.length];
					for (int i = 0; i < names.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						final long itemP= this.rEngine.rniEval(this.rEngine.rniCons(
										this.getNamespaceExportedValueFunP, this.rEngine.rniCons(
												envP, this.rEngine.rniCons(
														this.rEngine.rniPutStringByStr(namesStrP, i), this.NULL_P,
														this.name_SymP, false ),
												this.ns_SymP, false ),
										0, true ),
								this.rniSafeBaseExecEnvP );
						if (itemP != 0) {
							protect(itemP);
							itemObjects[i]= createDataObject(itemP, flags, EVAL_MODE_DEFAULT);
							continue;
						}
						else {
							itemObjects[i] = RMissing.INSTANCE;
							continue;
						}
					}
					return new JRIEnvironmentImpl(nsName, 0, itemObjects, names, names.length,
							REnvironment.ENVTYPE_NAMESPACE_EXPORTS, null );
				}
			}
			return new JRIEnvironmentImpl(nsName, 0, EMPTY_ROBJECT_ARRAY, EMPTY_STRING_ARRAY, 0,
					REnvironment.ENVTYPE_NAMESPACE_EXPORTS, null );
		}
		finally {
			this.currentDepth--;
		}
	}
	
	public boolean isInternEnv(final long envP) {
		return (envP == this.rjTmpEnvP
				|| envP == this.rniSafeBaseExecEnvP || envP == this.rniSafeGlobalExecEnvP );
	}
	
	public void addAllEnvs(final List<Long> envs, long envP) {
		while (envP != 0 && envP != this.Empty_EnvP) {
			final Long handler= Long.valueOf(envP);
			if (envs.contains(handler)) {
				return;
			}
			envs.add(handler);
			envP= this.rEngine.rniParentEnv(envP);
		}
	}
	
	
	public long seqLength(final double length) {
		return this.rEngine.rniEval(this.rEngine.rniCons(
						this.seqIntFunP, this.rEngine.rniCons(
								this.rEngine.rniPutDoubleArray(new double[] { length }), this.NULL_P,
								this.lengthOut_SymP, false ),
						0, true ),
				this.rniSafeBaseExecEnvP );
	}
	
	
	public String getSourceLine(final long objP) {
		this.rniTempEvalAssigned = true;
		if (this.rEngine.rniAssignVarBySym(this.x_SymP, objP, this.rniTempEnvP)) {
			final String line;
			final long lineP= this.rEngine.rniEval(this.deparseLineXCallP, this.rniTempEnvP);
			if (lineP != 0
					&& (line = this.rEngine.rniGetString(lineP)) != null
					&& line.length() > 0 ) {
	//			return args.substring(9,);
				return line;
			}
		}
		return null;
	}
	
	public String[] getSourceLines(final long objP) {
		this.rniTempEvalAssigned = true;
		if (this.rEngine.rniAssignVarBySym(this.x_SymP, objP, this.rniTempEnvP)) {
			final long linesP= this.rEngine.rniEval(this.deparseLinesXCallP, this.rniTempEnvP);
			if (linesP != 0) {
				return this.rEngine.rniGetStringArray(linesP);
			}
		}
		return null;
	}
	
	public boolean setOption(final long symP, final long valP) {
		final long p= this.rEngine.rniEval(this.rEngine.rniCons(
						this.optionsSymP, this.rEngine.rniCons(
								valP, this.NULL_P,
								symP, false),
						0, true),
				this.rniSafeGlobalExecEnvP );
		return (p != 0);
	}
	
	
	public JRIEnvironmentImpl createEnvObject(final long objP,
			final RObject[] itemObjects, final String[] names, final int length,
			final boolean loadClassName) {
		final byte type;
		String envName;
		if (objP == this.Base_EnvP) {
			type= REnvironment.ENVTYPE_BASE;
			envName= REnvironment.ENVNAME_BASE;
		}
		else if (objP == this.Global_EnvP) {
			type= REnvironment.ENVTYPE_GLOBAL;
			envName= REnvironment.ENVNAME_GLOBAL;
		}
		else if (objP == this.Empty_EnvP) {
			type= REnvironment.ENVTYPE_EMTPY;
			envName= REnvironment.ENVNAME_EMPTY;
		}
		else if (objP == this.BaseNamespace_EnvP) {
			type= REnvironment.ENVTYPE_NAMESPACE;
			envName= REnvironment.ENVNAME_BASE;
		}
		else if (objP == this.Autoload_EnvP) {
			type= REnvironment.ENVTYPE_AUTOLOADS;
			envName= REnvironment.ENVNAME_AUTOLOADS;
		}
		else {
			envName= this.rEngine.rniGetNamespaceEnvName(objP);
			if (envName != null) {
				type= REnvironment.ENVTYPE_NAMESPACE;
			}
			else {
				envName= this.rEngine.rniGetAttrStringBySym(objP, this.name_SymP);
				if (envName != null && envName.startsWith("package:")) {
					type= REnvironment.ENVTYPE_PACKAGE;
				}
				else {
					type= 0;
				}
			}
		}
		
		return new JRIEnvironmentImpl(envName, objP, itemObjects, names, length,
				type, (loadClassName) ? this.rEngine.rniGetClassAttrString(objP) : null );
	}
	
}
