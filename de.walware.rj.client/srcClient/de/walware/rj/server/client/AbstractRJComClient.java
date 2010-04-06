/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/RJ-Project (www.walware.de/goto/opensource).
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.walware.rj.RjException;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RReference;
import de.walware.rj.server.BinExchange;
import de.walware.rj.server.ComHandler;
import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.ConsoleReadCmdItem;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsPing;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.services.RService;


/**
 * Generic RJ Com protocol client for servers offering a {@link ConsoleEngine}.
 * <p>
 * Offers basic implementation for most methods of the {@link RService} API,
 * including:</p>
 * <ul>
 *   <li>Expression evaluation</li>
 *   <li>Data exchange to assign or eval/read {@link RObject}</li>
 *   <li>File exchange to write/read file on the server (must be enabled at server side)</li>
 *   <li>R graphics (requires an {@link RClientGraphicFactory}, set in {@link #initGraphicFactory()},
 *       or via {@link #setGraphicFactory(RClientGraphicFactory, RClientGraphicActions)})</li>
 *   <li>Console (REPL), if connected to server slot 0</li>
 * </ul>
 */
public abstract class AbstractRJComClient implements ComHandler {
	
	
	public static final String RJ_CLIENT_ID = "de.walware.rj.client";
	
	public static int[] version() {
		return new int[] { 0, 5, 0 };
	}
	
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
	
	
	private IProgressMonitor progressMonitor;
	
	private MainCmdItem mainC2SFirst;
	private final MainCmdC2SList mainC2SList = new MainCmdC2SList();
	private boolean mainRunGC;
	
	private boolean consoleReadCallbackRequired;
	private ConsoleReadCmdItem consoleReadCallback;
	
	private int dataLevelRequest = 0;
	private int dataLevelAnswer = 0;
	private final DataCmdItem[] dataAnswer = new DataCmdItem[16];
	
	private RClientGraphicFactory graphicFactory;
	private RClientGraphicActions graphicActions;
	private final RClientGraphic graphicDummy = new RClientGraphicDummy(1, 0, 0);
	private RClientGraphic[] graphics = new RClientGraphic[16];
	private int currentGraphicOptions;
	private RClientGraphic lastGraphic;
	
	private ConsoleEngine rjConsoleServer;
	
	private boolean closed;
	private String closedMessage = "Connection to R engine is closed.";
	
	
	protected AbstractRJComClient() {
		this.graphicFactory = new LazyGraphicFactory();
	}
	
	
	public final void setServer(final ConsoleEngine rjServer) {
		this.rjConsoleServer = rjServer;
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
		this.closed = closed;
	}
	
	
	public final void processMainCmd(final ObjectInput in) throws IOException {
		boolean runGC = false;
		updateBusy(in.readBoolean());
		
		int num = -1;
		while (true) {	// first
			num++;
			final byte type = in.readByte();
			switch (type) {
			case MainCmdItem.T_NONE:
				this.mainRunGC = runGC;
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				this.consoleReadCallback = new ConsoleReadCmdItem(in);
				updatePrompt(this.consoleReadCallback.getDataText(), (this.consoleReadCallback.getCmdOption() & 0xf) == RjsComObject.V_TRUE);
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				runGC = true;
				writeStdOutput(in.readUTF());
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_ERR_ITEM:
				runGC = true;
				writeErrOutput(in.readUTF());
				continue;
			case MainCmdItem.T_MESSAGE_ITEM:
				runGC = true;
				showMessage(in.readUTF());
				continue;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				runGC = true;
				processUICallback(in);
				continue;
			case MainCmdItem.T_GRAPH_ITEM:
				runGC = true;
				processGDCmd(in);
				continue;
			case MainCmdItem.T_DATA_ITEM:
				runGC = true;
				processDataCmd(in);
				continue;
			default:
				throw new IOException("Unknown cmdtype id: " + type);
			}
		}
	}
	
	
	public final boolean processUICallback(final ObjectInput in) throws IOException {
		final ExtUICmdItem item = new ExtUICmdItem(in);
		try {
			handleUICallback(item, this.progressMonitor);
		}
		catch (final IOException e) {
			throw e;
		}
		catch (final Exception e) {
			log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when exec RJ UI command '" + item.getCommand() + "'.", e)); 
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
	
	protected void handleUICallback(final ExtUICmdItem item, final IProgressMonitor monitor) throws Exception {
		log(new Status(IStatus.WARNING, RJ_CLIENT_ID, -1, "Unhandled RJ UI command '" + item.getCommand() + "'.", null)); 
		if (item.waitForClient()) {
			item.setAnswer(new RjsStatus(RjsStatus.ERROR, 0, "Client error processing current command."));
		}
	}
	
	
	public final void processGDCmd(final ObjectInput in) throws IOException {
		byte requestId = 0;
		final int options = in.readInt();
		final int devId = in.readInt();
		try {
			int n; 
			switch (in.readByte()) {
			case GDCmdItem.C_NEW_PAGE:
				addGraphic(devId,
						in.readDouble(),
						in.readDouble(),
						in.readBoolean());
				return;
			case GDCmdItem.C_CLOSE_DEVICE:
				removeGraphic(devId);
				return;
			case GDCmdItem.C_GET_SIZE:
				addC2SCmd(new GDCmdItem.Answer(requestId = in.readByte(),
						devId, getGraphic(devId).computeSize() ));
				return;
			case GDCmdItem.C_SET_ACTIVE_OFF:
				getGraphic(devId).setActive(false);
				return;
			case GDCmdItem.C_SET_ACTIVE_ON:
				getGraphic(devId).setActive(true);
				return;
			case GDCmdItem.C_SET_MODE:
				getGraphic(devId).setMode(in.readByte());
				return;
			case GDCmdItem.C_GET_FONTMETRIC:
				addC2SCmd(new GDCmdItem.Answer(requestId = in.readByte(),
						devId, getGraphic(devId).computeFontMetric(
								in.readInt() )));
				return;
			case GDCmdItem.C_GET_STRINGWIDTH:
				addC2SCmd(new GDCmdItem.Answer(requestId = in.readByte(),
						devId, getGraphic(devId).computeStringWidth(
								in.readUTF() )));
				return;
				
			case GDCmdItem.SET_CLIP:
				getGraphic(devId).addSetClip(
						in.readDouble(),
						in.readDouble(),
						in.readDouble(),
						in.readDouble() );
				return;
			case GDCmdItem.SET_COLOR:
				getGraphic(devId).addSetColor(
						in.readInt() );
				return;
			case GDCmdItem.SET_FILL:
				getGraphic(devId).addSetFill(
						in.readInt() );
				return;
			case GDCmdItem.SET_LINE:
				getGraphic(devId).addSetLine(
						in.readInt(),
						in.readDouble() );
				return;
			case GDCmdItem.SET_FONT:
				getGraphic(devId).addSetFont(
						in.readUTF(),
						in.readInt(),
						in.readDouble(),
						in.readDouble(),
						in.readDouble() );
				return;
				
			case GDCmdItem.DRAW_LINE:
				getGraphic(devId).addDrawLine(
						in.readDouble(),
						in.readDouble(),
						in.readDouble(),
						in.readDouble() );
				return;
			case GDCmdItem.DRAW_RECTANGLE:
				getGraphic(devId).addDrawRect(
						in.readDouble(),
						in.readDouble(),
						in.readDouble(),
						in.readDouble() );
				return;
			case GDCmdItem.DRAW_POLYLINE:
				n = in.readInt();
				getGraphic(devId).addDrawPolyline(
						readDouble(in, n),
						readDouble(in, n) );
				return;
			case GDCmdItem.DRAW_POLYGON:
				n = in.readInt();
				getGraphic(devId).addDrawPolygon(
						readDouble(in, n),
						readDouble(in, n) );
				return;
			case GDCmdItem.DRAW_CIRCLE:
				getGraphic(devId).addDrawCircle(
						in.readDouble(),
						in.readDouble(),
						in.readDouble() );
				return;
			case GDCmdItem.DRAW_TEXT:
				getGraphic(devId).addDrawText(
						in.readDouble(),
						in.readDouble(),
						in.readDouble(),
						in.readDouble(),
						in.readUTF() );
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
			if ((options & GDCmdItem.OV_WAITFORCLIENT) != 0) {
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
	
	
	public final void processDataCmd(final ObjectInput in) throws IOException {
		try {
			final DataCmdItem item = new DataCmdItem(in);
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
		this.dataLevelAnswer = 0;
	}
	
	
	protected abstract void log(IStatus status);
	
	protected abstract void handleServerStatus(final RjsStatus serverStatus, final IProgressMonitor monitor) throws CoreException;
	
	protected abstract void handleStatus(Status status, IProgressMonitor monitor);
	
	
	public final boolean runAsyncPing() {
		try {
			return (RjsStatus.OK_STATUS.equals(this.rjConsoleServer.runAsync(RjsPing.INSTANCE)));
		}
		catch (final RemoteException e) {
			// no need to log here
			return false;
		}
	}
	
	public final void runAsyncInterrupt() {
		try {
			this.rjConsoleServer.interrupt();
		}
		catch (final RemoteException e) {
			log(new Status(IStatus.ERROR, RJ_CLIENT_ID, -1, "An error occurred when trying to interrupt R.", e));
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
		catch (final Exception e) {
			// no need to log here
			handleServerStatus(new RjsStatus(RjsComObject.V_INFO, Server.S_LOST), monitor);
		}
	}
	
	public final void runMainLoop(RjsComObject sendCom, MainCmdItem sendItem, final IProgressMonitor monitor) throws CoreException {
		if (this.closed) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID, 0, this.closedMessage, null));
		}
		this.progressMonitor = monitor;
		int ok = 0;
		while (true) {
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
					sendCom = RjsStatus.OK_STATUS;
					ok = 0;
					continue;
				case RjsComObject.T_MAIN_LIST:
					sendItem = getC2SCmds();
					ok = 0;
					if (sendItem == null
							&& (!this.consoleReadCallbackRequired || this.consoleReadCallback != null)
							&& (this.dataLevelRequest == this.dataLevelAnswer) ) {
						if (this.mainRunGC) {
							this.mainRunGC = false;
							this.rjConsoleServer.runMainLoop(RjsPing.INSTANCE);
						}
						return;
					}
					continue;
				case RjsComObject.T_STATUS:
					handleServerStatus((RjsStatus) receivedCom, monitor);
					ok = 0;
					return;
				}
			}
			catch (final ConnectException e) {
				handleServerStatus(new RjsStatus(RjsStatus.INFO, Server.S_DISCONNECTED), monitor);
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
				handleServerStatus(new RjsStatus(RjsComObject.V_INFO, Server.S_LOST), monitor);
			}
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
		}
	}
	
	private final MainCmdItem getC2SCmds() {
		final MainCmdItem item = this.mainC2SFirst;
		this.mainC2SFirst = null;
		return item;
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
			runMainLoop(null, new DataCmdItem(((options & RObjectFactory.F_ONLY_STRUCT) == RObjectFactory.F_ONLY_STRUCT) ?
					DataCmdItem.EVAL_STRUCT : DataCmdItem.EVAL_DATA, 0, checkedDepth, command, factoryId), monitor);
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
			runMainLoop(null, new DataCmdItem(((options & RObjectFactory.F_ONLY_STRUCT) == RObjectFactory.F_ONLY_STRUCT) ?
					DataCmdItem.RESOLVE_STRUCT : DataCmdItem.RESOLVE_DATA, 0, checkedDepth, Long.toString(handle), factoryId), monitor);
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
