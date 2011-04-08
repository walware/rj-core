/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.client;

import static de.walware.rj.server.srvext.ServerUtil.MISSING_ANSWER_STATUS;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.OutputStream;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import de.walware.rj.RjException;
import de.walware.rj.data.RJIO;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RReference;
import de.walware.rj.server.BinExchange;
import de.walware.rj.server.ComHandler;
import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.ConsoleReadCmdItem;
import de.walware.rj.server.CtrlCmdItem;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsPing;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.services.RPlatform;
import de.walware.rj.services.RService;


/**
 * Generic RJ Com protocol client for servers offering a {@link ConsoleEngine}.
 * <p>
 * It offers basic implementation for most methods of the {@link RService} API,
 * including:</p>
 * <ul>
 *   <li>Expression evaluation</li>
 *   <li>Data exchange to assign or eval/read {@link RObject}</li>
 *   <li>File exchange to write/read file on the server (must be enabled at server side)</li>
 *   <li>R graphics (requires an {@link RClientGraphicFactory}, set in {@link #initGraphicFactory()},
 *       or via {@link #setGraphicFactory(RClientGraphicFactory, RClientGraphicActions)})</li>
 *   <li>Console (REPL), if connected to server slot 0</li>
 * </ul>
 * <p>
 * If offers two mechanisms allowing the usage of RService API and R thread when they are already
 * used by a regular call. So the two modes allows to "hijack" the R thread to "inject" additional
 * calls to the RService API:</p>
 * <ul>
 *   <li>Hot Mode: Enables to run code in any situation, also if R is busy. Nesting of hot modes is
 *       not possible. It can be requested asynchronous from any thread. The hot mode is appropriate
 *       for e.g. short task for the GUI.</li>
 *   <li>Extra Mode: Enables to run code at the before or after of a client-server communication.
 *       Must be requested synchronous from the R thread.</li>
 * </ul>
 */
public abstract class AbstractRJComClient implements ComHandler {
	
	
	public static final String RJ_CLIENT_ID = "de.walware.rj.client";
	
	public static int[] version() {
		return new int[] { 1, 0, 0 };
	}
	
	public static final int EXTRA_BEFORE = 1;
	public static final int EXTRA_NESTED = 2;
	
	private static final ScheduledExecutorService RJHelper_EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("RJHelper"));
	
	
	private class LazyGraphicFactory implements RClientGraphicFactory {
		
		
		private boolean initialized;
		
		
		public RClientGraphic newGraphic(final int devId, final double w, final double h,
				final boolean active, final RClientGraphicActions actions, final int options) {
			if (!this.initialized) {
				this.initialized = true;
				try {
					initGraphicFactory();
				}
				catch (final Exception e) {
					log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when initializing R client graphic factory.", e));
				}
				if (AbstractRJComClient.this.graphicFactory != LazyGraphicFactory.this) {
					return AbstractRJComClient.this.graphicFactory.newGraphic(devId, w, h,
							active, AbstractRJComClient.this.graphicActions, options);
				}
				else {
					log(new Status(IStatus.WARNING, RJ_CLIENT_ID, -1, "No R client graphic factory configured.", null));
				}
			}
			return new RClientGraphicDummy(devId, w, h);
		}
		
		public void closeGraphic(final RClientGraphic graphic) {
		}
		
	}
	
	private class KeepAliveRunnable implements Runnable {
		
		public void run() {
			AbstractRJComClient.this.runAsyncPing();
		}
		
	}
	
	private class HotModeRequestRunnable implements Runnable {
		
		public void run() {
			if (AbstractRJComClient.this.hotModeRequested.get()) {
				try {
					runAsyncCtrl(CtrlCmdItem.REQUEST_HOT_MODE);
				}
				catch (final CoreException e) {
					if (e.getStatus().getSeverity() != IStatus.CANCEL) {
						log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when requesting hot mode.", e));
					}
				}
			}
		}
	}
	
	
	private IProgressMonitor progressMonitor;
	
	private final RJIO mainIO = new RJIO();
	private MainCmdItem mainC2SFirst;
	private final MainCmdC2SList mainC2SList = new MainCmdC2SList(this.mainIO);
	private boolean mainRunGC;
	
	private boolean consoleReadCallbackRequired;
	private ConsoleReadCmdItem consoleReadCallback;
	
	private final Object platformLock = new Object();
	private Map<String, Object> platformData;
	private RPlatform platformObj;
	
	private int dataLevelRequest = 0;
	private int dataLevelAnswer = 0;
	private int dataLevelIgnore = 0;
	private final DataCmdItem[] dataAnswer = new DataCmdItem[16];
	
	private int hotModeState;
	private ConsoleReadCmdItem hotModeReadCallbackBackup;
	private MainCmdItem hotModeC2SFirstBackup;
	private final AtomicBoolean hotModeRequested = new AtomicBoolean();
	private final Runnable hotModeRunnable = new HotModeRequestRunnable();
	
	private int extraModeRequested;
	
	private RClientGraphicFactory graphicFactory;
	private RClientGraphicActions graphicActions;
	private final RClientGraphic graphicDummy = new RClientGraphicDummy(1, 0, 0);
	private RClientGraphic[] graphics = new RClientGraphic[16];
	private int currentGraphicOptions;
	private RClientGraphic lastGraphic;
	
	private ConsoleEngine rjConsoleServer;
	
	private boolean closed;
	private ScheduledFuture<?> keepAliveJob;
	private String closedMessage = "Connection to R engine is closed.";
	
	
	protected AbstractRJComClient() {
		this.graphicFactory = new LazyGraphicFactory();
	}
	
	@Deprecated
	public final void setServer(final ConsoleEngine rjServer) {
		setServer(rjServer, 1);
	}
	
	public final void setServer(final ConsoleEngine rjServer, final int client) {
		this.rjConsoleServer = rjServer;
		if (client == 0) {
			this.keepAliveJob = RJHelper_EXECUTOR.scheduleWithFixedDelay(new KeepAliveRunnable(), 50, 50, TimeUnit.SECONDS);
		}
	}
	
	public final ConsoleEngine getConsoleServer() {
		return this.rjConsoleServer;
	}
	
	protected void initGraphicFactory() {
	}
	
	public final void setGraphicFactory(final RClientGraphicFactory factory, final RClientGraphicActions actions) {
		if (factory == null) {
			throw new NullPointerException();
		}
		this.graphicFactory = factory;
		this.graphicActions = actions;
	}
	
	public boolean isClosed() {
		return this.closed;
	}
	
	public void setClosed(final boolean closed) {
		if (this.closed != closed) {
			this.closed = closed;
			if (closed) {
				final ScheduledFuture<?> job = this.keepAliveJob;
				if (job != null) {
					this.keepAliveJob = null;
					job.cancel(true);
				}
			}
		}
	}
	
	public void setRjsProperties(final Map<String, ? extends Object> properties) throws CoreException {
		try {
			this.rjConsoleServer.setProperties(properties);
		}
		catch (final RemoteException e) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, "An error occurred when setting server properties.", e));
		}
	}
	
	
	public final void processMainCmd(final ObjectInput in) throws IOException {
		boolean runGC = false;
		updateBusy(in.readBoolean());
		if (this.hotModeState == 4) {
			this.hotModeState = 0;
			this.consoleReadCallback = this.hotModeReadCallbackBackup;
			this.hotModeReadCallbackBackup = null;
			if (this.hotModeC2SFirstBackup != null) {
				addC2SCmd(this.hotModeC2SFirstBackup);
				this.hotModeC2SFirstBackup = null;
			}
		}
		
		this.mainIO.in = in;
		while (true) {
			final byte type = in.readByte();
			switch (type) {
			case MainCmdItem.T_NONE:
				this.mainRunGC = runGC;
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				processPrompt(new ConsoleReadCmdItem(this.mainIO));
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				runGC = true;
				writeStdOutput(this.mainIO.readString());
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_ERR_ITEM:
				runGC = true;
				writeErrOutput(this.mainIO.readString());
				continue;
			case MainCmdItem.T_MESSAGE_ITEM:
				runGC = true;
				showMessage(this.mainIO.readString());
				continue;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				runGC = true;
				processUICallback(this.mainIO);
				continue;
			case MainCmdItem.T_GRAPH_ITEM:
				runGC = true;
				processGDCmd(this.mainIO);
				continue;
			case MainCmdItem.T_DATA_ITEM:
				runGC = true;
				processDataCmd(this.mainIO);
				continue;
			default:
				throw new IOException("Unknown cmdtype id: " + type);
			}
		}
	}
	
	
	public final boolean processUICallback(final RJIO io) throws IOException {
		final ExtUICmdItem item = new ExtUICmdItem(io);
		try {
			final RList answer = handleUICallback(item.getDataText(), item.getDataArgs(),
					this.progressMonitor);
			item.setAnswer(answer);
		}
		catch (final IOException e) {
			throw e;
		}
		catch (final Exception e) {
			log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when exec RJ UI command '" + item.getDataText() + "'.", e)); 
			if (item.waitForClient()) {
				item.setAnswer(new RjsStatus(RjsStatus.ERROR, 0, "Client error processing current command."));
			}
		}
		if (item.waitForClient()) {
			addC2SCmd(item);
			return true;
		}
		return false;
	}
	
	protected RList handleUICallback(final String commandId, final RList args,
			final IProgressMonitor monitor) throws Exception {
		throw new CoreException(new Status(IStatus.WARNING, RJ_CLIENT_ID, -1, "Unhandled RJ UI command '" + commandId + "'.", null)); 
	}
	
	
	public final void processGDCmd(final RJIO io) throws IOException {
		byte requestId = 0;
		final int options = io.in.readInt();
		final int devId = io.in.readInt();
		try {
			int n; 
			switch (io.in.readByte()) {
			case GDCmdItem.C_NEW_PAGE:
				addGraphic(devId,
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readBoolean());
				return;
			case GDCmdItem.C_CLOSE_DEVICE:
				removeGraphic(devId);
				return;
			case GDCmdItem.C_GET_SIZE:
				addC2SCmd(new GDCmdItem.Answer(requestId = io.in.readByte(),
						devId, getGraphic(devId).computeSize() ));
				return;
			case GDCmdItem.C_SET_ACTIVE_OFF:
				getGraphic(devId).setActive(false);
				return;
			case GDCmdItem.C_SET_ACTIVE_ON:
				getGraphic(devId).setActive(true);
				return;
			case GDCmdItem.C_SET_MODE:
				getGraphic(devId).setMode(io.in.readByte());
				return;
			case GDCmdItem.C_GET_FONTMETRIC:
				addC2SCmd(new GDCmdItem.Answer(requestId = io.in.readByte(),
						devId, getGraphic(devId).computeFontMetric(
								io.in.readInt() )));
				return;
			case GDCmdItem.C_GET_STRINGWIDTH:
				addC2SCmd(new GDCmdItem.Answer(requestId = io.in.readByte(),
						devId, getGraphic(devId).computeStringWidth(
								io.readString() )));
				return;
				
			case GDCmdItem.SET_CLIP:
				getGraphic(devId).addSetClip(
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble() );
				return;
			case GDCmdItem.SET_COLOR:
				getGraphic(devId).addSetColor(
						io.in.readInt() );
				return;
			case GDCmdItem.SET_FILL:
				getGraphic(devId).addSetFill(
						io.in.readInt() );
				return;
			case GDCmdItem.SET_LINE:
				getGraphic(devId).addSetLine(
						io.in.readInt(),
						io.in.readDouble() );
				return;
			case GDCmdItem.SET_FONT:
				getGraphic(devId).addSetFont(
						io.readString(),
						io.in.readInt(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble() );
				return;
				
			case GDCmdItem.DRAW_LINE:
				getGraphic(devId).addDrawLine(
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble() );
				return;
			case GDCmdItem.DRAW_RECTANGLE:
				getGraphic(devId).addDrawRect(
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble() );
				return;
			case GDCmdItem.DRAW_POLYLINE:
				n = io.in.readInt();
				getGraphic(devId).addDrawPolyline(
						readDouble(io.in, n),
						readDouble(io.in, n) );
				return;
			case GDCmdItem.DRAW_POLYGON:
				n = io.in.readInt();
				getGraphic(devId).addDrawPolygon(
						readDouble(io.in, n),
						readDouble(io.in, n) );
				return;
			case GDCmdItem.DRAW_CIRCLE:
				getGraphic(devId).addDrawCircle(
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble() );
				return;
			case GDCmdItem.DRAW_TEXT:
				getGraphic(devId).addDrawText(
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.in.readDouble(),
						io.readString() );
				return;
			default:
				throw new UnsupportedOperationException("Unknown GD command.");
			}
		}
		catch (final IOException e) {
			throw e;
		}
		catch (final Exception e) {
			log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when processing graphic command.", e));
			if ((options & MainCmdItem.OV_WAITFORCLIENT) != 0) {
				if (requestId > 0) {
					addC2SCmd(new GDCmdItem.Answer(requestId, devId, new RjsStatus()));
				}
				else {
					throw new IllegalStateException();
				}
			}
			else {
				return;
			}
		}
	}
	
	
	public final void processDataCmd(final RJIO io) throws IOException {
		try {
			final DataCmdItem item = new DataCmdItem(io);
			if (this.dataLevelRequest > 0) {
				this.dataAnswer[this.dataLevelRequest] = item;
				this.dataLevelAnswer = this.dataLevelRequest;
			}
		}
		catch (final IOException e) {
			throw e;
		}
		catch (final Exception e) {
			log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when processing data command.", e));
		}
	}
	
	private final int newDataLevel() {
		final int level = ++this.dataLevelRequest;
		if (level >= this.dataAnswer.length) {
			this.dataLevelRequest--;
			throw new UnsupportedOperationException("too much nested operations");
		}
		this.dataLevelAnswer = 0;
		return level;
	}
	
	private final void finalizeDataLevel() {
		final int level = this.dataLevelRequest--;
		this.dataAnswer[level] = null;
		this.dataLevelAnswer = (this.dataAnswer[this.dataLevelRequest] != null) ? this.dataLevelRequest : 0;
	}
	
	
	protected abstract void log(IStatus status);
	
	protected abstract void handleServerStatus(final RjsStatus serverStatus, final IProgressMonitor monitor) throws CoreException;
	
	protected abstract void handleStatus(Status status, IProgressMonitor monitor);
	
	protected void scheduleConnectionCheck() {
	}
	
	
	public final boolean runAsyncPing() {
		try {
			final RjsComObject answer = this.rjConsoleServer.runAsync(RjsPing.INSTANCE);
			if (answer instanceof RjsStatus) {
				final RjsStatus status = (RjsStatus) answer;
				if (status.getSeverity() == RjsStatus.OK) {
					return true;
				}
				scheduleConnectionCheck();
			}
		}
		catch (final ConnectException e) {
			scheduleConnectionCheck();
		}
		catch (final RemoteException e) {
			// TODO if (rmiregistry is available) scheduleCheck();
			// no need to log here
		}
		catch (final Exception e) {
		}
		return false;
	}
	
	public final boolean runAsyncInterrupt() {
		try {
			runAsyncCtrl(CtrlCmdItem.REQUEST_CANCEL);
			return true;
		}
		catch (final CoreException e) {
			if (e.getStatus().getSeverity() != IStatus.CANCEL) {
				log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when trying to interrupt R.", e));
			}
			return false;
		}
	}
	
	public final void runAsyncCtrl(final int id) throws CoreException {
		final RjsStatus status = (RjsStatus) runAsync(new CtrlCmdItem(id));
		if (status.getSeverity() != RjsStatus.OK) {
			throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
					"Executing CTRL command failed.", null));
		}
	}
	
	public final RjsComObject runAsync(final RjsComObject com) throws CoreException {
		if (this.closed) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, 0, this.closedMessage, null));
		}
		try {
			return this.rjConsoleServer.runAsync(com);
		}
		catch (final Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, 0, "Communication error.", e));
		}
	}
	
	public final void runMainLoopPing(final IProgressMonitor monitor) throws CoreException {
		try {
			this.mainRunGC = false;
			final RjsStatus status = (RjsStatus) this.rjConsoleServer.runMainLoop(RjsPing.INSTANCE);
			if (status.getSeverity() == RjsStatus.OK) {
				return;
			}
			handleServerStatus(status, monitor);
		}
		catch (final ConnectException e) {
			handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_DISCONNECTED), monitor);
		}
		catch (final Exception e) {
			// no need to log here
			handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_LOST), monitor);
		}
	}
	
	public final void runMainLoop(RjsComObject sendCom, MainCmdItem sendItem, final IProgressMonitor monitor) throws CoreException {
		if (this.closed) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, 0, this.closedMessage, null));
		}
		final boolean loopReadCallbackIgnore = !this.consoleReadCallbackRequired;
		final int loopDataLevelIgnore = this.dataLevelIgnore;
		try {
			if ((this.extraModeRequested & EXTRA_BEFORE) != 0 && this.hotModeState < 1) {
				this.extraModeRequested = 0;
				this.dataLevelIgnore = this.dataLevelRequest;
				processExtraMode(EXTRA_BEFORE);
			}
			if (this.hotModeRequested.get() && this.hotModeState < 1) {
				this.dataLevelIgnore = this.dataLevelRequest;
				startHotMode();
			}
			this.progressMonitor = monitor;
			int ok = 0;
			while (!this.closed) {
				try {
					RjsComObject receivedCom = null;
					if (sendItem != null) {
						if (sendItem.getCmdType() == MainCmdItem.T_CONSOLE_READ_ITEM) {
							this.consoleReadCallback = null;
						}
						this.mainC2SList.setObjects(sendItem);
						sendCom = this.mainC2SList;
						sendItem = null;
					}
	//				System.out.println("client *-> server: " + sendCom);
					this.mainRunGC = false;
					receivedCom = this.rjConsoleServer.runMainLoop(sendCom);
					this.mainC2SList.clear();
					sendCom = null;
	//				System.out.println("client *<- server: " + receivedCom);
					switch (receivedCom.getComType()) {
					case RjsComObject.T_PING:
						ok = 0;
						sendCom = RjsStatus.OK_STATUS;
						continue;
					case RjsComObject.T_MAIN_LIST:
						ok = 0;
						switch (this.hotModeState) {
						case 1:
							sendCom = this.mainC2SList; // repeat
							continue;
						case 2:
							this.hotModeState = 3;
							this.hotModeRequested.set(false);
							this.dataLevelIgnore = this.dataLevelRequest;
							try {
								processHotMode();
								continue;
							}
							finally {
								this.progressMonitor = monitor;
								this.hotModeState = 4;
								this.consoleReadCallback.setAnswer(RjsStatus.OK_STATUS);
								sendItem = this.consoleReadCallback;
							}
						default:
							while ((sendItem = getC2SCmds()) == null
									&& (loopReadCallbackIgnore || this.consoleReadCallback != null)
									&& (this.dataLevelRequest <= loopDataLevelIgnore
											|| this.dataLevelRequest == this.dataLevelAnswer
											|| (this.extraModeRequested & EXTRA_NESTED) != 0 ) ) {
								if (this.mainRunGC) {
									this.mainRunGC = false;
									this.rjConsoleServer.runMainLoop(RjsPing.INSTANCE);
								}
								if ((this.extraModeRequested & EXTRA_NESTED) != 0 && this.hotModeState < 1) {
									this.extraModeRequested = 0;
									this.dataLevelIgnore = this.dataLevelRequest;
									try {
										processExtraMode(EXTRA_NESTED);
										continue; // validate again
									}
									finally {
										this.progressMonitor = monitor;
									}
								}
								else {
									return; // finished
								}
							}
							continue;
						}
					case RjsComObject.T_STATUS:
						ok = 0;
						processStatus((RjsStatus) receivedCom, monitor);
						sendCom = this.mainC2SList;
						continue;
					}
				}
				catch (final ConnectException e) {
					handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_DISCONNECTED), monitor);
					return;
				}
				catch (final RemoteException e) {
					log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "Communication error detail. Send:\n"+sendCom, e));
					if (!this.closed && runAsyncPing()) { // async to avoid server gc
						if (this.consoleReadCallback == null && ok == 0) {
							ok++;
							handleStatus(new Status(IStatus.ERROR, RJ_CLIENT_ID, "Communication error, see Eclipse log for detail."), monitor);
							continue;
						}
						throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "Communication error.", e));
					}
					handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_LOST), monitor);
					return;
				}
			}
			
			handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_DISCONNECTED), monitor);
		}
		finally {
			this.dataLevelIgnore = loopDataLevelIgnore;
		}
	}
	
	private final double[] readDouble(final ObjectInput in, final int n) throws IOException {
		final double[] data = new double[n];
		for (int i = 0; i < n; i++) {
			data[i] = in.readDouble();
		}
		return data;
	}
	
	
	protected final void addC2SCmd(final MainCmdItem item) {
		if (this.mainC2SFirst == null) {
			this.mainC2SFirst = item;
		}
		else {
			item.next = this.mainC2SFirst;
			this.mainC2SFirst = item;
			
			// TODO
			log(new Status(IStatus.INFO, RJ_CLIENT_ID, "Multiple C2S items:\\" + this.mainC2SFirst.toString() + "\n" + this.mainC2SFirst.next.toString()));
		}
	}
	
	private final MainCmdItem getC2SCmds() {
		final MainCmdItem item = this.mainC2SFirst;
		this.mainC2SFirst = null;
		return item;
	}
	
	private final void processStatus(final RjsStatus status, final IProgressMonitor monitor)
			throws CoreException {
		if ((status.getCode() & 0xffffff00) == 0) {
			handleServerStatus(status, monitor);
			return;
		}
		if (status.getSeverity() != RjsStatus.OK) {
			// TODO
//			System.out.println(status);
		}
	}
	
	private final void processPrompt(final ConsoleReadCmdItem item) {
		switch (item.getCmdOption() & 0xf) {
		case 2:
			if (this.hotModeState < 2) {
				this.hotModeState = 2;
				this.hotModeReadCallbackBackup = this.consoleReadCallback;
				this.hotModeC2SFirstBackup = this.mainC2SFirst;
				this.mainC2SFirst = null;
			}
			this.consoleReadCallback = item;
			return;
		case RjsComObject.V_TRUE:
			this.consoleReadCallback = item;
			updatePrompt(item.getDataText(), true);
			return;
		default:
			this.consoleReadCallback = item;
			updatePrompt(item.getDataText(), false);
			return;
		}
	}
	
	public void requestHotMode(final boolean async) {
		this.hotModeRequested.set(true);
		if (async) {
			RJHelper_EXECUTOR.schedule(this.hotModeRunnable, 100, TimeUnit.MILLISECONDS);
		}
	}
	
	public boolean startHotMode() {
		if (this.hotModeState == 0) {
			this.hotModeRequested.set(false);
			final boolean savedCallbackRequired = this.consoleReadCallbackRequired;
			this.consoleReadCallbackRequired = false;
			try {
				this.hotModeState = 1;
				runMainLoop(new CtrlCmdItem(CtrlCmdItem.REQUEST_HOT_MODE), null, new NullProgressMonitor());
				return true;
			}
			catch (final Throwable e) {
				this.hotModeState = 0;
				log(new Status(IStatus.ERROR, RJ_CLIENT_ID, 0,
						"An error occurred when running hot mode.", e ));
			}
			finally {
				this.consoleReadCallbackRequired = savedCallbackRequired;
			}
		}
		return false;
	}
	
	public void requestExtraMode(final int positions) {
		this.extraModeRequested = positions;
	}
	
	
	protected void updateBusy(final boolean isBusy) {
	}
	
	protected void updatePrompt(final String text, final boolean addToHistory) {
	}
	
	protected void writeStdOutput(final String text) {
	}
	
	protected void writeErrOutput(final String text) {
	}
	
	protected void showMessage(final String text) {
	}
	
	protected void processHotMode() {
	}
	
	protected void processExtraMode(final int i) {
	}
	
	private void addGraphic(final int devId, final double w, final double h, final boolean activate) throws RjException {
		if (devId >= 0) {
			if (devId >= this.graphics.length) {
				final RClientGraphic[] newArray = new RClientGraphic[devId + 10];
				System.arraycopy(this.graphics, 0, newArray, 0, this.graphics.length);
				this.graphics = newArray;
			}
			if (this.graphics[devId] != null) {
				this.graphics[devId].reset(w, h);
				this.graphics[devId].setActive(activate);
			}
			else {
				this.graphics[devId] = this.lastGraphic = this.graphicFactory.newGraphic(devId, w, h,
						activate, this.graphicActions, this.currentGraphicOptions);
			}
			return;
		}
		throw new RjException("Invalid GD devId: " + devId);
	}
	
	private void removeGraphic(final int devId) {
		if (devId >= 0 && devId < this.graphics.length) {
			if (this.graphics[devId] != null) {
				this.graphicFactory.closeGraphic(this.graphics[devId]);
				this.graphics[devId] = null;
			}
		}
	}
	
	protected RClientGraphic getGraphic(final int devId) throws RjException {
		if (devId >= 0 && devId < this.graphics.length) {
			final RClientGraphic graphic = this.graphics[devId];
			if (graphic != null) {
				return graphic;
			}
		}
		return this.graphicDummy;
	}
	
	public void disposeAllGraphics() {
		for (int devId = 0; devId < this.graphics.length; devId++) {
			try {
				removeGraphic(devId);
			}
			catch (final Exception e) {
				log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when disposing open R graphics.", e));
			}
		}
	}
	
	
	public final void activateConsole() {
		if (this.rjConsoleServer == null) {
			throw new IllegalStateException("Missing ConsoleEngine.");
		}
		this.consoleReadCallbackRequired = true;
	}
	
	public final void answerConsole(final String input, final IProgressMonitor monitor) throws CoreException {
		this.consoleReadCallback.setAnswer(input);
		runMainLoop(null, this.consoleReadCallback, monitor);
	}
	
	public final boolean isConsoleReady() {
		return (this.consoleReadCallback != null);
	}
	
	
	public final RPlatform getRPlatform() {
		synchronized (this.platformLock) {
			if (this.platformObj == null) {
				try {
					if (this.platformData == null) {
						this.platformData = this.rjConsoleServer.getPlatformData();
					}
					this.platformObj = new RPlatform((String) this.platformData.get("os.type"),
							(String) this.platformData.get("file.sep"), (String) this.platformData.get("path.sep"),
							(String) this.platformData.get("version.string") );
				}
				catch (final RemoteException e) {
					log(new Status(IStatus.ERROR, RJ_CLIENT_ID,
							"An error occured when loading data for RPlatform information.", e));
				}
			}
			return this.platformObj;
		}
	}
	
	public final void evalVoid(final String command, final IProgressMonitor monitor) throws CoreException {
		final int level = newDataLevel();
		try {
			runMainLoop(null, new DataCmdItem(DataCmdItem.EVAL_VOID, 0, (byte) 0, command, null), monitor);
			if (this.dataAnswer[level] == null || !this.dataAnswer[level].isOK()) {
				final RjsStatus status = (this.dataAnswer[level] != null) ? this.dataAnswer[level].getStatus() : MISSING_ANSWER_STATUS;
				if (status.getSeverity() == RjsStatus.CANCEL) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				else {
					throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
							"Evaluation failed: " + status.getMessage(), null));
				}
			}
			return;
		}
		finally {
			finalizeDataLevel();
		}
	}
	
	public RObject evalData(final String command, final String factoryId,
			final int options, final int depth, final IProgressMonitor monitor) throws CoreException {
		final byte checkedDepth = (depth < Byte.MAX_VALUE) ? (byte) depth : Byte.MAX_VALUE;
		final int level = newDataLevel();
		try {
			runMainLoop(null, new DataCmdItem(DataCmdItem.EVAL_DATA, options, checkedDepth, command, factoryId), monitor);
			if (this.dataAnswer[level] == null || !this.dataAnswer[level].isOK()) {
				final RjsStatus status = (this.dataAnswer[level] != null) ? this.dataAnswer[level].getStatus() : MISSING_ANSWER_STATUS;
				if (status.getSeverity() == RjsStatus.CANCEL) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				else {
					throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
							"Evaluation failed: " + status.getMessage(), null));
				}
			}
			return this.dataAnswer[level].getData();
		}
		finally {
			finalizeDataLevel();
		}
	}
	
	public final RObject evalData(final RReference reference, final String factoryId,
			final int options, final int depth, final IProgressMonitor monitor) throws CoreException {
		final byte checkedDepth = (depth < Byte.MAX_VALUE) ? (byte) depth : Byte.MAX_VALUE;
		final int level = newDataLevel();
		try {
			final long handle = reference.getHandle();
			runMainLoop(null, new DataCmdItem(DataCmdItem.RESOLVE_DATA, options, checkedDepth, Long.toString(handle), factoryId), monitor);
			if (this.dataAnswer[level] == null || !this.dataAnswer[level].isOK()) {
				final RjsStatus status = (this.dataAnswer[level] != null) ? this.dataAnswer[level].getStatus() : MISSING_ANSWER_STATUS;
				if (status.getSeverity() == RjsStatus.CANCEL) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				else {
					throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
							"Evaluation failed: " + status.getMessage(), null));
				}
			}
			return this.dataAnswer[level].getData();
		}
		finally {
			finalizeDataLevel();
		}
	}
	
	public final void assignData(final String expression, final RObject data, final IProgressMonitor monitor) throws CoreException {
		final int level = newDataLevel();
		try {
			runMainLoop(null, new DataCmdItem(DataCmdItem.ASSIGN_DATA, 0, expression, data), monitor);
			if (this.dataAnswer[level] == null || !this.dataAnswer[level].isOK()) {
				final RjsStatus status = (this.dataAnswer[level] != null) ? this.dataAnswer[level].getStatus() : MISSING_ANSWER_STATUS;
				if (status.getSeverity() == RjsStatus.CANCEL) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				else {
					throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
							"Assignment failed: " + status.getMessage(), null));
				}
			}
			return;
		}
		finally {
			finalizeDataLevel();
		}
	}
	
	
	public void downloadFile(final OutputStream out, final String fileName, final int options, final IProgressMonitor monitor) throws CoreException {
		final BinExchange request = new BinExchange(out, fileName, this.rjConsoleServer, options);
		final BinExchange answer;
		try {
			answer = (BinExchange) runAsync(request);
		}
		finally {
			request.clear();
		}
		if (answer == null || !answer.isOK()) {
			final RjsStatus status = (answer != null) ? answer.getStatus() : MISSING_ANSWER_STATUS;
			if (status.getSeverity() == RjsStatus.CANCEL) {
				throw new CoreException(Status.CANCEL_STATUS);
			}
			else {
				throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
						"Downloading file failed: " + status.getMessage(), null));
			}
		}
	}
	
	public byte[] downloadFile(final String fileName, final int options, final IProgressMonitor monitor) throws CoreException {
		final BinExchange request = new BinExchange(fileName, this.rjConsoleServer, options);
		final BinExchange answer;
		try {
			answer = (BinExchange) runAsync(request);
		}
		finally {
			request.clear();
		}
		if (answer == null || !answer.isOK()) {
			final RjsStatus status = (answer != null) ? answer.getStatus() : MISSING_ANSWER_STATUS;
			if (status.getSeverity() == RjsStatus.CANCEL) {
				throw new CoreException(Status.CANCEL_STATUS);
			}
			else {
				throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
						"Downloading file failed: " + status.getMessage(), null));
			}
		}
		return answer.getBytes();
	}
	
	public void uploadFile(final InputStream in, final long length, final String fileName, final int options, final IProgressMonitor monitor) throws CoreException {
		final BinExchange request = new BinExchange(in, length, fileName, this.rjConsoleServer, options);
		final BinExchange answer;
		try {
			answer = (BinExchange) runAsync(request);
		}
		finally {
			request.clear();
		}
		if (answer == null || !answer.isOK()) {
			final RjsStatus status = (answer != null) ? answer.getStatus() : MISSING_ANSWER_STATUS;
			if (status.getSeverity() == RjsStatus.CANCEL) {
				throw new CoreException(Status.CANCEL_STATUS);
			}
			else {
				throw new CoreException(new Status(status.getSeverity(), RJ_CLIENT_ID, status.getCode(),
						"Uploading file failed: " + status.getMessage(), null));
			}
		}
	}
	
	public int getGraphicOptions() {
		return this.currentGraphicOptions;
	}
	
	public void setGraphicOptions(final int options) {
		this.currentGraphicOptions = options;
		this.lastGraphic = null;
	}
	
	public RClientGraphic getLastGraphic() {
		return this.lastGraphic;
	}
	
}
