/*******************************************************************************
 * Copyright (c) 2008-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_CONTEXT;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_DEBUG;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_TRACE;
import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import de.walware.rj.RjException;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.server.RjsException;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.dbg.CallStack;
import de.walware.rj.server.dbg.DbgEnablement;
import de.walware.rj.server.dbg.DbgFilterState;
import de.walware.rj.server.dbg.ElementTracepointInstallationReport;
import de.walware.rj.server.dbg.ElementTracepointInstallationRequest;
import de.walware.rj.server.dbg.ElementTracepointPositions;
import de.walware.rj.server.dbg.FrameContext;
import de.walware.rj.server.dbg.FrameContextDetailRequest;
import de.walware.rj.server.dbg.SetDebugReport;
import de.walware.rj.server.dbg.SetDebugRequest;
import de.walware.rj.server.dbg.SrcfileData;
import de.walware.rj.server.dbg.Srcref;
import de.walware.rj.server.dbg.Tracepoint;
import de.walware.rj.server.dbg.TracepointEvent;
import de.walware.rj.server.dbg.TracepointListener;
import de.walware.rj.server.dbg.TracepointPosition;
import de.walware.rj.server.dbg.TracepointState;
import de.walware.rj.server.dbg.TracepointStatesUpdate;


final class JRIServerDbg {
	
	
	private static final int[] NA_SRCREF = new int[] {
			Integer.MIN_VALUE, Integer.MIN_VALUE,
			Integer.MIN_VALUE, Integer.MIN_VALUE,
			Integer.MIN_VALUE, Integer.MIN_VALUE,
	};
	
	private static final int[] LOCAL_METHOD_INDEX = new int[] { 2, 3 };
	private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\\#");
	
	
	private static final class FunInfo {
		
		public static final int INVALID = -1;
		
		public static final int FILE_PATH = 1;
		public static final int FILE_NAME = 2;
		
		public static final int DONE_CLEAR = 1;
		public static final int DONE_SET = 2;
		
		public final long orgFunP;
		public final int[] subIndex;
		public long currentMainFunP;
		public final long orgMainFunP;
		public final String rawMethodSignature;
		
		// preCheck
		public long orgBodyP;
		public long srcfileP;
		int fileType;
		public String file;
		
		public final int[] done;
		
		
		public FunInfo(final long orgFunP, final long currentFunP, final String signature) {
			this.orgFunP = orgFunP;
			this.subIndex = null;
			this.orgMainFunP = orgFunP;
			this.currentMainFunP = currentFunP;
			this.rawMethodSignature = signature;
			this.done = new int[1];
		}
		
		public FunInfo(final long orgFunP, final int[] subIndex, final FunInfo main) {
			this.orgFunP = orgFunP;
			this.subIndex = subIndex;
			this.orgMainFunP = main.orgMainFunP;
			this.currentMainFunP = main.currentMainFunP;
			this.rawMethodSignature = main.rawMethodSignature;
			this.done = main.done;
		}
		
	}
	
	private static final class CallStackFrame extends CallStack.Frame {
		
		public CallStackFrame(final int position, final String call) {
			super(position, call);
		}
		
		
		void setHandle(final long handle) {
			this.handle = handle;
		}
		
		void setFileName(final String fileName) {
			this.fileName = fileName;
		}
		
		void setFileTimestamp(final long fileTimestamp) {
			this.fileTimestamp = fileTimestamp;
		}
		
		void setExprSrcref(final int[] srcref) {
			this.exprSrcref = srcref;
		}
		
	}
	
	private static final class TracepointStateWithData extends TracepointState {
		
		
		private long parsedExprP;
		
		
		public TracepointStateWithData(final TracepointState state) {
			super(state.getType(), state.getFilePath(), state.getId(),
					state.getElementId(), state.getIndex(),
					state.getElementLabel(), state.getFlags(), state.getExpr() );
		}
		
		
		public void setParsedExpr(final long p) {
			if (p == 0) {
				this.flags |= FLAG_EXPR_INVALID;
			}
			this.parsedExprP = p;
		}
		
		public long getExprP() {
			return this.parsedExprP;
		}
		
	}
	
	
	private final Rengine rEngine;
	private final JRIServerRni rni;
	private final JRIServerUtils utils;
	
	private boolean disabled;
	private boolean disabledBySafeMode;
	private boolean savedTracingState;
	
	private boolean stepFilterEnabled = !Boolean.parseBoolean(
			System.getProperty("de.walware.rj.dbg.stepfilter.disabled"));
	
	private boolean deferredSuspend;
	private final List<Long> contextTmpDebugFrames = new ArrayList<Long>();
	private final List<Long> contextTmpDebugFuns = new ArrayList<Long>();
	
	private boolean checkToRelease;
	private final List<Long> toRelease = new ArrayList<Long>();
	
	private final Map<String, List<TracepointState>> tracepointMap = new HashMap<String, List<TracepointState>>();
	private final TracepointListener tracepointListener;
	
	private boolean breakpointsEnabled = true;
	private TracepointState hitBreakpointState;
	private int hitBreakpointFlags;
	private int[] hitBreakpointSrcref;
	private long hitBreakpointSrcfile;
	
	private final long sysNframeCallP;
	private final long sysFramesCallP;
	private final long sysCalls0CallP;
	private final long sysCallFunP;
	private final long sysCall0CallP;
	private final long sysFunctionFunP;
	
	private final long isFunP;
	private final long traceableStringP;
	
	private final long editFArgsP;
	private final long stepNextValueP;
	private final long stepSrcrefTmplP;
	private final long breakpointTmplP;
	private final long browserExprP;
	private final long getTraceExprEvalEnvCallP;
	
	private final long enableTraceingCallP;
	private final long disableTraceingCallP;

	
	
	public JRIServerDbg(final Rengine rEngine, final JRIServerRni rni,
			final JRIServerUtils utils, final TracepointListener tracepointListener ) {
		this.rEngine = rEngine;
		this.rni = rni;
		this.utils = utils;
		
		this.tracepointListener = tracepointListener;
		
		this.sysNframeCallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.nframe"),
						this.rni.p_BaseEnv), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysNframeCallP);
		this.sysFramesCallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.frames"),
						this.rni.p_BaseEnv ), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysFramesCallP);
		this.sysCalls0CallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rni.protect(this.rEngine.rniCons(
						this.rni.p_functionSymbol, this.rEngine.rniCons(
								this.rni.p_NULL, this.rEngine.rniCons(
										this.rEngine.rniCons(
												this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.calls"),
														this.rni.p_BaseEnv ), this.rni.p_NULL,
												0, true ), this.rni.p_NULL,
										0, true),
								0, false ),
						0, true )),
						this.rni.p_BaseEnv ), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysCalls0CallP);
		this.sysCallFunP = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.call"),
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysCallFunP);
		this.sysCall0CallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rni.protect(this.rEngine.rniCons(
						this.rni.p_functionSymbol, this.rEngine.rniCons(
								this.rni.p_NULL, this.rEngine.rniCons(
										this.rEngine.rniCons(
												this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.call"),
														this.rni.p_BaseEnv ), this.rni.p_NULL,
												0, true ), this.rni.p_NULL,
										0, true),
								0, false ),
						0, true )),
						this.rni.p_BaseEnv ), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysCall0CallP);
		this.sysFunctionFunP = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.function"),
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysFunctionFunP);
		this.isFunP = this.rEngine.rniEval(this.rEngine.rniParse("methods::is", 1),
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.isFunP);
		this.traceableStringP = this.rEngine.rniPutString("traceable");
		this.rEngine.rniPreserve(this.traceableStringP);
		
		this.editFArgsP = this.rEngine.rniCons(
				this.rni.p_MissingArg, this.rEngine.rniCons(
						this.rni.p_MissingArg, this.rni.p_NULL,
						this.rni.p_DotsSymbol, false ),
				rni.p_fdefSymbol, false );
		this.rEngine.rniPreserve(this.editFArgsP);
		
		this.stepNextValueP = this.rEngine.rniPutString("browser:n");
		this.rEngine.rniPreserve(this.stepNextValueP);
		
		this.stepSrcrefTmplP = this.rEngine.rniPutIntArray(NA_SRCREF);
		this.rEngine.rniPreserve(this.stepSrcrefTmplP);
		this.rEngine.rniSetAttrBySym(this.stepSrcrefTmplP, this.rni.p_whatSymbol, this.stepNextValueP);
		
		this.breakpointTmplP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"{ if (\"rj\" %in% loadedNamespaces()) rj:::.breakpoint() }", 1 ),
				0 );
		this.rEngine.rniPreserve(this.breakpointTmplP);
		
		this.browserExprP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"{ browser(skipCalls= 3L) }", 1 ),
				0 );
		this.rEngine.rniPreserve(this.browserExprP);
		
		this.getTraceExprEvalEnvCallP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"base::new.env(parent= base::sys.frame(-1))", 1), 0);
		this.rEngine.rniPreserve(this.getTraceExprEvalEnvCallP);
		
		this.enableTraceingCallP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"tracingState(TRUE)", 1 ),
				0 );
		this.rEngine.rniPreserve(this.enableTraceingCallP);
		
		this.disableTraceingCallP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"tracingState(FALSE)", 1 ),
				0 );
		this.rEngine.rniPreserve(this.disableTraceingCallP);
	}
	
	
	public void beginSafeMode() {
		if (this.disabled) {
			return;
		}
		this.disabled = true;
		this.disabledBySafeMode = true;
		
		final long p = this.rEngine.rniEval(this.disableTraceingCallP, this.rni.p_BaseEnv);
		this.savedTracingState = (p != 0 && this.rEngine.rniIsTrue(p));
	}
	
	public void endSafeMode() {
		if (!this.disabledBySafeMode) {
			return;
		}
		this.disabledBySafeMode = false;
		
		if (!this.disabled) { // ?
			return;
		}
		if (this.deferredSuspend) {
			doSuspend();
		}
		this.disabled = false;
		
		if (this.savedTracingState) {
			this.savedTracingState = false;
			this.rEngine.rniEval(this.enableTraceingCallP, this.rni.p_BaseEnv);
		}
	}
	
	public boolean isSafeMode() {
		return this.disabled;
	}
	
	public synchronized void setEnablement(final DbgEnablement request) {
		this.breakpointsEnabled = request.getBreakpointsEnabled();
	}
	
	
	public String handleBrowserPrompt(final String prompt) {
		if (this.disabled) {
			return "c\n";
		}
		final long srcrefP;
		{	final long call = this.rEngine.rniEval(this.sysCall0CallP, 0);
			srcrefP = (call != 0) ? this.rEngine.rniGetAttrBySym(call, this.rni.p_srcrefSymbol) : 0;
		}
		if (srcrefP != 0) {
			final long p = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_whatSymbol);
			if (p != 0) {
				final String s = this.rEngine.rniGetString(p);
				if (s != null && s.length() > 8 && s.startsWith("browser:")) {
					return s.substring(8)+"\n";
				}
			}
		}
		
		final TracepointState state = this.hitBreakpointState;
		if (state != null) {
			if (srcrefP != 0) {
				final int[] current = this.rEngine.rniGetIntArray(srcrefP);
				if (Arrays.equals(this.hitBreakpointSrcref, current)) { // compare srcfile?
					sendNotification(new TracepointEvent(TracepointEvent.KIND_ABOUT_TO_HIT,
							state.getType(), state.getFilePath(), state.getId(),
							state.getElementLabel(), this.hitBreakpointFlags, null ));
				}
			}
			this.hitBreakpointState = null;
		}
		
		clearContext();
		
		return null;
	}
	
	public void rCancelled() {
		this.hitBreakpointState = null;
	}
	
	/**
	 * Executes dbg commands from R
	 * 
	 * @param commandId the specified command id
	 * @param argsP pointer to command specified arguments
	 * @return pointer to answer or <code>0</code>
	 * @throws RjException if an error occurred when executing the command
	 */
	public long execRJCommand(final String commandId, final long argsP) throws RjException {
		this.rni.newDataLevel(255, Integer.MAX_VALUE, Integer.MAX_VALUE);
		final int savedProtected = this.rni.saveProtected();
		try {
			this.rni.protect(argsP);
			if (commandId.equals("checkBreakpoint")) {
				return checkBreakpoint(argsP);
			}
		}
		finally {
			this.rni.looseProtected(savedProtected);
			this.rni.exitDataLevel();
		}
		return 0;
	}
	
	
	public RjsStatus setFilterState(final DbgFilterState state) {
		synchronized (this) {
			this.stepFilterEnabled = state.stepFilterEnabled();
		}
		return RjsStatus.OK_STATUS;
	}
	
	public boolean isStepFilterEnabled() {
		return this.stepFilterEnabled;
	}
	
	
	/**
	 * Loads the list of the current frames
	 * 
	 * @return the frame list or <code>null</code> if not available
	 * @throws RjsException
	 */
	public CallStack loadCallStack() throws RjsException {
		int n = getNFrame();
		if (n < 0) {
			return null;
		}
		
		final List<CallStackFrame> list = new ArrayList<CallStackFrame>(n+1);
		{	long cdr = this.rni.evalExpr(this.sysCalls0CallP, 0, CODE_DBG_CONTEXT);
			if (cdr == 0 || this.rEngine.rniExpType(cdr) != REXP.LISTSXP) {
				if (n == 0 && cdr == this.rni.p_NULL) {
					cdr = 0;
				}
				else {
					return null;
				}
			}
			int i = 0;
			String call = null;
			while (true) {
				final CallStackFrame item = new CallStackFrame(i, call);
				call = null;
				long srcrefP = 0;
				if (cdr != 0) {
					final long car = this.rEngine.rniCAR(cdr);
					if (car != 0 && this.rEngine.rniExpType(car) == REXP.LANGSXP) {
						if (i < n) {
							call = this.rni.getSourceLine(car);
						}
						
						srcrefP = this.rEngine.rniGetAttrBySym(car, this.rni.p_srcrefSymbol);
					}
				}
				
				if (srcrefP != 0) {
					final long srcfileP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_srcfileSymbol);
					if (srcfileP != 0) {
						final String fileName = getFileName(srcfileP);
						if (fileName != null) {
							item.setFileName(fileName);
							item.setFileTimestamp(getFileTimestamp(srcfileP));
						}
						
						int[] srcref = getSrcrefObject(srcrefP);
						if (srcref != null) {
							final int[] add = getSrcrefObject(this.rEngine.rniGetVarBySym(
									srcfileP, this.rni.p_linesSrcrefSymbol ));
							if (add != null) {
								srcref = Srcref.add(srcref, add);
							}
							item.setExprSrcref(srcref);
						}
					}
				}
				
				list.add(item);
				if (++i > n) {
					n++;
					break;
				}
				if (call != null
						&& (call.startsWith(".doTrace") || call.startsWith("rj:::.breakpoint(")) ) {
					n = i;
					break;
				}
				cdr = this.rEngine.rniCDR(cdr);
			}
		}
		{	long cdr = this.rEngine.rniEval(this.sysFramesCallP, 0);
			if (cdr != 0 && this.rEngine.rniExpType(cdr) == REXP.LISTSXP) {
				int i = 1;
				while (cdr != 0 && i < n) {
					final long car = this.rEngine.rniCAR(cdr);
					if (car != 0 && this.rEngine.rniExpType(car) == REXP.ENVSXP) {
						if (i < n) {
							list.get(i).setHandle(car);
						}
					}
					i++;
					cdr = this.rEngine.rniCDR(cdr);
				}
			}
		}
		
		return new CallStack(list, true);
	}
	
	/**
	 * Loads the details of a frame
	 * 
	 * @param request the request specifying the frame
	 * @return the frame detail
	 * @throws RjsException
	 */
	public FrameContext loadFrameDetail(final FrameContextDetailRequest request)
			throws RjsException {
		final int n = getNFrame();
		if (n < 0 || request.getPosition() < 0 || request.getPosition() > n) {
			return null;
		}
		
		String call = null;
		if (request.getPosition() != 0) {
			final long currentCallP = this.rni.evalExpr(this.rEngine.rniCons(
					this.sysCallFunP, this.rEngine.rniCons(
							this.rEngine.rniPutIntArray(new int[] { request.getPosition() }), this.rni.p_NULL,
							0, false),
					0, true),
					0, CODE_DBG_CONTEXT );
			if (currentCallP != 0) {
				call = this.rni.getSourceLine(currentCallP);
			}
		}
		
		int[] firstSrcref = null;
		int[] lastSrcref = null;
		int[] exprSrcref = null;
		long contextFunP = 0;
		long contextSrcrefP = 0;
		long contextSrcfileP = 0;
		{	final long nextCallP = this.rEngine.rniEval((request.getPosition() < n) ?
					this.rEngine.rniCons(
							this.sysCallFunP, this.rEngine.rniCons(
									this.rEngine.rniPutIntArray(new int[] { request.getPosition()+1 }), this.rni.p_NULL,
									0, false ),
							0, true ) : this.sysCall0CallP,
					0 );
			if (nextCallP != 0) {
				contextSrcrefP = this.rEngine.rniGetAttrBySym(nextCallP, this.rni.p_srcrefSymbol);
			}
		}
		if (contextSrcrefP != 0) {
			exprSrcref = getSrcrefObject(contextSrcrefP);
			contextSrcfileP = this.rEngine.rniGetAttrBySym(contextSrcrefP, this.rni.p_srcfileSymbol);
		}
		
		if (request.getPosition() != 0) {
			{	// function
				final long currentFunP = this.rEngine.rniEval(
						this.rEngine.rniCons(this.sysFunctionFunP, this.rEngine.rniCons(
								this.rEngine.rniPutIntArray(new int[] { request.getPosition() }), this.rni.p_NULL,
								0, false ),
						0, true ),
						0 );
				if (currentFunP != 0) {
					contextFunP = getOrgFun(currentFunP);
		//			{	// package info
		//				final long packageP = this.rEngine.rniGetAttr(infoFunP, "package");
		//				if (packageP != 0) {
		//					String[] name = this.rEngine.rniGetStringArray(packageP);
		//					if (name != null && name.length == 0) {
		//						info[DataCmdItem.CONTEXT_DETAIL_FUN_PACKAGE] = new JRIVectorImpl<RStore>(new JRICharacterDataImpl(name), null, null);
		//					}
		//				}
		//			}
				}
			}
			if (contextFunP != 0) {
				final long bodyP = this.rEngine.rniGetCloBodyExpr(contextFunP);
				if (bodyP != 0) {
					if (contextSrcfileP == 0) {
						final long srcrefP = this.rEngine.rniGetAttr(bodyP, "wholeSrcref");
						if (srcrefP != 0) {
							final long srcfileP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_srcfileSymbol);
							if (srcfileP != 0) {
								contextSrcrefP = srcrefP;
								contextSrcfileP = srcfileP;
							}
						}
					}
					{	long srcrefP = this.rEngine.rniGetAttrBySym(bodyP, this.rni.p_srcrefSymbol);
						if (srcrefP != 0) {
							final long[] srcrefItemsP = this.rEngine.rniGetVector(srcrefP);
							if (srcrefItemsP != null && srcrefItemsP.length > 0) {
								srcrefP = srcrefItemsP[0];
								final long srcfileP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_srcfileSymbol);
								if (srcfileP != 0) {
									if (contextSrcrefP == 0) {
										contextSrcrefP = srcrefP;
										contextSrcfileP = srcfileP;
									}
									if (srcfileP == contextSrcfileP) {
										firstSrcref = getSrcrefObject(srcrefP);
										lastSrcref = getSrcrefObject(
												srcrefItemsP[srcrefItemsP.length-1] );
									}
								}
							}
						}
					}
					if (contextSrcrefP == 0) {
						contextSrcfileP = this.rEngine.rniGetAttrBySym(bodyP, this.rni.p_srcfileSymbol);
						if (contextSrcfileP == 0) {
							contextSrcfileP = this.rEngine.rniGetAttrBySym(contextFunP, this.rni.p_srcfileSymbol);
						}
					}
				}
			}
		}
		
		// file info
		String fileName = null;
		long fileTimestamp = 0;
		String fileEncoding = null;
		String filePath = null;
		if (contextSrcfileP != 0) {
			fileName = getFileName(contextSrcfileP);
			if (fileName != null) {
				filePath = getFilePath(contextSrcfileP, 0); // contextSrcrefP ?
				fileEncoding = getFileEncoding(contextSrcfileP);
				fileTimestamp = getFileTimestamp(contextSrcfileP);
			}
		}
		
		// source code
		int sourceType = 0;
		String sourceCode = null;
		int[] sourceSrcref = null;
		{	if (contextSrcfileP != 0) {
				final long sourceP = this.rEngine.rniGetVarBySym(
						contextSrcfileP, this.rni.p_linesSymbol );
				if (sourceP != 0) {
					final String[] sourceLines = this.rEngine.rniGetStringArray(sourceP);
					if (sourceLines != null && sourceLines.length > 0) {
						sourceType = FrameContext.SOURCETYPE_1_LINES;
						sourceCode = this.utils.concat(sourceLines, '\n');
						sourceSrcref = getSrcrefObject(this.rEngine.rniGetVarBySym(
								contextSrcfileP, this.rni.p_linesSrcrefSymbol ));
					}
				}
			}
			if (fileName != null && fileTimestamp != 0) {
				final File file = new File(fileName);
				{	final long modified = file.lastModified()/1000;
					if (Math.abs(modified - fileTimestamp) == 3600) {
						fileTimestamp = modified;
					}
				}
				if (sourceType == 0 && contextSrcfileP != 0
						&& file.exists() && file.lastModified()/1000 == fileTimestamp) {
					String encoding = fileEncoding;
					if (encoding == null) {
						encoding = "UTF-8";
					}
					Charset charset;
					if (encoding.equals("native.enc")) {
						charset = Charset.defaultCharset();
					}
					else {
						try {
							charset = Charset.forName(encoding);
						}
						catch (final Exception e) {
							charset = Charset.forName("UTF-8");
						}
					}
					final String content = this.utils.readFile(file, charset);
					if (content != null && file.lastModified()/1000 == fileTimestamp) {
						sourceType = FrameContext.SOURCETYPE_1_FILE;
						sourceCode = content;
					}
				}
			}
			if (sourceType == 0 && contextFunP != 0) {
				final long sourceP = this.rEngine.rniGetAttr(contextFunP, "source");
				if (sourceP != 0) {
					final String[] sourceLines = this.rEngine.rniGetStringArray(sourceP);
					if (sourceLines != null && sourceLines.length > 0) {
						sourceType = FrameContext.SOURCETYPE_2_LINES;
						sourceCode = this.utils.concat(sourceLines, '\n');
					}
				}
			}
			if (sourceType == 0 && contextFunP != 0) {
				final String[] sourceLines = this.rni.getSourceLines(contextFunP);
				if (sourceLines != null && sourceLines.length > 0) {
					sourceType = FrameContext.SOURCETYPE_3_DEPARSE;
					sourceCode = this.utils.concat(sourceLines, '\n');
				}
			}
			if (sourceType != 0) {
				if (sourceType >= 3) {
					int idx;
					sourceCode = ((call != null && (idx = call.indexOf('(')) > 0 ) ?
										call.substring(0, idx) : "f") + " <- " + sourceCode;
				}
			}
		}
		
		return new FrameContext(request.getPosition(), call,
				fileName, fileTimestamp, fileEncoding, filePath,
				sourceType, sourceCode, sourceSrcref, firstSrcref, lastSrcref, exprSrcref );
	}
	
	public SetDebugReport setDebug(final SetDebugRequest request) throws RjsException {
		final boolean changed;
		switch (request.getType()) {
		case SetDebugRequest.FRAME:
			changed = doSetDebug(request.getHandle(), request.getDebug(), request.isTemp());
			break;
		case SetDebugRequest.FUNCTION:
			changed = doSetDebug(request.getName(), request.getDebug(), request.isTemp());
			break;
		default:
			throw new RjsException(0, "Unsupported debug request type 0x" + Integer.toHexString(request.getType()));
		}
		return new SetDebugReport(changed);
	}
	
	public void requestSuspend() {
		if (this.disabled) {
			this.deferredSuspend = true;
		}
		else {
			doSuspend();
		}
	}
	
	private void doSuspend() {
		this.deferredSuspend = false;
		final int savedProtected = this.rni.saveProtected();
		try {
			final long[] frames = searchSuspendHandles();
			for (int i = 0; i < frames.length; i++) {
				if (frames[i] == 0) {
					break;
				}
				doSetDebug(frames[i], 1, true);
			}
		}
		catch (final Exception e) {
			JRIServerErrors.LOGGER.log(Level.SEVERE, "Setting suspend request failed.", e);
		}
		finally {
			this.rni.looseProtected(savedProtected);
		}
	}
	
	private boolean doSetDebug(long frameHandle, final int v, final boolean temp) {
		if (frameHandle == this.rni.p_GlobalEnv) {
			frameHandle = 0;
		}
		final boolean changed = this.rEngine.rniSetDebug(frameHandle, v);
		final Long handle = Long.valueOf(frameHandle);
		if (v == 0 || !temp) {
			this.contextTmpDebugFrames.remove(handle);
		}
		else if (changed && !this.contextTmpDebugFrames.contains(handle)) { // && enable && temp
			this.contextTmpDebugFrames.add(handle);
		}
		return changed;
	}
	
	private long[] searchSuspendHandles() throws RjsException {
		final long[] handles = new long[8];
		final CallStack stack = loadCallStack();
		final int n = stack.getFrames().size() - 1;
		if (n < 0) {
			return handles;
		}
		if (n == 0) {
			handles[0] = this.rni.p_GlobalEnv;
			return handles;
		}
		
		final int last = Math.max(n-3, 0);
		if (!this.stepFilterEnabled) {
			for (int i = n-1; i >= last; i--) {
				final CallStack.Frame frame0 = stack.getFrames().get(i);
				add(handles, frame0);
			}
		}
		else {
			for (int i = n-1; i >= last; i--) {
				final CallStack.Frame frame0 = stack.getFrames().get(i);
				switch (frame0.getFlags() & 0xff) {
				case (CallStack.FLAG_COMMAND | 0):
				case (CallStack.FLAG_SOURCE | 0):
				case (CallStack.FLAG_COMMAND | 1):
				case (CallStack.FLAG_SOURCE | 1):
					i -= 1;
					continue;
				case (CallStack.FLAG_COMMAND | 2):
				case (CallStack.FLAG_SOURCE | 2):
				case (CallStack.FLAG_COMMAND | 3):
				case (CallStack.FLAG_SOURCE | 3):
					i -= 1;
					break;
				default:
					if (i > 0
							&& frame0.getCall() != null && frame0.getCall().startsWith("eval(")) {
						final CallStack.Frame frame1 = stack.getFrames().get(i-1);
						if (frame1.getCall() != null && frame1.getCall().startsWith("eval(")) {
							i -= 1;
							break;
						}
					}
				}
				add(handles, frame0);
			}
		}
		return handles;
	}
	
	private void add(final long[] frameIds, final CallStack.Frame frame) {
		long handle = frame.getHandle();
		if (handle == 0) {
			if (frame.getPosition() == 0) {
				handle = this.rni.p_GlobalEnv;
			}
			else {
				return;
			}
		}
		for (int i = 0; i < frameIds.length; i++) {
			if (frameIds[i] == handle) {
				return;
			}
			if (frameIds[i] == 0) {
				frameIds[i] = handle;
				return;
			}
		}
	}
	
	private boolean doSetDebug(final String fName, final int v, final boolean temp) throws RjsException {
		if (fName == null) {
			return false;
		}
		final long frameId = this.rni.getTopFrame();
		long funP;
		{	long exprP = this.rEngine.rniParse(fName, 1);
			final long[] list;
			if (exprP != 0 && (list = this.rEngine.rniGetVector(exprP)) != null && list.length == 1) {
				exprP = this.rni.protect(list[0]);
			}
			else {
				throw new RjsException(CODE_DBG_DEBUG | 0x2, "Invalid function name.");
			}
			if (this.rEngine.rniGetLength(exprP) == 1 && this.rEngine.rniExpType(exprP) == REXP.SYMSXP) {
				funP = this.rEngine.rniFindFunBySym(exprP, frameId);
			}
			else {
				funP = this.rni.evalExpr(exprP, frameId, CODE_DBG_DEBUG | 0x4);
			}
		}
		final int type;
		if (funP != 0 && ((type = this.rEngine.rniExpType(funP)) == REXP.CLOSXP
				|| type == REXP.SPECIALSXP || type == REXP.BUILTINSXP )) {
			final boolean changed = this.rEngine.rniSetDebug(funP, v);
			final Long handle = Long.valueOf(funP);
			if (v == 0 || !temp) {
				if (this.contextTmpDebugFuns.remove(handle)) {
					this.rEngine.rniRelease(funP);
				}
			}
			else if (changed && !this.contextTmpDebugFuns.contains(handle)) {
				this.rEngine.rniPreserve(funP);
				this.contextTmpDebugFuns.add(handle);
			}
			return changed;
		}
		else {
			throw new RjsException(CODE_DBG_DEBUG | 0x6, "No function found.");
		}
	}
	
	public void clearContext() {
		if (this.checkToRelease) {
			synchronized (this.toRelease) {
				this.checkToRelease = false;
				for (int i = 0; i < this.toRelease.size(); i++) {
					this.rEngine.rniRelease(this.toRelease.get(i).longValue());
				}
				this.toRelease.clear();
			}
		}
		
		this.deferredSuspend = false;
		
		if (!this.contextTmpDebugFuns.isEmpty()) {
			try {
				for (final Long handle : this.contextTmpDebugFuns) {
					this.rEngine.rniSetDebug(handle.longValue(), 0);
					this.rEngine.rniRelease(handle.longValue());
				}
			}
			finally {
				this.contextTmpDebugFuns.clear();
			}
		}
		if (!this.contextTmpDebugFrames.isEmpty()) {
			try {
				long cdr = this.rni.evalExpr(this.sysFramesCallP, 0, CODE_DBG_DEBUG | 0xe);
				final List<Long> stack = new ArrayList<Long>(this.contextTmpDebugFrames.size());
				boolean topInStack = false;
				if (cdr != 0 && this.rEngine.rniExpType(cdr) == REXP.LISTSXP) {
					while (cdr != 0) {
						final long car = this.rEngine.rniCAR(cdr);
						if (car != 0 && this.rEngine.rniExpType(car) == REXP.ENVSXP) {
							final Long handle = Long.valueOf(car);
							if (!stack.contains(handle)
									&& this.rEngine.rniGetDebug(car) != 0) {
								if (this.contextTmpDebugFrames.contains(handle)) {
									stack.add(handle);
									topInStack = true;
								}
								else {
									topInStack = false;
								}
							}
						}
						cdr = this.rEngine.rniCDR(cdr);
					}
				}
				
				if (topInStack) {
					stack.remove(stack.size()-1);
				}
				for (int i = stack.size()-1; i >= 0; i--) {
					this.rEngine.rniSetDebug(stack.get(i).longValue(), 0);
				}
			}
			catch (final Throwable e) {
				LOGGER.log(Level.SEVERE, "An error occured when checking suspended frames", e);
			}
			finally {
				this.contextTmpDebugFrames.clear();
			}
		}
	}
	
	/**
	 * Installs the specified tracepoints
	 * 
	 * @param request with elements and their tracepoints
	 * @return a report
	 * @throws RjsException
	 */
	public ElementTracepointInstallationReport installTracepoints(final ElementTracepointInstallationRequest request)
			throws RjsException {
		final List<? extends ElementTracepointPositions> elementList = request.getRequests();
		final int[] resultCodes = new int[elementList.size()];
		Arrays.fill(resultCodes, ElementTracepointInstallationReport.NOTFOUND);
		
		final List<Long> envTodo = new ArrayList<Long>();
		final List<Long> envDone = new ArrayList<Long>();
		addAll(envTodo, this.rni.p_GlobalEnv);
		{	long cdr = this.rni.evalExpr(this.sysFramesCallP, 0, CODE_DBG_TRACE);
			if (cdr != 0 && this.rEngine.rniExpType(cdr) == REXP.LISTSXP) {
				while (cdr != 0) {
					final long car = this.rEngine.rniCAR(cdr);
					if (car != 0 && this.rEngine.rniExpType(car) == REXP.ENVSXP) {
						addAll(envTodo, car);
					}
					cdr = this.rEngine.rniCDR(cdr);
				}
			}
		}
		
		while (!envTodo.isEmpty()) {
			final Long env = envTodo.remove(0);
			envDone.add(env);
			final int savedProtected = this.rni.saveProtected();
			final String[] names = this.rEngine.rniGetStringArray(this.rEngine.rniListEnv(env.longValue(), true));
			for (int namesIdx = 0; namesIdx < names.length; namesIdx++) {
				if (this.rni.p_BaseEnv == env.longValue()
						&& names[namesIdx].equals(".Last.value")) {
					continue;
				}
				final long funP;
				final String nameString = names[namesIdx];
				{	long p = this.rEngine.rniGetVar(env.longValue(), nameString);
					if (p == 0) {
						continue;
					}
					int type = this.rEngine.rniExpType(p);
					if (type == REXP.PROMSXP) {
						p = this.rEngine.rniGetPromise(p, 1);
						type = this.rEngine.rniExpType(p);
					}
					if (type != REXP.CLOSXP) {
						continue;
					}
					funP = p;
				}
				final long orgFunP = getOrgFun(funP);
				if (orgFunP == 0) {
					continue;
				}
				
				FunInfo commonInfo = new FunInfo(orgFunP, funP, null);
				if (!preCheck(commonInfo)) {
					commonInfo = null;
				}
				
				final long nameStringP = this.rni.protect(this.rEngine.rniPutString(nameString));
				
				// method
				List<FunInfo> methodInfos = null;
				try {
					final long nameEnvP = checkGeneric(orgFunP, nameStringP, env.longValue());
					
					if (nameEnvP != 0) {
						final String[] signatures = this.rEngine.rniGetStringArray(
								this.rEngine.rniListEnv(nameEnvP, true) );
						methodInfos = new ArrayList<FunInfo>(signatures.length);
						for (int signarturesIdx = 0; signarturesIdx < signatures.length; signarturesIdx++) {
							try {
								final long methodP;
								{	long p = this.rEngine.rniGetVar(nameEnvP, signatures[signarturesIdx]);
									int type = this.rEngine.rniExpType(p);
									if (type == REXP.PROMSXP) {
										p = this.rEngine.rniGetPromise(p, 1);
										type = this.rEngine.rniExpType(p);
									}
									if (type != REXP.CLOSXP) {
										continue;
									}
									methodP = p;
								}
								final long mainOrgMethodP = getOrgFun(methodP);
								if (mainOrgMethodP == 0) {
									continue;
								}
								final FunInfo methodInfo = new FunInfo(mainOrgMethodP,
										methodP, signatures[signarturesIdx] );
								if (preCheck(methodInfo)) {
									methodInfos.add(methodInfo);
								}
								
								long localMethodP = this.rEngine.rniGetCloBodyExpr(mainOrgMethodP);
								if (localMethodP == 0 || this.rEngine.rniExpType(localMethodP) != REXP.LANGSXP
										|| this.rEngine.rniGetLength(localMethodP) < 3
										|| this.rEngine.rniCAR(localMethodP) != this.rni.p_BlockSymbol) {
									continue;
								}
								localMethodP = this.rEngine.rniCAR(this.rEngine.rniCDR(localMethodP));
								if (localMethodP == 0 || this.rEngine.rniExpType(localMethodP) != REXP.LANGSXP
										|| this.rEngine.rniGetLength(localMethodP) < 3
										|| this.rEngine.rniCAR(localMethodP) != this.rni.p_AssignSymbol) {
									continue;
								}
								localMethodP = this.rEngine.rniCAR(this.rEngine.rniCDR(this.rEngine.rniCDR(localMethodP)));
								if (localMethodP == 0 || this.rEngine.rniExpType(localMethodP) != REXP.CLOSXP) {
									continue;
								}
								{	final FunInfo subInfo = new FunInfo(localMethodP, LOCAL_METHOD_INDEX,
											methodInfo );
									if (preCheck(subInfo)) {
										methodInfos.add(subInfo);
									}
								}
							}
							catch (final Exception e) {
								final LogRecord record = new LogRecord(Level.SEVERE,
										"Method check failed for function ''{0}({1})'' in ''0x{2}''.");
								record.setParameters(new Object[] { nameString, signatures[signarturesIdx], Long.toHexString(env.longValue()) });
								record.setThrown(e);
								JRIServerErrors.LOGGER.log(record);
							}
						}
					}
				}
				catch (final Exception e) {
					final LogRecord record = new LogRecord(Level.SEVERE,
							"Generic check failed for function ''{0}'' in ''0x{1}''.");
					record.setParameters(new Object[] { nameString, Long.toHexString(env.longValue()) });
					record.setThrown(e);
					JRIServerErrors.LOGGER.log(record);
				}
				
				for (int elementIdx = 0; elementIdx < elementList.size(); elementIdx++) {
					if (commonInfo != null && commonInfo.done[0] < FunInfo.DONE_SET) {
						final int funSet = trySetTracepoints(elementList.get(elementIdx),
								commonInfo, nameString, nameStringP, env.longValue());
						if (funSet > resultCodes[elementIdx]) {
							resultCodes[elementIdx] = funSet;
							continue;
						}
					}
					if (methodInfos != null) {
						for (int methodIdx = 0; methodIdx < methodInfos.size(); methodIdx++) {
							final FunInfo methodInfo = methodInfos.get(methodIdx);
							if (methodInfo.done[0] < FunInfo.DONE_SET) {
								final int methodSet = trySetTracepoints(elementList.get(elementIdx),
										methodInfo, nameString, nameStringP, env.longValue());
								if (methodSet > resultCodes[elementIdx]) {
									resultCodes[elementIdx] = methodSet;
								}
							}
						}
					}
				}
			}
			this.rni.looseProtected(savedProtected);
		}
		
		if (LOGGER.isLoggable(Level.FINER)) {
			final StringBuilder sb = new StringBuilder("Dbg: installTracepoints");
			for (int i = 0; i < resultCodes.length; i++) {
				sb.append('\n').append(i).append(".");
				sb.append(" --> ").append(resultCodes[i]);
				sb.append('\n').append(elementList.get(i));
			}
			LOGGER.log(Level.FINER, sb.toString());
		}
		
		return new ElementTracepointInstallationReport(resultCodes);
	}
	
	private void addAll(final List<Long> envs, long envP) {
		while (envP != 0 && envP != this.rni.p_EmptyEnv) {
			final Long handler = Long.valueOf(envP);
			if (envs.contains(handler)) {
				return;
			}
			envs.add(handler);
			envP = this.rEngine.rniParentEnv(envP);
		}
	}
	
	private long checkGeneric(final long funP, final long nameStringP, final long envP) throws Exception {
		long nameEnvP = this.rEngine.rniGetCloEnv(funP);
		if (nameEnvP == 0 || this.rEngine.rniExpType(nameEnvP) != REXP.ENVSXP) {
			return 0;
		}
		nameEnvP = this.rEngine.rniGetVarBySym(nameEnvP, this.rni.p_DotAllMTable);
		if (nameEnvP == 0 || this.rEngine.rniExpType(nameEnvP) != REXP.ENVSXP) {
			return 0;
		}
		
		final long p = this.rEngine.rniEval(this.rEngine.rniCons(
				this.rni.p_isGenericSymbol, this.rEngine.rniCons(
						nameStringP, this.rEngine.rniCons(
								envP, this.rEngine.rniCons(
										funP, this.rni.p_NULL,
										this.rni.p_fdefSymbol, false ),
								this.rni.p_whereSymbol, false ),
						0, false ),
				0, true ),
				0 );
		return (p != 0 && this.rEngine.rniIsTrue(p)) ? nameEnvP : 0;
	}
	
	private boolean preCheck(final FunInfo funInfo) {
		funInfo.orgBodyP = this.rEngine.rniGetCloBodyExpr(funInfo.orgFunP);
		if (funInfo.orgBodyP == 0) {
			return false;
		}
		funInfo.srcfileP = this.rEngine.rniGetAttrBySym(funInfo.orgBodyP, this.rni.p_srcfileSymbol);
		if (funInfo.srcfileP == 0 || this.rEngine.rniExpType(funInfo.srcfileP) != REXP.ENVSXP) {
			return false;
		}
		funInfo.file = getFilePath(funInfo.srcfileP, 0);
		if (funInfo.file != null) {
			funInfo.fileType = FunInfo.FILE_PATH;
			return true;
		}
		funInfo.file = getFileName(funInfo.srcfileP);
		if (funInfo.file != null) {
			funInfo.fileType = FunInfo.FILE_NAME;
			return true;
		}
		funInfo.fileType = FunInfo.INVALID;
		return false;
	}
	
	private int trySetTracepoints(final ElementTracepointPositions elementTracepointPositions,
			final FunInfo funInfo,
			final String nameString, final long nameStringP, final long envP) {
		int[] baseSrcref = null;
		
		// file
		final SrcfileData srcfile = elementTracepointPositions.getSrcfile();
		boolean ok = false;
		if (srcfile.getPath() == null) {
			return ElementTracepointInstallationReport.NOTFOUND;
		}
		if (funInfo.fileType == FunInfo.FILE_PATH) {
			if (funInfo.file.equals(srcfile.getPath())) {
				ok = true;
			}
			else {
				return ElementTracepointInstallationReport.NOTFOUND;
			}
		}
		if (!ok && funInfo.fileType == FunInfo.FILE_NAME) {
			if (funInfo.file.equals(srcfile.getName())) {
				ok = true;
			}
			else {
				return ElementTracepointInstallationReport.NOTFOUND;
			}
		}
		if (!ok) {
			return ElementTracepointInstallationReport.NOTFOUND;
		}
		
		// element
		ok = false;
		if (elementTracepointPositions.getElementId() != null) {
			final long p = this.rEngine.rniGetAttrBySym(funInfo.orgBodyP, this.rni.p_appElementIdSymbol);
			if (p != 0) {
				if (elementTracepointPositions.getElementId().equals(this.rEngine.rniGetString(p))) {
					ok = true;
				}
				else {
					return ElementTracepointInstallationReport.NOTFOUND;
				}
			}
		}
		{	long p = this.rEngine.rniGetAttrBySym(funInfo.orgBodyP, this.rni.p_srcrefSymbol);
			if (p != 0) {
				p = this.rEngine.rniGetVectorElt(p, 0);
				if (p != 0) {
					baseSrcref = this.rEngine.rniGetIntArray(p);
					if (baseSrcref != null && baseSrcref.length < 6) {
						baseSrcref = null;
					}
				}
			}
		}
		if (!ok && baseSrcref != null && elementTracepointPositions.getElementSrcref() != null) {
			long funTimestamp;
			if (srcfile.getTimestamp() != 0
					&& baseSrcref[0] == elementTracepointPositions.getElementSrcref()[0]
					&& baseSrcref[4] == elementTracepointPositions.getElementSrcref()[4]
					&& ((funTimestamp = getFileTimestamp(funInfo.srcfileP)) == 0 // rpkg
							|| funTimestamp == srcfile.getTimestamp()
							|| Math.abs(funTimestamp - srcfile.getTimestamp()) == 3600 )) {
				ok = true;
			}
		}
		final int l;
		if (ok) {
			funInfo.done[0] = FunInfo.DONE_SET;
			l = elementTracepointPositions.getPositions().size();
		}
		else {
			l = 0;
			long p;
			if (funInfo.done[0] < FunInfo.DONE_CLEAR
					&& funInfo.currentMainFunP != funInfo.orgMainFunP
					&& (p = this.rEngine.rniGetAttrBySym(funInfo.currentMainFunP, this.rni.p_dbgElementIdSymbol)) != 0
					&& elementTracepointPositions.getElementId().equals(this.rEngine.rniGetString(p)) ) {
				ok = true;
				funInfo.done[0] = FunInfo.DONE_CLEAR;
			}
		}
		if (!ok) {
			return ElementTracepointInstallationReport.NOTFOUND;
		}
		
		// ok
		int result = ElementTracepointInstallationReport.FOUND_UNCHANGED;
		final int savedProtected = this.rni.saveProtected();
		try {
			long traceArgsP = this.rni.p_NULL;
			traceArgsP = this.rEngine.rniCons(
					envP, traceArgsP,
					this.rni.p_whereSymbol, false );
			if (funInfo.rawMethodSignature != null) {
				final String[] signature = SIGNATURE_PATTERN.split(funInfo.rawMethodSignature);
				traceArgsP = this.rEngine.rniCons(
						this.rEngine.rniPutStringArray(signature), traceArgsP,
						this.rni.p_signatureSymbol, false );
			}
			traceArgsP = this.rEngine.rniCons(
					nameStringP, traceArgsP,
					this.rni.p_whatSymbol, false );
			
			long editFunP = 0;
			if (l > 0) {
				// prepare
				final long newMainFunP = this.rni.protect(this.rEngine.rniDuplicate(funInfo.orgMainFunP));
				// if (subIndex != null) assert(this.rEngine.rniGetCloBodyExpr(newMainFunP) == bodyP);
				long newMainBodyP = this.rni.protect(this.rEngine.rniDuplicate(
						this.rEngine.rniGetCloBodyExpr(newMainFunP) ));
				long newBodyP = newMainBodyP;
				long subListP = 0;
				long subCloP = 0;
				if (funInfo.subIndex != null && funInfo.subIndex.length > 0) {
					for (int i = 0; i < funInfo.subIndex.length; i++) {
						if (this.rEngine.rniExpType(newBodyP) != REXP.LANGSXP
								|| this.rEngine.rniGetLength(newBodyP) < funInfo.subIndex[i]) {
							throw new IllegalStateException();
						}
						for (int idx = 1; idx < funInfo.subIndex[i]; idx++) {
							newBodyP = this.rEngine.rniCDR(newBodyP);
						}
						subListP = newBodyP;
						newBodyP = this.rEngine.rniCAR(newBodyP);
					}
					if (this.rEngine.rniExpType(newBodyP) == REXP.CLOSXP) {
						subListP = 0;
						subCloP = newBodyP;
						// assert(this.rEngine.rniGetCloBodyExpr(subCloP) == bodyP);
						newBodyP = this.rni.protect(this.rEngine.rniDuplicate(
								this.rEngine.rniGetCloBodyExpr(newBodyP) ));
					}
				}
				if (this.rEngine.rniExpType(newBodyP) != REXP.LANGSXP) {
					throw new IllegalStateException("unsupported SXP-type: " + this.rEngine.rniExpType(newBodyP));
				}
				
				final long filePathP = this.rni.protect(this.rEngine.rniPutString(srcfile.getPath()));
				final long elementIdP = this.rni.protect(this.rEngine.rniPutString(elementTracepointPositions.getElementId()));
				
				final int i = 0;
				int j = 0;
				while (j < l) {
					if (elementTracepointPositions.getPositions().get(j).getIndex().length == 0) {
						j++;
					}
					else {
						break;
					}
				}
				
				if (j < l) {
					addTrace(newBodyP, elementTracepointPositions.getPositions().subList(j, l), 0,
							funInfo, filePathP, elementIdP, baseSrcref, null );
				}
				if (i < j) {
					newBodyP = createTraceLang(newBodyP, elementTracepointPositions.getPositions().subList(i, j),
							funInfo, filePathP, elementIdP, baseSrcref);
				}
				
				if (subCloP != 0) {
					this.rEngine.rniSetAttrBySym(subCloP, this.rni.p_originalSymbol,
							this.rEngine.rniDuplicate(subCloP));
					this.rEngine.rniSetCloBody(subCloP, newBodyP);
				}
				else if (subListP != 0) {
					this.rEngine.rniSetCAR(subListP, newBodyP);
				}
				else {
					newMainBodyP = newBodyP;
				}
				
				this.rEngine.rniSetAttrBySym(newBodyP, this.rni.p_dbgElementIdSymbol, elementIdP);
				if (newBodyP != newMainBodyP) {
					this.rEngine.rniSetAttrBySym(newMainBodyP, this.rni.p_dbgElementIdSymbol, elementIdP);
				}
				this.rEngine.rniSetCloBody(newMainFunP, newMainBodyP);
				editFunP = createEditFun(newMainFunP);
			}
			
			if (funInfo.currentMainFunP != funInfo.orgMainFunP) {
				// unset
				final long untraceP = this.rEngine.rniCons(
						this.rni.p_untraceSymbol, traceArgsP,
						0, true );
				this.rni.evalExpr(untraceP, 0, CODE_DBG_TRACE );
				funInfo.currentMainFunP = funInfo.orgMainFunP;
			}
			result = ElementTracepointInstallationReport.FOUND_UNSET;
			if (l > 0) { // set
				final long traceP = this.rEngine.rniCons(
						this.rni.p_traceSymbol, this.rEngine.rniCons(
								editFunP, traceArgsP,
								this.rni.p_editSymbol, false ),
						0, true );
				this.rni.evalExpr(traceP, 0, CODE_DBG_TRACE );
				result = ElementTracepointInstallationReport.FOUND_SET;
			}
		}
		catch (final Exception e) {
			final LogRecord record = new LogRecord(Level.SEVERE, (funInfo.rawMethodSignature != null) ?
					"Updating tracepoints failed for method ''{0}({1})'' in ''0x{2}''." :
					"Updating tracepoints failed for function ''{0}'' in ''0x{2}''.");
			record.setParameters(new Object[] { nameString, funInfo.rawMethodSignature, Long.toHexString(envP) });
			record.setThrown(e);
			JRIServerErrors.LOGGER.log(record);
		}
		finally {
			this.rni.looseProtected(savedProtected);
		}
		return result;
	}
	
	private long createEditFun(final long newFDefP) {
		final long fBodyP = this.rEngine.rniCons(
				this.rni.p_BlockSymbol, this.rEngine.rniCons(
						newFDefP, this.rni.p_NULL,
						0, false ),
				0, true );
		return this.rni.protect(this.rEngine.rniEval(this.rni.protect(this.rEngine.rniCons(
				this.rni.p_functionSymbol, this.rEngine.rniCons(
						this.editFArgsP, this.rEngine.rniCons(
								fBodyP, this.rni.p_NULL,
								0, false ),
						0, false ),
				0, true )),
				this.rni.p_BaseEnv ));
	}
	
	private void addTrace(final long newP, final List<? extends TracepointPosition> list, final int depth,
			final FunInfo funInfo, final long filePathP, final long elementIdP,
			final int[] baseSrcref, final int[] lastSrcref)
			throws UnexpectedRDataException {
		final int length = this.rEngine.rniGetLength(newP);
		final long newSrcrefP = this.rEngine.rniGetAttrBySym(newP, this.rni.p_srcrefSymbol);
		int currentIdx = 1;
		long currentP = newP;
		for (int i = 0; i < list.size(); ) {
			final int[] breakpointIndex = list.get(i).getIndex();
			final int breakpointIdx = breakpointIndex[depth];
			if (currentIdx > breakpointIdx || breakpointIdx > length) {
				throw new IllegalStateException();
			}
			while (currentIdx < breakpointIdx) {
				currentP = this.rEngine.rniCDR(currentP);
				currentIdx++;
			}
			// collect nested breakpoints
			int j = (depth+1 == breakpointIndex.length) ? i+1 : i; // exclusive
			int k = i+1; // exclusive
			while (k < list.size()) {
				final int[] nextIndex = list.get(k).getIndex();
				if (breakpointIdx == nextIndex[depth]) {
					if (j == k && depth+1 == nextIndex.length) {
						j++;
					}
					k++;
					continue;
				}
				else {
					break;
				}
			}
			
			long currentValueP = this.rEngine.rniCAR(currentP);
			final long currentSrcrefP = (newSrcrefP != 0) ?
					this.rEngine.rniGetVectorElt(newSrcrefP, breakpointIdx-1) : 0;
			int[] currentSrcref = null;
			if (i < j) {
				currentSrcref = bestSrcref(baseSrcref, list.get(i).getSrcref(), currentSrcrefP,
						lastSrcref );
			}
			else {
				if (currentSrcrefP != 0) {
					currentSrcref = this.rEngine.rniGetIntArray(currentSrcrefP);
				}
				if (currentSrcref == null) {
					currentSrcref = lastSrcref;
				}
			}
			if (j < k) {
				if (this.rEngine.rniExpType(currentValueP) != REXP.LANGSXP) {
					throw new IllegalStateException("unsupported SXP-type: " + this.rEngine.rniExpType(currentValueP));
				}
				long orgP;
				if (this.rEngine.rniGetLength(currentValueP) == 3
						&& this.rEngine.rniCAR(currentValueP) == this.rni.p_functionSymbol) {
					orgP = this.rEngine.rniDuplicate(currentValueP);
				}
				else {
					orgP = 0;
				}
				addTrace(currentValueP, list.subList(j, k), depth+1,
						funInfo, filePathP, elementIdP, baseSrcref, currentSrcref );
				if (orgP != 0) {
					orgP = this.rEngine.rniEval(orgP, 0);
					if (orgP != 0) {
						currentValueP = this.rEngine.rniEval(currentValueP, 0);
						if (currentValueP == 0) {
							throw new UnexpectedRDataException("closure");
						}
						this.rEngine.rniSetAttrBySym(currentValueP, this.rni.p_originalSymbol, orgP);
						this.rEngine.rniSetCAR(currentP, currentValueP);
					}
				}
			}
			if (i < j) {
				this.rEngine.rniSetCAR(currentP, createTraceLang(currentValueP, list.subList(i, j),
						funInfo, filePathP, elementIdP, currentSrcref ));
			}
			i = k;
		}
	}
	
	private int[] bestSrcref(final int[] baseSrcref, final int[] positionSrcref, 
			final long currentSrcrefP, final int[] lastSrcref)
			throws UnexpectedRDataException {
		int[] currentSrcref = null;
		if (currentSrcrefP != 0) {
			currentSrcref = this.rEngine.rniGetIntArray(currentSrcrefP);
			this.rEngine.rniSetAttrBySym(currentSrcrefP,
					this.rni.p_whatSymbol, this.stepNextValueP );
		}
		else if (baseSrcref != null && positionSrcref != null) {
			currentSrcref = Srcref.add(baseSrcref, positionSrcref);
			if (currentSrcref != null && lastSrcref != null) {
				if (currentSrcref[0] < lastSrcref[0]
						|| currentSrcref[2] > lastSrcref[2] ) {
					currentSrcref = null;
				}
				else {
					if (currentSrcref[4] != Integer.MIN_VALUE
							&& lastSrcref[4] != Integer.MIN_VALUE
							&& currentSrcref[0] == lastSrcref[0]
							&& currentSrcref[4] < lastSrcref[4] ) {
						currentSrcref[4] = Integer.MIN_VALUE;
					}
					if (currentSrcref[5] != Integer.MIN_VALUE
							&& lastSrcref[5] != Integer.MIN_VALUE
							&& currentSrcref[2] == lastSrcref[2]
							&& currentSrcref[5] > lastSrcref[5] ) {
						currentSrcref[5] = Integer.MIN_VALUE;
					}
				}
			}
		}
		if (currentSrcref == null && lastSrcref != null) {
			currentSrcref = lastSrcref;
		}
		return currentSrcref;
	}
	
	private long createTraceLang(long currentP, final List<? extends TracepointPosition> list,
			final FunInfo funInfo, final long filePathP, final long elementIdP,
			final int[] currentSrcref) {
		for (int i = list.size()-1; i >= 0; i--) {
			int n;
			final TracepointPosition position = list.get(i);
			if (position.getType() == Tracepoint.TYPE_LB) {
				final long breakpointP = createBreakpointCall(position, currentSrcref,
						funInfo, filePathP, elementIdP, 0 );
				currentP = this.rEngine.rniCons(
						breakpointP, this.rEngine.rniCons(
								currentP, this.rni.p_NULL,
								0, false ),
						0, false );
				n = 1;
			}
			else if (position.getType() == Tracepoint.TYPE_FB) {
				final long entryP = createBreakpointCall(position, currentSrcref,
						funInfo, filePathP, elementIdP, TracepointState.FLAG_MB_ENTRY );
				long exitP = createBreakpointCall(position, currentSrcref,
						funInfo, filePathP, elementIdP, TracepointState.FLAG_MB_EXIT );
				exitP = this.rni.protect(this.rEngine.rniCons(
						this.rni.p_onExitSymbol, this.rEngine.rniCons(
								exitP, this.rni.p_NULL,
								0, false ),
						0, true ));
				currentP = this.rEngine.rniCons(
						exitP, this.rEngine.rniCons(
							entryP, this.rEngine.rniCons(
									currentP, this.rni.p_NULL,
									0, false ),
							0, false ),
						0, false );
				n = 2;
			}
			else {
				continue;
			}
			currentP = this.rni.protect(this.rEngine.rniCons(
					this.rni.p_BlockSymbol, currentP,
					0, true ));
			this.rEngine.rniSetAttrBySym(currentP, this.rni.p_srcrefSymbol, createTraceSrcref(n,
					(currentSrcref != null) ? currentSrcref : NA_SRCREF, funInfo.srcfileP, elementIdP ));
		}
		return currentP;
	}
	
	private long createBreakpointCall(final TracepointPosition position, final int[] srcref,
			final FunInfo funInfo, final long filePathP, final long elementIdP, final int flags) {
		final long exprP = this.rEngine.rniDuplicate(this.breakpointTmplP);
		{	final long[] srcrefList = new long[2];
			srcrefList[0] = this.rni.protect(this.rEngine.rniDuplicate(this.stepSrcrefTmplP));
			srcrefList[1] = this.rni.protect(this.rEngine.rniPutIntArray(
					(srcref != null) ? srcref : NA_SRCREF ));
			this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_srcfileSymbol, funInfo.srcfileP);
			if (filePathP != 0) {
				this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_appFilePathSymbol, filePathP);
			}
			this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_dbgElementIdSymbol, elementIdP);
			this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_atSymbol,
					this.rEngine.rniPutIntArray(position.getIndex()) );
			this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_idSymbol,
					this.rEngine.rniPutRawArray(RDataUtil.encodeLongToRaw(position.getId())) );
			if (flags != 0) {
				this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_flagsSymbol,
						this.rEngine.rniPutIntArray(new int[] { flags }) );
			}
			this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_whatSymbol, this.stepNextValueP);
			this.rEngine.rniSetAttrBySym(exprP, this.rni.p_srcrefSymbol, this.rEngine.rniPutVector(srcrefList));
		}
		return exprP;
	}
	
	private long createTraceSrcref(final int n, final int[] srcref,
			final long srcfileP, final long elementIdP) {
		final long[] list = new long[n+2];
		list[0] = this.rni.protect(this.rEngine.rniPutIntArray(srcref));
		this.rEngine.rniSetAttrBySym(list[0], this.rni.p_srcfileSymbol, srcfileP);
		this.rEngine.rniSetAttrBySym(list[0], this.rni.p_whatSymbol, this.stepNextValueP);
		this.rEngine.rniSetAttrBySym(list[0], this.rni.p_dbgElementIdSymbol, elementIdP);
		for (int i = 1; i <= n; i++) {
			list[i] = list[0];
		}
		list[n+1] = this.rni.protect(this.rEngine.rniPutIntArray(srcref));
		this.rEngine.rniSetAttrBySym(list[n+1], this.rni.p_srcfileSymbol, srcfileP);
		this.rEngine.rniSetAttrBySym(list[n+1], this.rni.p_dbgElementIdSymbol, elementIdP);
		return this.rEngine.rniPutVector(list);
	}
	
	/**
	 * Updates the state of the specified tracepoints
	 * 
	 * @param request the request with tracepoint state updates
	 * @param reset if all existing tracepoint state should be reset
	 * @return update result status
	 */
	public RjsStatus updateTracepointStates(final TracepointStatesUpdate request) {
		synchronized (this.tracepointMap) {
			if (request.getReset()) {
				this.tracepointMap.clear();
			}
			final List<TracepointState> list = request.getStates();
			String path = null;
			List<TracepointState> pathList = null;
			for (int i = 0; i < list.size(); i++) {
				final TracepointState state = list.get(i);
				if (path != state.getFilePath()) {
					path = state.getFilePath();
					pathList = this.tracepointMap.get(path);
					if (pathList == null) {
						if (state.getType() == Tracepoint.TYPE_DELETED) {
							continue;
						}
						pathList = new ArrayList<TracepointState>(8);
						this.tracepointMap.put(path, pathList);
					}
				}
				final int idx = pathList.indexOf(state);
				if (idx >= 0) {
					final TracepointState oldState = pathList.remove(idx);
					if (oldState instanceof TracepointStateWithData) {
						final long p = ((TracepointStateWithData) oldState).getExprP();
						if (p != 0) {
							synchronized (this.toRelease) {
								this.checkToRelease = true;
								this.toRelease.add(Long.valueOf(p));
							}
						}
					}
				}
				if (state.getType() == Tracepoint.TYPE_DELETED) {
					continue;
				}
				pathList.add(state);
			}
			if (this.tracepointMap.size() > 32) {
				final Iterator<Entry<String, List<TracepointState>>> iter = this.tracepointMap.entrySet().iterator();
				while (iter.hasNext()) {
					if (iter.next().getValue().isEmpty()) {
						iter.remove();
					}
				}
			}
		}
		return RjsStatus.OK_STATUS;
	}
	
	private long checkBreakpoint(final long callP) throws RjException {
		if (this.disabled || !this.breakpointsEnabled) {
			return 0;
		}
		final long srcrefP = this.rEngine.rniGetAttrBySym(callP, this.rni.p_srcrefSymbol);
		if (srcrefP == 0) {
			throw new RjException("Missing data: srcref.");
		}
		final long srcfileP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_srcfileSymbol);
		if (srcfileP == 0 || this.rEngine.rniExpType(srcfileP) != REXP.ENVSXP) {
			throw new RjException("Missing data: srcfile env of srcref.");
		}
		final String filePath = getFilePath(srcfileP, srcrefP);
		if (filePath == null) {
			throw new RjException("Missing data: path of srcref.");
		}
		final long idP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_idSymbol);
		final long atP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_atSymbol);
		if (idP == 0 || atP == 0 ) {
			throw new RjException("Missing data: id/position.");
		}
		final long id = RDataUtil.decodeLongFromRaw(this.rEngine.rniGetRawArray(idP));
		final int[] index = this.rEngine.rniGetIntArray(atP);
		TracepointState breakpointState = null;
		synchronized (this.tracepointMap) {
			final List<TracepointState> list = this.tracepointMap.get(filePath);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					final TracepointState state = list.get(i);
					if ((state.getType() & Tracepoint.TYPE_BREAKPOINT) != 0
							&& id == state.getId()) {
						breakpointState = state;
						break;
					}
				}
				if (breakpointState == null) {
					String elementId = null;
					final long elementIdP = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_dbgElementIdSymbol);
					if (elementIdP != 0) {
						elementId = this.rEngine.rniGetString(elementIdP);
					}
					if (elementId != null) {
						for (int i = 0; i < list.size(); i++) {
							final TracepointState state = list.get(i);
							if ((state.getType() & Tracepoint.TYPE_BREAKPOINT) != 0
									&& elementId.equals(state.getElementId())
									&& Arrays.equals(index, state.getIndex()) ) {
								breakpointState = state;
								break;
							}
						}
					}
				}
			}
		}
		if (breakpointState == null) {
			LOGGER.log(Level.FINE, "Skipping breakpoint because of missing state.");
			return 0;
		}
		if (!breakpointState.isEnabled()) {
//			LOGGER.log(Level.FINER, "Skipping breakpoint because it is disabled.");
			return 0;
		}
		
		this.hitBreakpointState = null;
		this.hitBreakpointSrcref = this.rEngine.rniGetIntArray(srcrefP);
		if (this.hitBreakpointSrcref == null) {
			throw new RjException("Missing data: srcref values.");
		}
		if (!this.breakpointsEnabled) {
			return 0;
		}
		
		int codeFlags = 0;
		{	// load flags
			final long p = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_flagsSymbol);
			if (p != 0) {
				codeFlags = this.rEngine.rniGetIntArray(p)[0];
			}
		}
		int flags = 0;
		{	// check flags
			final int stateFlags = breakpointState.getFlags();
			if (breakpointState.getType() == Tracepoint.TYPE_FB) {
				if ((codeFlags & stateFlags & (TracepointState.FLAG_MB_ENTRY | TracepointState.FLAG_MB_EXIT)) == 0) {
//					LOGGER.log(Level.FINER, "Skipping breakpoint because current position is disabled.");
					return 0;
				}
			}
			flags |= (codeFlags & 0x00ff0000);
		}
		if (breakpointState.getExpr() != null) { // check expr
			final long exprP = getTracepointEvalExpr(breakpointState);
			if (exprP == 0) {
//				LOGGER.log(Level.WARNING, "Creating expression for breakpoint condition failed.");
				return 0;
			}
			
			this.disabled = true;
			try {
				final long envP = this.rEngine.rniEval(this.getTraceExprEvalEnvCallP, 0);
				if (envP == 0) {
					LOGGER.log(Level.SEVERE, "Creating environment for breakpoint condition failed.");
					return 0;
				}
				this.rni.protect(envP);
				final long p = this.rni.evalExpr(exprP, envP, 1);
				if (!this.rEngine.rniIsTrue(p)) {
					return 0;
				}
			}
			catch (final RjsException e) {
//				LOGGER.log(Level.FINER, "Skipping breakpoint because evaluating the condition failed.", e);
				// evaluation failed (expression invalid...)
//				TODO notify ?
//				e.printStackTrace();
				return 0;
			}
			finally {
				this.disabled = false;
			}
		}
		
		final long browserExprP = this.rni.protect(this.rEngine.rniDuplicate(this.browserExprP));
		{	final long[] srcrefList = new long[2];
			srcrefList[0] = this.rni.protect(this.rEngine.rniDuplicate(this.stepSrcrefTmplP));
			srcrefList[1] = this.rni.protect(this.rEngine.rniPutIntArray(this.hitBreakpointSrcref));
			if ((codeFlags & TracepointState.FLAG_MB_EXIT) == 0) {
				this.rEngine.rniSetAttrBySym(srcrefList[1], this.rni.p_whatSymbol, this.stepNextValueP);
				srcrefList[1] = this.rni.protect(this.rEngine.rniDuplicate(this.stepSrcrefTmplP));
			}
			this.rEngine.rniSetAttrBySym(browserExprP, this.rni.p_srcrefSymbol,
					this.rEngine.rniPutVector(srcrefList) );
		}
		
		if (this.hitBreakpointSrcref != null && this.hitBreakpointSrcref.length >= 6) {
			this.hitBreakpointState = breakpointState;
			this.hitBreakpointFlags = flags;
			this.hitBreakpointSrcfile = srcfileP;
		}
		return browserExprP;
	}
	
	private long getTracepointEvalExpr(final TracepointState breakpointState) {
		final String expr = breakpointState.getExpr();
		if (expr == null) {
			return 0;
		}
		final TracepointStateWithData stateWithData = (breakpointState instanceof TracepointStateWithData) ?
				(TracepointStateWithData) breakpointState : new TracepointStateWithData(breakpointState);
		boolean parsed = false;
		long exprP = stateWithData.getExprP();
		if (exprP == 0 && (stateWithData.getFlags() & TracepointState.FLAG_EXPR_INVALID) == 0) {
			parsed = true;
			exprP = this.rEngine.rniParse("{\n" + expr + "\n}", 1);
			if (exprP != 0) {
				exprP = this.rEngine.rniGetVectorElt(exprP, 0);
			}
			if (exprP == 0) {
//				sendNotification(notification);
			}
		}
		if (exprP != 0) {
			this.rni.protect(exprP);
		}
		if (parsed || stateWithData != breakpointState) {
			synchronized (this.tracepointMap) {
				if (parsed) {
					stateWithData.setParsedExpr(exprP);
				}
				final List<TracepointState> list = this.tracepointMap.get(breakpointState.getFilePath());
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						final TracepointState state = list.get(i);
						if (state == breakpointState) {
							this.rEngine.rniPreserve(exprP);
							list.set(i, stateWithData);
							break;
						}
						else if (state == stateWithData) {
							this.rEngine.rniPreserve(exprP);
							break;
						}
					}
				}
			}
		}
		return exprP;
	}
	
	
	private void sendNotification(final TracepointEvent notification) {
		if (this.tracepointListener != null) {
			this.tracepointListener.handle(notification);
		}
	}
	
	
	private int getNFrame() throws RjsException {
		final long p = this.rEngine.rniEval(this.sysNframeCallP, 0 );
		if (p != 0) {
			final int[] na = this.rEngine.rniGetIntArray(p);
			if (na != null && na.length > 0) {
				return na[0];
			}
		}
		return -1;
	}
	
	private long getOrgFun(final long funP) {
		final long p = this.rEngine.rniGetAttrBySym(funP, this.rni.p_originalSymbol);
		if (p != 0 && p != funP /*&& isTraceable(funP)*/) {
			if (this.rEngine.rniExpType(p) != REXP.CLOSXP) {
				return 0;
			}
			return p;
		}
		return funP;
	}
	
	private boolean isTraceable(final long funP) {
		final long p = this.rEngine.rniEval(this.rEngine.rniCons(
				this.isFunP, this.rEngine.rniCons(
						funP, this.rEngine.rniCons(
								this.traceableStringP, this.rni.p_NULL,
								0, false ),
						0, false ),
				0, true),
				0 );
		return (p != 0 && this.rEngine.rniIsTrue(p));
	}
	
	private String getFilePath(final long srcfileP, final long srcrefP) {
		long p = this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_appFilePathSymbol);
		if (p == 0 && (srcrefP == 0
				|| (p = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_appFilePathSymbol)) == 0 )) {
			return null;
		}
		return this.rEngine.rniGetString(p);
	}
	
	private String getFileName(final long srcfileP) {
		String filename;
		{	final long p = this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_filenameSymbol);
			if (p == 0
					|| (filename = this.rEngine.rniGetString(p)) == null
					|| filename.length() == 0) {
				return null;
			}
		}
		if (filename.charAt(0) == '<') {
			return null;
		}
		if (filename.charAt(0) != '/' && filename.charAt(0) != '\\'
				&& filename.indexOf(':') < 0) {
			{	final long p = this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_wdSymbol);
				if (p != 0) {
					final String wd = this.rEngine.rniGetString(p);
					if (wd != null && wd.length() > 0) {
						filename = wd + File.separatorChar + filename;
					}
				}
			}
		}
		return checkFilename(filename);
	}
	
	private String getFileEncoding(final long srcfileP) {
		final long p = this.rEngine.rniGetVar(srcfileP, "encoding");
		if (p != 0) {
			return this.rEngine.rniGetString(p);
		}
		return null;
	}
	
	private long getFileTimestamp(final long srcfileP) {
		final long p = this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_timestampSymbol);
		if (p != 0) {
			final double[] array = this.rEngine.rniGetDoubleArray(p);
			if (array != null && array.length > 0) {
				return (long) array[0];
			}
		}
		return 0;
	}
	
	private String checkFilename(final String filename) {
		if (filename.charAt(0) == '\\') {
			return (filename.indexOf('/', 1) > 0) ? filename.replace('/', '\\') : filename;
		}
		else {
			return (filename.indexOf('\\', 1) > 0) ?filename.replace('\\', '/') : filename;
		}
	}
	
	private int[] getSrcrefObject(final long srcrefP) {
		if (srcrefP != 0) {
			final int[] array = this.rEngine.rniGetIntArray(srcrefP);
			if (array != null && array.length >= 6 && array[0] != Integer.MIN_VALUE) {
				return array;
			}
		}
		return null;
	}
	
}
