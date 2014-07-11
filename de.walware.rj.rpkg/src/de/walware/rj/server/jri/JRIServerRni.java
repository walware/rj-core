/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
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

import de.walware.rj.RjInitFailedException;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RDataUtil;
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
import de.walware.rj.server.srvImpl.AbstractServerControl;


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
	
	public final long p_NULL;
	public final long p_Unbound;
	public final long p_MissingArg;
	public final long p_BaseEnv;
	public final long p_GlobalEnv;
	public final long p_EmptyEnv;
	public final long p_AutoloadEnv;
	public final long p_RJTempEnv;
	
	public final long p_AssignSymbol;
	public final long p_BlockSymbol;
	public final long p_DotsSymbol;
	public final long p_DotAllMTable;
	public final long p_atSymbol;
	public final long p_ClassSymbol;
	public final long p_classSymbol;
	public final long p_dimNamesSymbol;
	public final long p_editSymbol;
	public final long p_envSymbol;
	public final long p_exprSymbol;
	public final long p_errorSymbol;
	public final long p_imaginarySymbol;
	public final long p_idSymbol;
	public final long p_isGenericSymbol;
	public final long p_fdefSymbol;
	public final long p_filenameSymbol;
	public final long p_flagsSymbol;
	public final long p_functionSymbol;
	public final long p_fromSymbol;
	public final long p_levelsSymbol;
	public final long p_linesSymbol;
	public final long p_linesSrcrefSymbol;
	public final long p_nameSymbol;
	public final long p_namesSymbol;
	public final long p_newSymbol;
	public final long p_onExitSymbol;
	public final long p_originalSymbol;
	public final long p_realSymbol;
	public final long p_rowNamesSymbol;
	public final long p_signatureSymbol;
	public final long p_srcfileSymbol;
	public final long p_srcrefSymbol;
	public final long p_timestampSymbol;
	public final long p_traceSymbol;
	public final long p_untraceSymbol;
	public final long p_valueSymbol;
	public final long p_whatSymbol;
	public final long p_whereSymbol;
	public final long p_wdSymbol;
	public final long p_xSymbol;
	public final long p_zSymbol;
	
	public final long p_appFilePathSymbol;
	public final long p_appElementIdSymbol;
	public final long p_dbgElementIdSymbol;
	
	public final long p_TRUE;
	public final long p_FALSE;
	
	public final long p_factorClassString;
	public final long p_orderedClassString;
	public final long p_dataframeClassString;
	public final long p_complexFun;
	public final long p_ReFun;
	public final long p_ImFun;
	
	private final long p_seqIntFun;
	private final long p_lengthOutSymbol;
	
	private final long tryCatchFunP;
	private final long slotNamesFunP;
	private final long getFHeaderFunP;
	private final long deparseLineXCallP;
	private final long deparseLinesXCallP;
	private final long getEnvNameFunP;
	
	public final long p_evalTryCatch_errorExpr;
	public final long p_evalTemp_classExpr;
	public final long p_evalTemp_rmExpr;
	public final long p_evalDummyExpr;
	
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
	
	private boolean rniEvalTempAssigned;
	
	boolean rniInterrupted;
	
	
	public JRIServerRni(final Rengine rEngine) {
		this.rEngine = rEngine;
		
		final int savedProtected = saveProtected();
		try {
			this.p_NULL = this.rEngine.rniSpecialObject(Rengine.SO_NilValue);
			this.p_Unbound = this.rEngine.rniSpecialObject(Rengine.SO_UnboundValue);
			this.p_MissingArg = this.rEngine.rniSpecialObject(Rengine.SO_MissingArg);
			this.p_BaseEnv = this.rEngine.rniSpecialObject(Rengine.SO_BaseEnv);
			this.p_GlobalEnv = this.rEngine.rniSpecialObject(Rengine.SO_GlobalEnv);
			this.p_EmptyEnv = this.rEngine.rniSpecialObject(Rengine.SO_EmptyEnv);
			{	final long p = this.rEngine.rniEval(this.rEngine.rniInstallSymbol(".AutoloadEnv"), //$NON-NLS-1$
						this.p_BaseEnv );
				this.p_AutoloadEnv = (p != 0 && this.rEngine.rniExpType(p) == REXP.ENVSXP) ? p : 0;
			}
			
			this.p_AssignSymbol = this.rEngine.rniInstallSymbol("<-"); //$NON-NLS-1$
			this.p_BlockSymbol = this.rEngine.rniInstallSymbol("{"); //$NON-NLS-1$
			this.p_DotsSymbol = this.rEngine.rniInstallSymbol("..."); //$NON-NLS-1$
			this.p_DotAllMTable = this.rEngine.rniInstallSymbol(".AllMTable"); //$NON-NLS-1$
			this.p_atSymbol = this.rEngine.rniInstallSymbol("at"); //$NON-NLS-1$
			this.p_ClassSymbol = this.rEngine.rniInstallSymbol("Class"); //$NON-NLS-1$
			this.p_classSymbol = this.rEngine.rniInstallSymbol("class"); //$NON-NLS-1$
			this.p_dimNamesSymbol = this.rEngine.rniInstallSymbol("dimnames"); //$NON-NLS-1$
			this.p_editSymbol = this.rEngine.rniInstallSymbol("edit"); //$NON-NLS-1$
			this.p_envSymbol = this.rEngine.rniInstallSymbol("env"); //$NON-NLS-1$
			this.p_errorSymbol = this.rEngine.rniInstallSymbol("error"); //$NON-NLS-1$
			this.p_exprSymbol = this.rEngine.rniInstallSymbol("expr"); //$NON-NLS-1$
			this.p_fdefSymbol = this.rEngine.rniInstallSymbol("fdef"); //$NON-NLS-1$
			this.p_filenameSymbol = this.rEngine.rniInstallSymbol("filename"); //$NON-NLS-1$
			this.p_flagsSymbol = this.rEngine.rniInstallSymbol("flags"); //$NON-NLS-1$
			this.p_functionSymbol = this.rEngine.rniInstallSymbol("function"); //$NON-NLS-1$
			this.p_fromSymbol = this.rEngine.rniInstallSymbol("from"); //$NON-NLS-1$
			this.p_idSymbol = this.rEngine.rniInstallSymbol("id"); //$NON-NLS-1$
			this.p_isGenericSymbol = this.rEngine.rniInstallSymbol("isGeneric"); //$NON-NLS-1$
			this.p_imaginarySymbol = this.rEngine.rniInstallSymbol("imaginary"); //$NON-NLS-1$
			this.p_levelsSymbol = this.rEngine.rniInstallSymbol("levels"); //$NON-NLS-1$
			this.p_linesSymbol = this.rEngine.rniInstallSymbol("lines"); //$NON-NLS-1$
			this.p_linesSrcrefSymbol = this.rEngine.rniInstallSymbol("linesSrcref"); //$NON-NLS-1$
			this.p_nameSymbol = this.rEngine.rniInstallSymbol("name"); //$NON-NLS-1$
			this.p_namesSymbol = this.rEngine.rniInstallSymbol("names"); //$NON-NLS-1$
			this.p_newSymbol = this.rEngine.rniInstallSymbol("new"); //$NON-NLS-1$
			this.p_onExitSymbol = this.rEngine.rniInstallSymbol("on.exit"); //$NON-NLS-1$
			this.p_originalSymbol = this.rEngine.rniInstallSymbol("original"); //$NON-NLS-1$
			this.p_realSymbol = this.rEngine.rniInstallSymbol("real"); //$NON-NLS-1$
			this.p_rowNamesSymbol = this.rEngine.rniInstallSymbol("row.names"); //$NON-NLS-1$
			this.p_signatureSymbol = this.rEngine.rniInstallSymbol("signature"); //$NON-NLS-1$
			this.p_srcfileSymbol = this.rEngine.rniInstallSymbol("srcfile"); //$NON-NLS-1$
			this.p_srcrefSymbol = this.rEngine.rniInstallSymbol("srcref"); //$NON-NLS-1$
			this.p_timestampSymbol = this.rEngine.rniInstallSymbol("timestamp"); //$NON-NLS-1$
			this.p_traceSymbol = this.rEngine.rniInstallSymbol("trace"); //$NON-NLS-1$
			this.p_untraceSymbol = this.rEngine.rniInstallSymbol("untrace"); //$NON-NLS-1$
			this.p_valueSymbol = this.rEngine.rniInstallSymbol("value"); //$NON-NLS-1$
			this.p_whatSymbol = this.rEngine.rniInstallSymbol("what"); //$NON-NLS-1$
			this.p_whereSymbol = this.rEngine.rniInstallSymbol("where"); //$NON-NLS-1$
			this.p_wdSymbol = this.rEngine.rniInstallSymbol("wd"); //$NON-NLS-1$
			this.p_xSymbol = this.rEngine.rniInstallSymbol("x"); //$NON-NLS-1$
			this.p_zSymbol = this.rEngine.rniInstallSymbol("z"); //$NON-NLS-1$
			
			this.p_appFilePathSymbol = this.rEngine.rniInstallSymbol("statet.Path");
			this.p_appElementIdSymbol = this.rEngine.rniInstallSymbol("statet.ElementId");
			this.p_dbgElementIdSymbol = this.rEngine.rniInstallSymbol("dbg.ElementId");
			
			this.p_TRUE = this.rEngine.rniPutBoolArray(new boolean[] { true });
			this.rEngine.rniPreserve(this.p_TRUE);
			this.p_FALSE = this.rEngine.rniPutBoolArray(new boolean[] { false });
			this.rEngine.rniPreserve(this.p_FALSE);
			
			this.p_RJTempEnv = this.rEngine.rniEval(this.rEngine.rniCons(
					this.rEngine.rniInstallSymbol("new.env"), this.p_NULL, 0, true ), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.p_RJTempEnv);
			this.p_orderedClassString = this.rEngine.rniPutStringArray(
					new String[] { "ordered", "factor" } ); //$NON-NLS-1$ //$NON-NLS-2$
			this.rEngine.rniPreserve(this.p_orderedClassString);
			this.p_factorClassString = this.rEngine.rniPutString("factor"); //$NON-NLS-1$
			this.rEngine.rniPreserve(this.p_factorClassString);
			this.p_dataframeClassString = this.rEngine.rniPutString("data.frame"); //$NON-NLS-1$
			this.rEngine.rniPreserve(this.p_dataframeClassString);
			
			this.p_complexFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("complex"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.p_complexFun);
			this.p_ReFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("Re"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.p_ReFun);
			this.p_ImFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("Im"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.p_ImFun);
			
			
			this.p_seqIntFun = this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("seq.int"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.p_seqIntFun);
			this.p_lengthOutSymbol = this.rEngine.rniInstallSymbol("length.out"); //$NON-NLS-1$
			
			this.tryCatchFunP = this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("tryCatch"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.tryCatchFunP);
			this.slotNamesFunP = this.rEngine.rniEval(
					this.rEngine.rniParse("methods::.slotNames", 1), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.slotNamesFunP);
			this.getEnvNameFunP = this.rEngine.rniEval(
					this.rEngine.rniInstallSymbol("environmentName"), //$NON-NLS-1$
					this.p_BaseEnv );
			this.rEngine.rniPreserve(this.getEnvNameFunP);
			
			{	final long pasteFunP = protect(this.rEngine.rniEval(
						this.rEngine.rniInstallSymbol("paste"), //$NON-NLS-1$
						this.p_BaseEnv ));
				final long deparseFunP = protect(this.rEngine.rniEval(
						this.rEngine.rniInstallSymbol("deparse"), //$NON-NLS-1$
						this.p_BaseEnv ));
				
				final long controlSymbolP = this.rEngine.rniInstallSymbol("control"); //$NON-NLS-1$
				final long widthcutoffSymbolP = this.rEngine.rniInstallSymbol("width.cutoff"); //$NON-NLS-1$
				final long collapseSymbolP = this.rEngine.rniInstallSymbol("collapse"); //$NON-NLS-1$
				
				final long deparseControlValueP = protect(this.rEngine.rniPutStringArray(
						new String[] { "keepInteger", "keepNA" } )); //$NON-NLS-1$ //$NON-NLS-2$
				final long collapseValueP = protect(this.rEngine.rniPutString("")); //$NON-NLS-1$
				
				{	// function(x)paste(deparse(expr=args(name=x),control=c("keepInteger", "keepNA"),width.cutoff=500L),collapse="")
					final long fArgsP = protect(this.rEngine.rniCons(
							this.p_MissingArg, this.p_NULL,
							this.p_xSymbol, false ));
					final long argsFunP = protect(this.rEngine.rniEval(
							this.rEngine.rniInstallSymbol("args"), //$NON-NLS-1$
							this.p_BaseEnv ));
					final long argsCallP = protect(this.rEngine.rniCons(
							argsFunP, this.rEngine.rniCons(
									this.p_xSymbol, this.p_NULL,
									this.p_nameSymbol, false ),
							0, true ));
					final long deparseCallP = protect(this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									argsCallP, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 500 }), this.p_NULL,
													widthcutoffSymbolP, false ),
											controlSymbolP, false ),
									this.p_exprSymbol, false ),
							0, true ));
					final long fBodyP = this.rEngine.rniCons(
							pasteFunP, this.rEngine.rniCons(
									deparseCallP, this.rEngine.rniCons(
											collapseValueP, this.p_NULL,
											collapseSymbolP, false ),
									0, false ),
							0, true );
					this.getFHeaderFunP = this.rEngine.rniEval(this.rEngine.rniCons(
							this.p_functionSymbol, this.rEngine.rniCons(
									fArgsP, this.rEngine.rniCons(
											fBodyP, this.p_NULL,
											0, false ),
									0, false ),
							0, true ),
							this.p_BaseEnv );
					this.rEngine.rniPreserve(this.getFHeaderFunP);
				}
				
				{	// paste(deparse(expr=x,control=c("keepInteger", "keepNA"),width.cutoff=500L),collapse="")
					final long deparseCallP = protect(this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									this.p_xSymbol, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 500 }), this.p_NULL,
													widthcutoffSymbolP, false ),
											controlSymbolP, false ),
									this.p_exprSymbol, false ),
							0, true ));
					this.rEngine.rniPreserve(deparseCallP);
					this.deparseLineXCallP = this.rEngine.rniCons(
							pasteFunP, this.rEngine.rniCons(
									deparseCallP, this.rEngine.rniCons(
											collapseValueP, this.p_NULL,
											collapseSymbolP, false ),
									0, false ),
							0, true );
					this.rEngine.rniPreserve(this.deparseLineXCallP);
				}
				{	// deparse(expr=x,control=c("keepInteger", "keepNA"),width.cutoff=80L)
					this.deparseLinesXCallP = this.rEngine.rniCons(
							deparseFunP, this.rEngine.rniCons(
									this.p_xSymbol, this.rEngine.rniCons(
											deparseControlValueP, this.rEngine.rniCons(
													this.rEngine.rniPutIntArray(new int[] { 80 }), this.p_NULL,
													widthcutoffSymbolP, false ),
											controlSymbolP, false ),
									this.p_exprSymbol, false ),
							0, true);
					this.rEngine.rniPreserve(this.deparseLinesXCallP);
				}
			}
			
			this.p_evalTryCatch_errorExpr = this.rEngine.rniCons(
					this.rEngine.rniEval(this.rEngine.rniParse("function(e){" +
							"s<-raw(5);" +
							"class(s)<-\".rj.eval.error\";" +
							"attr(s,\"error\")<-e;" +
							"attr(s,\"output\")<-paste(capture.output(print(e)),collapse=\"\\n\");" +
							"invisible(s);}", 1 ), 0 ), this.p_NULL,
					this.p_errorSymbol, false );
			this.rEngine.rniPreserve(this.p_evalTryCatch_errorExpr);
			
			this.p_evalTemp_classExpr = this.rEngine.rniParse("class(x);", 1);
			this.rEngine.rniPreserve(this.p_evalTemp_classExpr);
			this.p_evalTemp_rmExpr = this.rEngine.rniParse("rm(x);", 1);
			this.rEngine.rniPreserve(this.p_evalTemp_rmExpr);
			
			this.p_evalDummyExpr = this.rEngine.rniParse("1+1;", 1);
			this.rEngine.rniPreserve(this.p_evalDummyExpr);
			
			if (LOGGER.isLoggable(Level.FINER)) {
				final StringBuilder sb = new StringBuilder("Pointers:");
				
				final Field[] fields = getClass().getDeclaredFields();
				for (final Field field : fields) {
					final String name = field.getName();
					if (name.startsWith("p_") && Long.TYPE.equals(field.getType())) {
						sb.append("\n\t");
						sb.append(name.substring(2));
						sb.append(" = ");
						try {
							final long p = field.getLong(this);
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
				throw new RjInitFailedException("Base functions are missing (check 'Renviron')");
			}
		}
		catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize engine.", e);
			AbstractServerControl.exit(162);
			throw new RuntimeException(); // for final fields
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
		
		if (this.rniEvalTempAssigned) {
			this.rEngine.rniEval(this.p_evalTemp_rmExpr, this.p_RJTempEnv);
			this.rniEvalTempAssigned = false;
		}
	}
	
	
	public long createFCall(final String name, final RList args) throws RjsException {
		long argsP = this.p_NULL;
		for (int i = (int) args.getLength() - 1; i >= 0; i--) {
			final String argName = args.getName(i);
			final RObject argValue = args.get(i);
			final long argValueP;
			if (argValue != null) {
				argValueP = assignDataObject(argValue);
			}
			else {
				argValueP = this.p_MissingArg;
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
			return this.p_GlobalEnv;
		}
		try {
			long envP = 0;
			switch (data.getRObjectType()) {
			case RObject.TYPE_REFERENCE:
				envP = ((RReference) data).getHandle();
				break;
			case RObject.TYPE_LANGUAGE:
				envP = evalExpr(assignDataObject(data), 0, CODE_DATA_COMMON);
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
						exprP, this.p_evalTryCatch_errorExpr,
						this.p_exprSymbol, false ),
				0, true );
		final long objP = this.rEngine.rniEval(exprP, envP);
		if (objP == 0) {
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			throw new IllegalStateException("JRI returned error code " + objP + " (pointer = 0x" + Long.toHexString(exprP) + ")");
		}
		this.rEngine.rniProtect(objP);
		this.rniProtectedCounter++;
		if (this.rEngine.rniExpType(objP) == REXP.RAWSXP) {
			final String className1 = this.rEngine.rniGetClassAttrString(objP);
			if (className1 != null && className1.equals(".rj.eval.error")) {
				String message = null;
				final long outputP = this.rEngine.rniGetAttr(objP, "output");
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
		RStore names;
		long objP;
		switch(obj.getRObjectType()) {
		case RObject.TYPE_NULL:
		case RObject.TYPE_MISSING:
			return this.p_NULL;
		case RObject.TYPE_VECTOR: {
			objP = assignDataStore(obj.getData());
			names = ((RVector<?>) obj).getNames();
			if (names != null) {
				this.rEngine.rniSetAttrBySym(objP, this.p_namesSymbol, assignDataStore(names));
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
				this.rEngine.rniSetAttrBySym(objP, this.p_namesSymbol, assignDataStore(names));
			}
			names = list.getRowNames();
			this.rEngine.rniSetAttrBySym(objP, this.p_rowNamesSymbol, (names != null) ?
					assignDataStore(names) : seqLength(list.getRowCount()) );
			this.rEngine.rniSetAttrBySym(objP, this.p_classSymbol, this.p_dataframeClassString);
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
				this.rEngine.rniSetAttrBySym(objP, this.p_namesSymbol, assignDataStore(names));
			}
			return objP; }
		case RObject.TYPE_REFERENCE:
			return ((RReference) obj).getHandle();
		case RObject.TYPE_S4OBJECT: {
			final RS4Object s4obj = (RS4Object) obj;
			objP = this.p_NULL;
			for (int i = (int) s4obj.getLength()-1; i >= 0; i--) {
				final RObject slotObj = s4obj.get(i);
				if (slotObj != null && slotObj.getRObjectType() != RObject.TYPE_MISSING) {
					objP= checkAndProtect(this.rEngine.rniCons(
							assignDataObject(slotObj), objP,
							this.rEngine.rniInstallSymbol(s4obj.getName(i)), false ));
				}
			}
			return protect(evalExpr(this.rEngine.rniCons(
					this.p_newSymbol, this.rEngine.rniCons(
							this.rEngine.rniPutString(s4obj.getRClassName()), objP,
							this.p_ClassSymbol, false ),
					0, true ),
					0, (CODE_DATA_ASSIGN_DATA | 0x8) )); }
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
				"The assignment for R objects of type " + RDataUtil.getObjectTypeName(obj.getRObjectType()) + " is not yet supported." );
	}
	
	public long assignDataStore(final RStore data) throws RNullPointerException {
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
					this.p_complexFun, this.rEngine.rniCons(
							realP, this.rEngine.rniCons(
									imaginaryP, this.p_NULL,
									this.p_imaginarySymbol, false ),
							this.p_realSymbol, false ),
					0, true ),
					this.p_BaseEnv )); }
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
			this.rEngine.rniSetAttr(objP, "levels",
					this.rEngine.rniPutStringArray(factor.getJRILevelsArray()) );
			this.rEngine.rniSetAttr(objP, "class",
					factor.isOrdered() ? this.p_orderedClassString : this.p_factorClassString );
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
					{	final long levelsP = this.rEngine.rniGetAttrBySym(objP, this.p_levelsSymbol);
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
							new JRIArrayImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									className1, dim ) :
							new JRIArrayImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, dim, getDimNames(objP, dim.length) );
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RNumericStore>(
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
					return new RReferenceImpl(objP, RObject.TYPE_REFERENCE, "environment");
				}
				final String[] names = this.rEngine.rniGetStringArray(this.rEngine.rniListEnv(objP, true));
				if (names != null) {
					final String className1;
					if (mode != EVAL_MODE_DATASLOT) {
						className1 = this.rEngine.rniGetClassAttrString(objP);
					}
					else {
						className1 = null;
					}
					
					if (objP == this.p_AutoloadEnv || names.length > this.maxEnvsLength) {
						return new JRIEnvironmentImpl(getEnvName(objP), objP, null, null,
								names.length, className1 );
					}
					
					final RObject[] itemObjects = new RObject[names.length];
					for (int i = 0; i < names.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						final long itemP = this.rEngine.rniGetVar(objP, names[i]);
						if (itemP != 0) {
							itemObjects[i] = createDataObject(itemP, flags, EVAL_MODE_DEFAULT);
							continue;
						}
						else {
							itemObjects[i] = RMissing.INSTANCE;
							continue;
						}
					}
					return new JRIEnvironmentImpl(getEnvName(objP), objP, itemObjects, names,
							names.length, className1 );
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
				if (objP == this.p_MissingArg) {
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
//				final long classP = this.rEngine.rniEval(this.rEngine.rniCons(this.p_classFun,
//						this.rEngine.rniCons(objP, this.p_NULL, this.p_xSymbol, false), 0, true),
//						this.p_BaseEnv);
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
		final long classP = this.rEngine.rniGetAttrBySym(objP, this.p_classSymbol);
		String className = null;
		if (classP != 0 && classP != this.p_NULL) {
			className = this.rEngine.rniGetString(classP);
			final long slotNamesP = this.rEngine.rniEval(this.rEngine.rniCons(
					this.slotNamesFunP, this.rEngine.rniCons(
							classP, this.p_NULL,
							this.p_xSymbol, false ),
					0, true ),
					0 );
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
			this.rniEvalTempAssigned = true;
			if (this.rEngine.rniAssign("x", objP, this.p_RJTempEnv)
					&& (classP = this.rEngine.rniEval(this.p_evalTemp_classExpr, this.p_RJTempEnv)) != 0
					&& (className1 = this.rEngine.rniGetString(classP)) != null ) {
				return className1;
			}
		}
		return "<unknown>";
	}
	
	private String[] getNames(final long objP) {
		final long namesP = this.rEngine.rniGetAttrBySym(objP, this.p_namesSymbol);
		return (namesP != 0) ? this.rEngine.rniGetStringArray(namesP) : null;
	}
	
	private SimpleRListImpl<RStore> getDimNames(final long objP, final int length) {
		final long namesP = this.rEngine.rniGetAttrBySym(objP, this.p_dimNamesSymbol);
		if (this.rEngine.rniExpType(namesP) == REXP.VECSXP) {
			final long[] names1P = this.rEngine.rniGetVector(namesP);
			if (names1P != null && names1P.length == length) {
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
				return new SimpleRListImpl<RStore>(names1, names0);
			}
		}
		return null;
	}
	
	private String[] getRowNames(final long objP) {
		final long namesP = this.rEngine.rniGetAttrBySym(objP, this.p_rowNamesSymbol);
		return (namesP != 0) ? this.rEngine.rniGetStringArray(namesP) : null;
	}
	
	private double[] getComplexRe(final long objP) {
		final double[] num;
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(
				this.p_ReFun, this.rEngine.rniCons(
						objP, this.p_NULL,
						this.p_zSymbol, false ),
				0, true ),
				this.p_BaseEnv );
		if (numP == 0) {
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			throw new IllegalStateException("JRI returned error code " + numP);
		}
		if ((num = this.rEngine.rniGetDoubleArray(numP)) != null) {
			return num;
		}
		throw new IllegalStateException();
	}
	
	private double[] getComplexIm(final long objP) {
		final double[] num;
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(
				this.p_ImFun, this.rEngine.rniCons(
						objP, this.p_NULL,
						this.p_zSymbol, false ),
				0, true ),
				this.p_BaseEnv );
		if (numP == 0) {
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			throw new IllegalStateException("JRI returned error code " + numP);
		}
		if ((num = this.rEngine.rniGetDoubleArray(numP)) != null) {
			return num;
		}
		throw new IllegalStateException();
	}
	
	private String getFHeader(final long cloP) {
		final String args;
		final long argsP;
		if ((argsP = this.rEngine.rniEval(this.rEngine.rniCons(
						this.getFHeaderFunP, this.rEngine.rniCons(
								cloP, this.p_NULL,
								this.p_xSymbol, false ),
						0, true ),
						this.p_BaseEnv )) != 0
				&& (args = this.rEngine.rniGetString(argsP)) != null
				&& args.length() >= 11 ) { // "function ()".length
//			return args.substring(9,);
			return args;
		}
		return null;
	}
	
	public long seqLength(final double length) {
		return this.rEngine.rniEval(this.rEngine.rniCons(
				this.p_seqIntFun, this.rEngine.rniCons(
						this.rEngine.rniPutDoubleArray(new double[] { length }), this.p_NULL,
						this.p_lengthOutSymbol, false ),
				0, true ),
				this.p_BaseEnv );
	}
	
	
	public String getSourceLine(final long objP) {
		this.rniEvalTempAssigned = true;
		if (this.rEngine.rniAssign("x", objP, this.p_RJTempEnv)) {
			final String line;
			final long lineP;
			if ((lineP = this.rEngine.rniEval(this.deparseLineXCallP, this.p_RJTempEnv)) != 0
					&& (line = this.rEngine.rniGetString(lineP)) != null
					&& line.length() > 0 ) {
	//			return args.substring(9,);
				return line;
			}
		}
		return null;
	}
	
	public String[] getSourceLines(final long objP) {
		this.rniEvalTempAssigned = true;
		if (this.rEngine.rniAssign("x", objP, this.p_RJTempEnv)) {
			final long linesP;
			if ((linesP = this.rEngine.rniEval(this.deparseLinesXCallP, this.p_RJTempEnv)) != 0) {
				return this.rEngine.rniGetStringArray(linesP);
			}
		}
		return null;
	}
	
	public String getEnvName(final long envP) {
		if (envP == this.p_GlobalEnv) {
			return "R_GlobalEnv";
		}
		else if (envP == this.p_EmptyEnv) {
			return "R_EmptyEnv";
		}
		else {
			final long p;
			if ((p = this.rEngine.rniEval(protect(this.rEngine.rniCons(
					this.getEnvNameFunP, this.rEngine.rniCons(
							envP, this.p_NULL,
							this.p_envSymbol, false ),
					0, true )),
					this.p_BaseEnv )) != 0) {
				return this.rEngine.rniGetString(p);
			}
			return null;
		}
	}
	
	public void addAllEnvs(final List<Long> envs, long envP) {
		while (envP != 0 && envP != this.p_EmptyEnv) {
			final Long handler= Long.valueOf(envP);
			if (envs.contains(handler)) {
				return;
			}
			envs.add(handler);
			envP= this.rEngine.rniParentEnv(envP);
		}
	}
	
}
