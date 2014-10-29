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

import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_CONTEXT;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_DEBUG;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_TRACE;
import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import de.walware.rj.RjException;
import de.walware.rj.server.RjsException;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.dbg.CallStack;
import de.walware.rj.server.dbg.CtrlReport;
import de.walware.rj.server.dbg.DbgEnablement;
import de.walware.rj.server.dbg.DbgFilterState;
import de.walware.rj.server.dbg.DbgListener;
import de.walware.rj.server.dbg.DbgRequest;
import de.walware.rj.server.dbg.Frame;
import de.walware.rj.server.dbg.FrameContext;
import de.walware.rj.server.dbg.FrameContextDetailRequest;
import de.walware.rj.server.dbg.FrameRef;
import de.walware.rj.server.dbg.SetDebugReport;
import de.walware.rj.server.dbg.SetDebugRequest;
import de.walware.rj.server.dbg.Srcref;
import de.walware.rj.server.dbg.TracepointEvent;


final class JRIServerDbg {
	
	
	private static final class CallStackFrame extends Frame {
		
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
	
	
	private final JRIServer server;
	private final Rengine rEngine;
	private final JRIServerRni rni;
	private final ServerUtils utils;
	
	private int disabled;
	private int savedTracingState;
	
	private final DbgListener dbgListener;
	
	private boolean stepFilterEnabled = !Boolean.parseBoolean(
			System.getProperty("de.walware.rj.dbg.stepfilter.disabled")); //$NON-NLS-1$
	
	private boolean isSuspended; // browser prompt
	
	private boolean deferredSuspend;
	private final List<Long> contextTmpDebugFrames = new ArrayList<Long>();
	private final List<Long> contextTmpDebugFuns = new ArrayList<Long>();
	
	private boolean checkToRelease;
	private final List<Long> toRelease = new ArrayList<Long>();
	
	private final TracepointManager tracepointManager;
	
	private final long sysNFrameFunP;
	private final long sysFramesFunP;
	private final long sysCallFunP;
	private final long sysFunctionFunP;
	
	private final long sysNFrameCallP;
	private final long sysFramesCallP;
	private final long sysCalls0CallP;
	private final long sysCall0CallP;
	
	private final long getTopFrameCallP;
	
	private final long isFunP;
	private final long traceableStringP;
	
	private final long enableTraceingCallP;
	private final long disableTraceingCallP;
	
	
	public JRIServerDbg(final JRIServer server, final JRIServerRni rni, final ServerUtils utils) {
		this.server= server;
		this.rEngine = rni.getREngine();
		this.rni = rni;
		this.utils = utils;
		
		this.dbgListener= server;
		
		this.tracepointManager= new TracepointManager(this, rni);
		
		this.sysNFrameFunP= this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.nframe"), //$NON-NLS-1$
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysNFrameFunP);
		this.sysFramesFunP= this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.frames"), //$NON-NLS-1$
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysFramesFunP);
		this.sysCallFunP= this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.call"), //$NON-NLS-1$
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysCallFunP);
		this.sysFunctionFunP = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.function"), //$NON-NLS-1$
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.sysFunctionFunP);
		
		this.sysNFrameCallP = this.rEngine.rniCons(this.sysNFrameFunP, this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysNFrameCallP);
		this.sysFramesCallP = this.rEngine.rniCons(this.sysFramesFunP, this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysFramesCallP);
		this.sysCalls0CallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rni.protect(this.rEngine.rniCons(
						this.rni.p_functionSymbol, this.rEngine.rniCons(
								this.rni.p_NULL, this.rEngine.rniCons(
										this.rEngine.rniCons(
												this.rEngine.rniEval(this.rEngine.rniInstallSymbol("sys.calls"), //$NON-NLS-1$
														this.rni.p_BaseEnv ), this.rni.p_NULL,
												0, true ), this.rni.p_NULL,
										0, true),
								0, false ),
						0, true )),
						this.rni.p_BaseEnv ), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysCalls0CallP);
		this.sysCall0CallP = this.rEngine.rniCons(
				this.rEngine.rniEval(this.rni.protect(this.rEngine.rniCons(
						this.rni.p_functionSymbol, this.rEngine.rniCons(
								this.rni.p_NULL, this.rEngine.rniCons(
										this.rEngine.rniCons(this.sysCallFunP, this.rni.p_NULL,
												0, true ), this.rni.p_NULL,
										0, true),
								0, false ),
						0, true )),
						this.rni.p_BaseEnv ), this.rni.p_NULL,
				0, true );
		this.rEngine.rniPreserve(this.sysCall0CallP);
		
		this.getTopFrameCallP = this.rEngine.rniCons(
				this.sysFramesFunP, this.rEngine.rniCons(
						this.sysNFrameCallP, this.rni.p_NULL,
						0, false), // which
				0, true );
		this.rEngine.rniPreserve(this.getTopFrameCallP);
		
		this.isFunP = this.rEngine.rniEval(this.rEngine.rniParse("methods::is", 1), //$NON-NLS-1$
				this.rni.p_BaseEnv );
		this.rEngine.rniPreserve(this.isFunP);
		this.traceableStringP = this.rEngine.rniPutString("traceable"); //$NON-NLS-1$
		this.rEngine.rniPreserve(this.traceableStringP);
		
		this.enableTraceingCallP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"tracingState(TRUE)", 1 ), //$NON-NLS-1$
				0 );
		this.rEngine.rniPreserve(this.enableTraceingCallP);
		
		this.disableTraceingCallP = this.rEngine.rniGetVectorElt(this.rEngine.rniParse(
				"tracingState(FALSE)", 1 ), //$NON-NLS-1$
				0 );
		this.rEngine.rniPreserve(this.disableTraceingCallP);
	}
	
	
	public void beginSafeMode() {
		disable(true);
		
		if (this.savedTracingState == 0) {
			final long p = this.rEngine.rniEval(this.disableTraceingCallP, this.rni.p_BaseEnv);
			if (p != 0) {
				this.savedTracingState= this.rEngine.rniIsTrue(p) ? 1 : -1;
			}
		}
	}
	
	public void endSafeMode() {
		disable(false);
	}
	
	void disable(final boolean on) {
		if (on) {
			this.disabled++;
		}
		else {
			this.disabled--;
			
			if (this.disabled == 0) {
				reEnabled();
			}
		}
	}
	
	private void reEnabled() {
		if (this.deferredSuspend) {
			doSuspend();
		}
		if (this.savedTracingState != 0) {
			this.savedTracingState= 0;
			if (this.savedTracingState == 1) {
				this.rEngine.rniEval(this.enableTraceingCallP, this.rni.p_BaseEnv);
			}
		}
	}
	
	
	public synchronized void setEnablement(final DbgEnablement request) {
		this.tracepointManager.setBreakpointsEnabled(request.getBreakpointsEnabled());
	}
	
	
	public TracepointManager getTracepointManager() {
		return this.tracepointManager;
	}
	
	
	public String handleBrowserPrompt(final String prompt) {
		if (this.disabled > 0) {
			return "c\n"; //$NON-NLS-1$
		}
		final long srcrefP;
		{	final long call = this.rEngine.rniEval(this.sysCall0CallP, 0);
			srcrefP = (call != 0) ? this.rEngine.rniGetAttrBySym(call, this.rni.p_srcrefSymbol) : 0;
		}
		if (srcrefP != 0) {
			final long p = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_whatSymbol);
			if (p != 0) {
				final String s = this.rEngine.rniGetString(p);
				if (s != null && s.length() > 8 && s.startsWith("browser:")) { //$NON-NLS-1$
					return s.substring(8)+'\n';
				}
			}
		}
		
		clearContext();
		
		this.tracepointManager.handleSuspended(srcrefP);
		
		this.isSuspended= true;
		
		return null;
	}
	
	public void rCancelled() {
		this.tracepointManager.handleCancelled();
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
			if (commandId.equals("checkBreakpoint")) { //$NON-NLS-1$
				if (this.disabled > 0) {
					return 0;
				}
				return this.tracepointManager.checkBreakpoint(argsP);
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
	 * Returns the list of the current frames
	 * 
	 * @return the frame list or <code>null</code> if not available
	 * @throws RjsException
	 */
	
	public CallStack getCallStack() throws RjsException {
		return loadCallStack();
	}
	
	private CallStack loadCallStack() throws RjsException {
		int n = getNFrame();
		if (n < 0) {
			return null;
		}
		
		final List<CallStackFrame> list = new ArrayList<CallStackFrame>(n + 1);
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
							final int[] add= getSrcrefObject(this.rEngine.rniGetVarBySym(
									srcfileP, this.rni.p_linesSrcrefSymbol, 0 ));
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
						&& (call.startsWith(".doTrace") || call.startsWith("rj:::.breakpoint(")) ) { //$NON-NLS-1$ //$NON-NLS-2$
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
				final long sourceP= this.rEngine.rniGetVarBySym(
						contextSrcfileP, this.rni.p_linesSymbol, 0 );
				if (sourceP != 0) {
					final String[] sourceLines = this.rEngine.rniGetStringArray(sourceP);
					if (sourceLines != null && sourceLines.length > 0) {
						sourceType = FrameContext.SOURCETYPE_1_LINES;
						sourceCode = this.utils.concat(sourceLines, '\n');
						sourceSrcref= getSrcrefObject(this.rEngine.rniGetVarBySym(
								contextSrcfileP, this.rni.p_linesSrcrefSymbol, 0 ));
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
						encoding = "UTF-8"; //$NON-NLS-1$
					}
					Charset charset;
					if (encoding.equals("native.enc")) { //$NON-NLS-1$
						charset = Charset.defaultCharset();
					}
					else {
						try {
							charset = Charset.forName(encoding);
						}
						catch (final Exception e) {
							charset = Charset.forName("UTF-8"); //$NON-NLS-1$
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
								call.substring(0, idx) : "f") + " <- " + sourceCode; //$NON-NLS-1$ //$NON-NLS-2$
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
		if (this.disabled > 0) {
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
				final Frame frame0 = stack.getFrames().get(i);
				add(handles, frame0);
			}
		}
		else {
			for (int i = n-1; i >= last; i--) {
				final Frame frame0 = stack.getFrames().get(i);
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
							&& frame0.getCall() != null && frame0.getCall().startsWith("eval(")) { //$NON-NLS-1$
						final Frame frame1 = stack.getFrames().get(i-1);
						if (frame1.getCall() != null && frame1.getCall().startsWith("eval(")) { //$NON-NLS-1$
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
	
	private void add(final long[] frameIds, final Frame frame) {
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
		final long frameId = getTopFrame();
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
	
	public CtrlReport exec(final DbgRequest request) throws RjsException {
		switch (request.getOp()) {
		
		case DbgRequest.RESUME:
			if (this.isSuspended) {
				scheduleResume("c"); //$NON-NLS-1$
				return CtrlReport.createRequestExecuted(DbgRequest.RESUME);
			}
			return CtrlReport.createRequestNotApplicable(this.isSuspended);
		
		case DbgRequest.STEP_INTO:
			if (this.isSuspended) {
				if (!this.utils.isRVersionEqualGreater(3, 1)) {
					return CtrlReport.createRequestNotSupported(this.isSuspended);
				}
				scheduleResume("s"); //$NON-NLS-1$
				return CtrlReport.createRequestExecuted(DbgRequest.STEP_INTO);
			}
			return CtrlReport.createRequestNotApplicable(this.isSuspended);
		
		case DbgRequest.STEP_OVER:
			if (this.isSuspended) {
				scheduleResume("n"); //$NON-NLS-1$
				return CtrlReport.createRequestExecuted(DbgRequest.STEP_OVER);
			}
			return CtrlReport.createRequestNotApplicable(this.isSuspended);
		
		case DbgRequest.STEP_RETURN:
			if (this.isSuspended) {
				final CallStack callStack= getCallStack();
				final Object target= ((DbgRequest.StepReturn) request).getTarget();
				Frame targetFrame= null;
				if (target instanceof FrameRef.ByHandle) {
					final long targetHandle= ((FrameRef.ByHandle) target).getHandle();
					targetFrame= callStack.findFrame(targetHandle);
				}
				if (targetFrame != null && !targetFrame.isTopFrame()) {
					doSetDebug(targetFrame.getHandle(), 1, false);
					scheduleResume("c"); //$NON-NLS-1$
//					scheduleResume("browserSetDebug(n= " + (relPos) + "L); c"); //$NON-NLS-1$ //$NON-NLS-2$
					return CtrlReport.createRequestExecuted(DbgRequest.STEP_RETURN);
				}
			}
			return CtrlReport.createRequestNotApplicable(this.isSuspended);
		
		default:
			return CtrlReport.createRequestNotSupported(this.isSuspended);
		}
	}
	
	private void scheduleResume(final String rCommand) {
		this.server.dorAppend2SConsoleAnswer(rCommand);
	}
	
	
	public void addToRelease(final long p) {
		synchronized (this.toRelease) {
			this.checkToRelease= true;
			this.toRelease.add(p);
		}
	}
	
	public void clearContext() {
		this.isSuspended= false;
		
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
	
	
	void sendNotification(final TracepointEvent notification) {
		if (this.dbgListener != null) {
			this.dbgListener.handle(notification);
		}
	}
	
	
	long getTopFrame() {
		return this.rEngine.rniEval(this.getTopFrameCallP, this.rni.p_BaseEnv);
	}
	
	void addAllStackEnvs(final List<Long> envs) throws RjsException {
		this.rni.addAllEnvs(envs, this.rni.p_GlobalEnv);
		{	long cdr= this.rni.evalExpr(this.sysFramesCallP, 0, CODE_DBG_TRACE);
			if (cdr != 0 && this.rEngine.rniExpType(cdr) == REXP.LISTSXP) {
				while (cdr != 0) {
					final long car= this.rEngine.rniCAR(cdr);
					if (car != 0 && this.rEngine.rniExpType(car) == REXP.ENVSXP) {
						this.rni.addAllEnvs(envs, car);
					}
					cdr= this.rEngine.rniCDR(cdr);
				}
			}
		}
	}
	
	int getNFrame() throws RjsException {
		final long p = this.rEngine.rniEval(this.sysNFrameCallP, 0 );
		if (p != 0) {
			final int[] na = this.rEngine.rniGetIntArray(p);
			if (na != null && na.length > 0) {
				return na[0];
			}
		}
		return -1;
	}
	
	long getOrgFun(final long funP) {
		final long p = this.rEngine.rniGetAttrBySym(funP, this.rni.p_originalSymbol);
		if (p != 0 && p != funP /*&& isTraceable(funP)*/) {
			if (this.rEngine.rniExpType(p) != REXP.CLOSXP) {
				return 0;
			}
			return p;
		}
		return funP;
	}
	
	boolean isTraceable(final long funP) {
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
	
	String getFilePath(final long srcfileP, final long srcrefP) {
		long p= this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_appFilePathSymbol, 0);
		if (p == 0 && (srcrefP == 0
				|| (p = this.rEngine.rniGetAttrBySym(srcrefP, this.rni.p_appFilePathSymbol)) == 0 )) {
			return null;
		}
		return this.rEngine.rniGetString(p);
	}
	
	String getFileName(final long srcfileP) {
		String filename;
		{	final long p= this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_filenameSymbol, 0);
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
			{	final long p= this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_wdSymbol, 0);
				if (p != 0) {
					final String wd = this.rEngine.rniGetString(p);
					if (wd != null && wd.length() > 0) {
						filename = wd + File.separatorChar + filename;
					}
				}
			}
		}
		return this.utils.checkFilename(filename);
	}
	
	String getFileEncoding(final long srcfileP) {
		final long p= this.rEngine.rniGetVarBySym(srcfileP, this.rni.encodingSymP, 0);
		if (p != 0) {
			return this.rEngine.rniGetString(p);
		}
		return null;
	}
	
	long getFileTimestamp(final long srcfileP) {
		final long p= this.rEngine.rniGetVarBySym(srcfileP, this.rni.p_timestampSymbol, 0);
		if (p != 0) {
			final double[] array = this.rEngine.rniGetDoubleArray(p);
			if (array != null && array.length > 0) {
				return (long) array[0];
			}
		}
		return 0;
	}
	
	int[] getSrcrefObject(final long srcrefP) {
		if (srcrefP != 0) {
			final int[] array = this.rEngine.rniGetIntArray(srcrefP);
			if (array != null && array.length >= 6 && array[0] != Integer.MIN_VALUE) {
				return array;
			}
		}
		return null;
	}
	
}
