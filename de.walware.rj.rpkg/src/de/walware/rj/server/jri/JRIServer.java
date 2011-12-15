/*******************************************************************************
 * Copyright (c) 2008-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import static de.walware.rj.server.RjsComObject.V_ERROR;
import static de.walware.rj.server.RjsComObject.V_FALSE;
import static de.walware.rj.server.RjsComObject.V_OK;
import static de.walware.rj.server.RjsComObject.V_TRUE;
import static de.walware.rj.server.Server.S_CONNECTED;
import static de.walware.rj.server.Server.S_DISCONNECTED;
import static de.walware.rj.server.Server.S_NOT_STARTED;
import static de.walware.rj.server.Server.S_STOPPED;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_CTRL_COMMON;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_CTRL_REQUEST_CANCEL;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_CTRL_REQUEST_HOT_MODE;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_ASSIGN_DATA;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_COMMON;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_EVAL_DATA;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DATA_RESOLVE_DATA;
import static de.walware.rj.server.jri.JRIServerErrors.CODE_DBG_COMMON;
import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;
import static de.walware.rj.server.jri.JRIServerRni.EVAL_MODE_DEFAULT;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.rosuda.JRI.RConfig;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

import de.walware.rj.RjException;
import de.walware.rj.RjInitFailedException;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.ConsoleMessageCmdItem;
import de.walware.rj.server.ConsoleReadCmdItem;
import de.walware.rj.server.ConsoleWriteErrCmdItem;
import de.walware.rj.server.ConsoleWriteOutCmdItem;
import de.walware.rj.server.CtrlCmdItem;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.DbgCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.GraOpCmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdS2CList;
import de.walware.rj.server.RJ;
import de.walware.rj.server.RjsComConfig;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsException;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.server.dbg.DbgEnablement;
import de.walware.rj.server.dbg.DbgFilterState;
import de.walware.rj.server.dbg.ElementTracepointInstallationRequest;
import de.walware.rj.server.dbg.FrameContextDetailRequest;
import de.walware.rj.server.dbg.SetDebugRequest;
import de.walware.rj.server.dbg.TracepointEvent;
import de.walware.rj.server.dbg.TracepointListener;
import de.walware.rj.server.dbg.TracepointStatesUpdate;
import de.walware.rj.server.gd.Coord;
import de.walware.rj.server.gd.GraOp;
import de.walware.rj.server.srvImpl.AbstractServerControl;
import de.walware.rj.server.srvImpl.ConsoleEngineImpl;
import de.walware.rj.server.srvImpl.DefaultServerImpl;
import de.walware.rj.server.srvImpl.InternalEngine;
import de.walware.rj.server.srvImpl.RJClassLoader;
import de.walware.rj.server.srvext.Client;
import de.walware.rj.server.srvext.ExtServer;
import de.walware.rj.server.srvext.RjsGraphic;
import de.walware.rj.server.srvext.ServerRuntimePlugin;
import de.walware.rj.server.srvext.ServerUtil;


/**
 * Remove server based on
 */
public final class JRIServer extends RJ
		implements InternalEngine, RMainLoopCallbacks, ExtServer, TracepointListener {
	
	
	private static final int ENGINE_NOT_STARTED = 0;
	private static final int ENGINE_RUN_IN_R = 1;
	private static final int ENGINE_WAIT_FOR_CLIENT = 2;
	private static final int ENGINE_STOPPED = 4;
	
	private static final int CLIENT_NONE = 0;
	private static final int CLIENT_OK = 1;
	private static final int CLIENT_OK_WAIT = 2;
	private static final int CLIENT_CANCEL = 3;
	
	private static final long STALE_SPAN = 5L * 60L * 1000000000L;
	
	private static final int KILO = 1024;
	private static final int MEGA = 1048576;
	private static final int GIGA = 1073741824;
	
	private static final int STDOUT_BUFFER_SIZE = 0x2000;
	
	private static final long REQUIRED_JRI_API = 0x010a;
	
	
	private static long s2long(final String s, final long defaultValue) {
		if (s != null && s.length() > 0) {
			final int multi;
			switch (s.charAt(s.length()-1)) {
			case 'G':
				multi = GIGA;
				break;
			case 'M':
				multi = MEGA;
				break;
			case 'K':
				multi = KILO;
				break;
			case 'k':
				multi = 1000;
				break;
			default:
				multi = 1;
				break;
			}
			try {
				if (multi != 1) {
					return Long.parseLong(s.substring(0, s.length()-1)) * multi;
				}
				else {
					return Long.parseLong(s);
				}
			}
			catch (final NumberFormatException e) {}
		}
		return defaultValue;
	}
	
	
	private class InitCallbacks implements RMainLoopCallbacks {
		public String rReadConsole(final Rengine re, final String prompt, final int addToHistory) {
			initEngine(re);
			return JRIServer.this.rReadConsole(re, prompt, addToHistory);
		}
		public void rWriteConsole(final Rengine re, final String text, final int oType) {
			initEngine(re);
			JRIServer.this.rWriteConsole(re, text, oType);
		}
		public void rFlushConsole(final Rengine re) {
			initEngine(re);
			JRIServer.this.rFlushConsole(re);
		}
		public void rBusy(final Rengine re, final int which) {
			initEngine(re);
			JRIServer.this.rBusy(re, which);
		}
		public void rShowMessage(final Rengine re, final String message) {
			initEngine(re);
			JRIServer.this.rShowMessage(re, message);
		}
		public String rChooseFile(final Rengine re, final int newFile) {
			initEngine(re);
			return JRIServer.this.rChooseFile(re, newFile);
		}
		public void rLoadHistory(final Rengine re, final String filename) {
			initEngine(re);
			JRIServer.this.rLoadHistory(re, filename);
		}
		public void rSaveHistory(final Rengine re, final String filename) {
			initEngine(re);
			JRIServer.this.rSaveHistory(re, filename);
		}
		public long rExecJCommand(final Rengine re, final String commandId, final long argsExpr, final int options) {
			initEngine(re);
			return JRIServer.this.rExecJCommand(re, commandId, argsExpr, options);
		}
		public void rProcessJEvents(final Rengine re) {
			JRIServer.this.mainExchangeLock.lock();
			try {
				if (JRIServer.this.hotModeRequested) {
					JRIServer.this.hotModeDelayed = true;
				}
			}
			finally {
				JRIServer.this.mainExchangeLock.unlock();
			}
		}
	}
	
	private class HotLoopCallbacks implements RMainLoopCallbacks {
		public String rReadConsole(final Rengine re, final String prompt, final int addToHistory) {
			if (prompt.startsWith("Browse")) {
				return "c\n";
			}
			return "\n";
		}
		public void rWriteConsole(final Rengine re, final String text, final int oType) {
			LOGGER.log(Level.WARNING, "HotMode - Console Output:\n" + text);
		}
		public void rFlushConsole(final Rengine re) {
			JRIServer.this.rFlushConsole(re);
		}
		public void rBusy(final Rengine re, final int which) {
			JRIServer.this.rBusy(re, which);
		}
		public void rShowMessage(final Rengine re, final String message) {
			LOGGER.log(Level.WARNING, "HotMode - Message:\n" + message);
		}
		public String rChooseFile(final Rengine re, final int newFile) {
			return null;
		}
		public void rLoadHistory(final Rengine re, final String filename) {
		}
		public void rSaveHistory(final Rengine re, final String filename) {
		}
		public long rExecJCommand(final Rengine re, final String commandId, final long argsExpr, final int options) {
			return 0;
		}
		public void rProcessJEvents(final Rengine re) {
		}
	}
	
	
	private String name;
	
	private Server publicServer;
	private RJClassLoader rClassLoader;
	private Rengine rEngine;
	private List<String> rArgs;
	private final RConfig rConfig;
	private long rMemSize;
	
	private final ReentrantLock mainExchangeLock = new ReentrantLock();
	private final Condition mainExchangeClient = this.mainExchangeLock.newCondition();
	private final Condition mainExchangeR = this.mainExchangeLock.newCondition();
	private final ReentrantLock mainInterruptLock = new ReentrantLock();
	private int mainLoopState;
	private boolean mainLoopBusyAtServer = false;
	private boolean mainLoopBusyAtClient = true;
	private int mainLoopClient0State;
	private int mainLoopClientListen;
	private int mainLoopServerStack;
	private final char[] mainLoopStdOutBuffer = new char[STDOUT_BUFFER_SIZE];
	private String mainLoopStdOutSingle;
	private int mainLoopStdOutSize;
	private final MainCmdItem[] mainLoopS2CNextCommandsFirst = new MainCmdItem[2];
	private final MainCmdItem[] mainLoopS2CNextCommandsLast = new MainCmdItem[2];
	private final MainCmdS2CList[] mainLoopS2CLastCommands = new MainCmdS2CList[] { new MainCmdS2CList(), new MainCmdS2CList() };
	private final List<MainCmdItem> mainLoopS2CRequest = new ArrayList<MainCmdItem>();
	private MainCmdItem mainLoopC2SCommandFirst;
	private int mainLoopS2CAnswerFail;
	
	private boolean safeMode;
	
	private boolean hotModeRequested;
	private boolean hotModeDelayed;
	private boolean hotMode;
	private final RMainLoopCallbacks hotModeCallbacks = new HotLoopCallbacks();
	
	private int serverState;
	
	private final ReentrantReadWriteLock[] clientLocks = new ReentrantReadWriteLock[] {
			new ReentrantReadWriteLock(), new ReentrantReadWriteLock() };
	private Client client0;
	private ConsoleEngine client0Engine;
	private ConsoleEngine client0ExpRef;
	private ConsoleEngine client0PrevExpRef;
	private volatile long client0LastPing;
	
	private final Object pluginsLock = new Object();
	private ServerRuntimePlugin[] pluginsList = new ServerRuntimePlugin[0];
	
	private final JRIServerUtils utils = new JRIServerUtils();
	
	private RObjectFactory rObjectFactory;
	
	private JRIServerGraphics graphics;
	
	private Map<String, String> platformDataCommands;
	private final Map<String, Object> platformDataValues = new HashMap<String, Object>();
	
	private int rniListsMaxLength = 10000;
	private int rniEnvsMaxLength = 10000;
	private JRIServerRni rni;
	
	private JRIServerDbg dbgSupport;
	
	
	public JRIServer() {
		this.rConfig = new RConfig();
		// default 16M, overwritten by arg --max-cssize, if set
		this.rConfig.MainCStack_Size = s2long(
				System.getProperty("de.walware.rj.rMainCStack_Size"), 16 * MEGA );
		// default true
		this.rConfig.MainCStack_SetLimit = !"false".equalsIgnoreCase(
				System.getProperty("de.walware.rj.rMainCStack_SetLimit") );
		
		this.mainLoopState = ENGINE_NOT_STARTED;
		this.mainLoopClient0State = CLIENT_NONE;
		
		this.serverState = S_NOT_STARTED;
	}
	
	
	public int[] getVersion() {
		return new int[] { 1, 0, 0 };
	}
	
	public void init(final String name, final Server publicServer, final RJClassLoader loader) throws Exception {
		if (loader == null) {
			throw new NullPointerException("loader");
		}
		this.name = name;
		this.publicServer = publicServer;
		this.rClassLoader = loader;
		
		this.platformDataCommands = new HashMap<String, String>();
		this.platformDataCommands.put("os.type", ".Platform$OS.type");
		this.platformDataCommands.put("file.sep", ".Platform$file.sep");
		this.platformDataCommands.put("path.sep", ".Platform$path.sep");
		this.platformDataCommands.put("version.string", "paste(R.version$major, R.version$minor, sep=\".\")");
	}
	
	public void addPlugin(final ServerRuntimePlugin plugin) {
		if (plugin == null) {
			throw new IllegalArgumentException();
		}
		synchronized (this.pluginsLock) {
			final int oldSize = this.pluginsList.length;
			final ServerRuntimePlugin[] newList = new ServerRuntimePlugin[oldSize + 1];
			System.arraycopy(this.pluginsList, 0, newList, 0, oldSize);
			newList[oldSize] = plugin;
			this.pluginsList= newList;
		}
	}
	
	public void removePlugin(final ServerRuntimePlugin plugin) {
		if (plugin == null) {
			return;
		}
		synchronized (this.pluginsLock) {
			final int oldSize = this.pluginsList.length;
			for (int i = 0; i < oldSize; i++) {
				if (this.pluginsList[i] == plugin) {
					final ServerRuntimePlugin[] newList = new ServerRuntimePlugin[oldSize - 1];
					System.arraycopy(this.pluginsList, 0, newList, 0, i);
					System.arraycopy(this.pluginsList, i + 1, newList, i, oldSize - i - 1);
					this.pluginsList = newList;
					return;
				}
			}
		}
	}
	
	private void handlePluginError(final ServerRuntimePlugin plugin, final Throwable error) {
		// log and disable
		LOGGER.log(Level.SEVERE, "[Plugins] An error occurred in plug-in '"+plugin.getSymbolicName()+"', plug-in will be disabled.", error);
		removePlugin(plugin);
		try {
			plugin.rjStop(V_ERROR);
		}
		catch (final Throwable stopError) {
			LOGGER.log(Level.WARNING, "[Plugins] An error occurred when trying to disable plug-in '"+plugin.getSymbolicName()+"'.", error);
		}
	}
	
	
	private void connectClient0(final Client client,
			final ConsoleEngine consoleEngine, final ConsoleEngine export) {
		disconnectClient0();
		
		this.client0 = client;
		this.client0Engine = consoleEngine;
		this.client0ExpRef = export;
		DefaultServerImpl.addClient(export);
		this.client0LastPing = System.nanoTime();
		this.serverState = S_CONNECTED;
	}
	
	private void disconnectClient0() {
		if (this.serverState >= S_CONNECTED && this.serverState < S_STOPPED) {
			this.serverState = S_DISCONNECTED;
		}
		if (this.client0PrevExpRef != null) {
			try {
				UnicastRemoteObject.unexportObject(this.client0PrevExpRef, true);
			}
			catch (final Exception e) {}
			this.client0PrevExpRef = null;
		}
		if (this.client0 != null) {
			final Client client = this.client0;
			DefaultServerImpl.removeClient(this.client0ExpRef);
			this.client0 = null;
			this.client0Engine = null;
			this.client0PrevExpRef = this.client0ExpRef;
			this.client0ExpRef = null;
			
			if (this.hotModeRequested) {
				this.hotModeRequested = false;
			}
			internalClearClient(client);
		}
	}
	
	
	private void checkClient(final Client client) throws RemoteException {
//		final String expectedClient = this.currentMainClientId;
//		final String remoteClient;
//		try {
//			remoteClient = RemoteServer.getClientHost();
//		}
//		catch (final ServerNotActiveException e) {
//			throw new IllegalStateException(e);
//		}
		if (client.slot == 0 && this.client0 != client
//				|| expectedClient == null 
//				|| !expectedClient.equals(remoteClient)
				) {
			throw new ConnectException("Not connected.");
		}
	}
	
	public Client getCurrentClient() {
		return this.client0;
	}
	
	public int getState() {
		final int state = this.serverState;
		if (state == Server.S_CONNECTED
				&& (System.nanoTime() - this.client0LastPing) > STALE_SPAN) {
			return Server.S_CONNECTED_STALE;
		}
		return state;
	}
	
	public synchronized ConsoleEngine start(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		assert (client.slot == 0);
		this.clientLocks[client.slot].writeLock().lock();
		try {
			if (this.mainLoopState != ENGINE_NOT_STARTED) {
				throw new IllegalStateException("R engine is already started.");
			}
			
			final ConsoleEngineImpl consoleEngine = new ConsoleEngineImpl(this.publicServer, this, client);
			final ConsoleEngine export = (ConsoleEngine) AbstractServerControl.exportObject(consoleEngine);
			
			final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(this.rClassLoader);
				
				if (Rengine.getVersion() < REQUIRED_JRI_API) {
					final String message = "Unsupported JRI version (API found: 0x" + Long.toHexString(Rengine.getVersion()) + ", required: 0x" + Long.toHexString(REQUIRED_JRI_API) + ").";
					LOGGER.log(Level.SEVERE, message);
					internalRStopped();
					throw new RjInitFailedException(message);
				}
				
				final String[] args = checkArgs((String[]) properties.get("args"));
				
				this.mainLoopState = ENGINE_RUN_IN_R;
				this.hotMode = true;
				final Rengine re = new Rengine(args, this.rConfig, true, new InitCallbacks());
				
				while (this.rEngine != re) {
					Thread.sleep(100);
				}
				if (!re.waitForR()) {
					internalRStopped();
					throw new IllegalThreadStateException("R thread not started (" + re.getExitCode() + ")");
				}
				
				this.mainExchangeLock.lock();
				try {
					this.mainLoopS2CAnswerFail = 0;
					this.mainLoopClient0State = CLIENT_OK;
				}
				finally {
					this.mainExchangeLock.unlock();
				}
				
				setProperties(client.slot, properties, true);
				
				LOGGER.log(Level.INFO, "R engine started successfully. New Client-State: 'Connected'.");
				
				connectClient0(client, consoleEngine, export);
				return export;
			}
			catch (final Throwable e) {
				this.serverState = S_STOPPED;
				final String message = "Could not start the R engine";
				LOGGER.log(Level.SEVERE, message, e);
				if (export != null) {
					UnicastRemoteObject.unexportObject(export, true);
				}
				throw new RemoteException(message, e);
			}
			finally {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
		}
		finally {
			this.clientLocks[client.slot].writeLock().unlock();
		}
	}
	
	public void setProperties(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		this.clientLocks[client.slot].readLock().lock();
		try {
			checkClient(client);
			setProperties(client.slot, properties, properties.containsKey("rj.com.init"));
		}
		finally {
			this.clientLocks[client.slot].readLock().unlock();
		}
	}
	
	private void setProperties(final byte slot, final Map<String, ? extends Object> properties, final boolean init) {
		{	final Object max = properties.get(RjsComConfig.RJ_DATA_STRUCTS_LISTS_MAX_LENGTH_PROPERTY_ID);
			if (max instanceof Integer) {
				this.rniListsMaxLength = ((Integer) max).intValue();
			}
		}
		{	final Object max = properties.get(RjsComConfig.RJ_DATA_STRUCTS_ENVS_MAX_LENGTH_PROPERTY_ID);
			if (max instanceof Integer) {
				this.rniEnvsMaxLength = ((Integer) max).intValue();
			}
		}
		if (init) {
			final Object id = properties.get(RjsComConfig.RJ_COM_S2C_ID_PROPERTY_ID);
			if (id instanceof Integer) {
				this.mainLoopS2CLastCommands[slot].setId(((Integer) id).intValue());
			}
			else {
				this.mainLoopS2CLastCommands[slot].setId(0);
			}
		}
		else {
			properties.remove(RjsComConfig.RJ_COM_S2C_ID_PROPERTY_ID);
		}
		super.setClientProperties(slot, properties);
	}
	
	private void initEngine(final Rengine re) {
		this.rEngine = re;
		this.rEngine.setContextClassLoader(this.rClassLoader);
		this.rObjectFactory = new JRIObjectFactory();
		RjsComConfig.setDefaultRObjectFactory(this.rObjectFactory);
		
		this.rEngine.addMainLoopCallbacks(this.hotModeCallbacks);
		try {
			this.rni = new JRIServerRni(this.rEngine);
			
			this.dbgSupport = new JRIServerDbg(re, this.rni, this.utils, this);
			
			loadPlatformData();
			
			if (this.rClassLoader.getOSType() == RJClassLoader.OS_WIN) {
				if (this.rArgs.contains("--internet2")
						|| "2".equals(System.getenv("R_NETWORK")) ) {
					this.rEngine.rniEval(this.rEngine.rniParse("utils::setInternet2(use=TRUE)", 1), 0);
				}
				if (this.rMemSize != 0) {
					final long rniP = this.rEngine.rniEval(this.rEngine.rniParse("utils::memory.limit()", 1), 0);
					if (rniP != 0) {
						final long memSizeMB = this.rMemSize / MEGA;
						final double[] array = this.rEngine.rniGetDoubleArray(rniP);
						if (array != null && array.length == 1 && memSizeMB > array[0]) {
							this.rEngine.rniEval(this.rEngine.rniParse("utils::memory.limit(size="+memSizeMB+")", 1), 0);
						}
					}
				}
			}
			
			this.graphics = new JRIServerGraphics(this, this.rEngine);
			
			this.hotMode = false;
			if (this.hotModeDelayed) {
				this.hotModeRequested = true;
			}
			if (!this.hotModeRequested) {
				return;
			}
		}
		finally {
			this.rEngine.addMainLoopCallbacks(JRIServer.this);
		}
		
		// if (this.hotModeRequested)
		rProcessJEvents(this.rEngine);
	}
	
	private void loadPlatformData() {
		try {
			for (final Entry<String, String> dataEntry : this.platformDataCommands.entrySet()) {
				final DataCmdItem dataCmd = internalEvalData(new DataCmdItem(DataCmdItem.EVAL_DATA,
						0, (byte) 1, dataEntry.getValue(), null, null, null ));
				if (dataCmd != null && dataCmd.isOK()) {
					final RObject data = dataCmd.getData();
					if (data.getRObjectType() == RObject.TYPE_VECTOR) {
						switch (data.getData().getStoreType()) {
						case RStore.CHARACTER:
							if (data.getLength() == 1) {
								this.platformDataValues.put(dataEntry.getKey(), data.getData().get(0));
								continue;
							}
						}
					}
				}
				LOGGER.log(Level.WARNING, "The platform data item '" + dataEntry.getKey() + "' could not be created.");
			}
		}
		catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "An error occurred when loading platform data.", e);
		}
		
		if (LOGGER.isLoggable(Level.FINE)) {
			final StringBuilder sb = new StringBuilder("R Platform Data");
			ServerUtil.prettyPrint(this.platformDataValues, sb);
			LOGGER.log(Level.FINE, sb.toString());
		}
	}
	
	public Map<String, Object> getPlatformData() {
		return this.platformDataValues;
	}
	
	private String[] checkArgs(final String[] args) {
		final List<String> checked = new ArrayList<String>(args.length+1);
		boolean saveState = false;
		for (final String arg : args) {
			if (arg != null && arg.length() > 0) {
				// add other checks here
				if (arg.equals("--interactive")) {
					saveState = true;
				}
				else if (arg.startsWith("--max-cssize=")) {
					long size = s2long(arg.substring(13), 0);
					size = ((size + MEGA - 1) / MEGA) * MEGA;
					if (size >= 4 * MEGA) {
						this.rConfig.MainCStack_Size = size;
					}
				}
				else if (arg.startsWith("--max-mem-size=")) {
					final long size = s2long(arg.substring(15), 0);
					if (size > 0) {
						this.rMemSize = size;
					}
				}
				checked.add(arg);
			}
		}
		if (!saveState && this.rClassLoader.getOSType() != RJClassLoader.OS_WIN) {
			checked.add(0, "--interactive");
		}
		this.rArgs = checked;
		return checked.toArray(new String[checked.size()]);
	}
	
	public ConsoleEngine connect(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		assert (client.slot == 0);
		this.clientLocks[client.slot].writeLock().lock();
		try {
			if (this.client0 == client) {
				return this.client0ExpRef;
			}
			final ConsoleEngine consoleEngine = new ConsoleEngineImpl(this.publicServer, this, client);
			final ConsoleEngine export = (ConsoleEngine) AbstractServerControl.exportObject(consoleEngine);
			
			this.mainExchangeLock.lock();
			try {
				this.mainLoopS2CAnswerFail = 0;
				switch (this.mainLoopState) {
				case ENGINE_WAIT_FOR_CLIENT:
				case ENGINE_RUN_IN_R:
					// exit old client
					if (this.mainLoopClient0State == CLIENT_OK_WAIT) {
						this.mainLoopClient0State = CLIENT_CANCEL;
						this.mainExchangeClient.signalAll();
						while (this.mainLoopClient0State == CLIENT_CANCEL) {
							try {
								this.mainExchangeClient.awaitNanos(100000000L);
							}
							catch (final InterruptedException e) {}
						}
						// setup new client
						if (this.mainLoopClient0State != CLIENT_NONE) {
							throw new AssertionError();
						}
					}
					
					this.mainLoopBusyAtClient = true;
					this.mainLoopClient0State = CLIENT_OK;
					this.mainLoopS2CAnswerFail = 0;
					if (this.mainLoopS2CLastCommands[0].getItems() != null) {
						if (this.mainLoopS2CNextCommandsFirst[0] != null) {
							MainCmdItem last = this.mainLoopS2CLastCommands[0].getItems();
							while (last.next != null) {
								last = last.next;
							}
							last.next = this.mainLoopS2CNextCommandsFirst[0];
						}
						this.mainLoopS2CNextCommandsFirst[0] = this.mainLoopS2CLastCommands[0].getItems();
					}
					if (this.mainLoopS2CNextCommandsFirst[0] == null && !this.mainLoopS2CRequest.isEmpty()) {
						this.mainLoopS2CNextCommandsFirst[0] = this.mainLoopS2CNextCommandsLast[0] = this.mainLoopS2CRequest.get(this.mainLoopS2CRequest.size()-1);
					}
					// restore mainLoopS2CNextCommandsLast, if necessary
					if (this.mainLoopS2CNextCommandsLast[0] == null && this.mainLoopS2CNextCommandsFirst != null) {
						this.mainLoopS2CNextCommandsLast[0] = this.mainLoopS2CNextCommandsFirst[0];
						while (this.mainLoopS2CNextCommandsLast[0].next != null) {
							this.mainLoopS2CNextCommandsLast[0] = this.mainLoopS2CNextCommandsLast[0].next;
						}
					}
					// notify listener client
//					try {
//						new RjsStatusImpl2(V_CANCEL, S_DISCONNECTED, 
//								(this.currentUsername != null) ? ("user "+ this.currentUsername) : null);
//					}
//					catch (Throwable e) {}
					LOGGER.log(Level.INFO, "New client connected. New Client-State: 'Connected'.");
					
					setProperties(client.slot, properties, true);
					
					connectClient0(client, consoleEngine, export);
					return export;
				default:
					throw new IllegalStateException("R engine is not running.");
				}
			}
			catch (final Throwable e) {
				final String message = "An error occurred when connecting.";
				LOGGER.log(Level.SEVERE, message, e);
				if (export != null) {
					UnicastRemoteObject.unexportObject(export, true);
				}
				throw new RemoteException(message, e);
			}
			finally {
				this.mainExchangeLock.unlock();
			}
		}
		finally {
			this.clientLocks[client.slot].writeLock().unlock();
		}
	}
	
	public void disconnect(final Client client) throws RemoteException {
		assert (client.slot == 0);
		this.clientLocks[client.slot].writeLock().lock();
		try {
			checkClient(client);
			
			this.mainExchangeLock.lock();
			try {
				if (this.mainLoopClient0State == CLIENT_OK_WAIT) {
					// exit old client
					this.mainLoopClient0State = CLIENT_CANCEL;
					while (this.mainLoopClient0State == CLIENT_CANCEL) {
						this.mainExchangeClient.signalAll();
						try {
							this.mainExchangeClient.awaitNanos(100000000L);
						}
						catch (final InterruptedException e) {}
					}
					// setup new client
					if (this.mainLoopClient0State != CLIENT_NONE) {
						throw new AssertionError();
					}
				}
				else {
					this.mainLoopClient0State = CLIENT_NONE;
				}
				disconnectClient0();
			}
			finally {
				this.mainExchangeLock.unlock();
			}
		}
		finally {
			this.clientLocks[client.slot].writeLock().unlock();
		}
	}
	
	public RjsComObject runMainLoop(final Client client, final RjsComObject command) throws RemoteException {
		this.clientLocks[client.slot].readLock().lock();
		boolean clientLock = true;
		try {
			checkClient(client);
			
			this.mainLoopS2CLastCommands[client.slot].clear();
			
			switch ((command != null) ? command.getComType() : RjsComObject.T_MAIN_LIST) {
			
			case RjsComObject.T_PING:
				return RjsStatus.OK_STATUS;
			
			case RjsComObject.T_MAIN_LIST:
				final MainCmdC2SList mainC2SCmdList = (MainCmdC2SList) command;
				if (client.slot > 0 && mainC2SCmdList != null) {
					MainCmdItem item = mainC2SCmdList.getItems();
					while (item != null) {
						item.slot = client.slot;
						item = item.next;
					}
				}
				this.mainExchangeLock.lock();
				this.clientLocks[client.slot].readLock().unlock();
				clientLock = false;
				try {
					return internalMainCallbackFromClient(client.slot, mainC2SCmdList);
				}
				finally {
					this.mainExchangeLock.unlock();
				}
				
			case RjsComObject.T_CTRL:
				return internalCtrl(client.slot, (CtrlCmdItem) command);
				
			case RjsComObject.T_FILE_EXCHANGE:
				return command;
			
			default:
				throw new IllegalArgumentException("Unknown command: " + "0x"+Integer.toHexString(command.getComType()) + ".");
			
			}
		}
		finally {
			if (clientLock) {
				this.clientLocks[client.slot].readLock().unlock();
			}
		}
	}
	
	
	public RjsComObject runAsync(final Client client, final RjsComObject command) throws RemoteException {
		this.clientLocks[client.slot].readLock().lock();
		try {
			checkClient(client);
			
			if (command == null) {
				throw new IllegalArgumentException("Missing command.");
			}
			switch (command.getComType()) {
			
			case RjsComObject.T_PING:
				if (client.slot == 0) {
					this.client0LastPing = System.nanoTime();
				}
				return internalPing();
			
			case RjsComObject.T_CTRL:
				return internalCtrl(client.slot, (CtrlCmdItem) command);
			
			case RjsComObject.T_DBG:
				return internalAsyncDbg(client.slot, (DbgCmdItem) command);
				
			case RjsComObject.T_FILE_EXCHANGE:
				return command;
			
			default:
				throw new IllegalArgumentException("Unknown command: " + "0x"+Integer.toHexString(command.getComType()) + ".");
			}
		}
		finally {
			this.clientLocks[client.slot].readLock().unlock();
		}
	}
	
	private RjsStatus internalCtrl(final byte slot, final CtrlCmdItem cmd) {
		switch (cmd.getCtrlId()) {
		
		case CtrlCmdItem.REQUEST_CANCEL:
			try {
				if (this.mainInterruptLock.tryLock(1L, TimeUnit.SECONDS)) {
					try {
						this.rni.rniInterrupted = true;
						this.rEngine.rniStop(0);
						
						if (this.dbgSupport != null) {
							this.dbgSupport.rCancelled();
						}
						
						return RjsStatus.OK_STATUS;
					}
					catch (final Throwable e) {
						LOGGER.log(Level.SEVERE, "An error occurred when trying to interrupt the R engine.", e);
						return new RjsStatus(RjsStatus.ERROR, CODE_CTRL_REQUEST_CANCEL | 0x2);
					}
					finally {
						this.mainInterruptLock.unlock();
					}
				}
			}
			catch (final InterruptedException e) {
				Thread.interrupted();
			}
			return new RjsStatus(RjsStatus.ERROR, CODE_CTRL_REQUEST_CANCEL | 0x1, "Timeout.");
		
		case CtrlCmdItem.REQUEST_HOT_MODE:
			this.mainExchangeLock.lock();
			try {
				if (!this.hotModeRequested) {
					if (this.hotMode) {
						this.hotModeRequested = true;
					}
					else {
						this.hotModeRequested = true;
						this.rEngine.rniSetProcessJEvents(1);
						this.mainExchangeR.signalAll();
					}
				}
				return RjsStatus.OK_STATUS;
			}
			catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "An error occurred when requesting hot mode.", e);
				return new RjsStatus(RjsStatus.ERROR, CODE_CTRL_REQUEST_HOT_MODE | 0x2);
			}
			finally {
				this.mainExchangeLock.unlock();
			}
			
		}
		return new RjsStatus(RjsStatus.ERROR, CODE_CTRL_COMMON | 0x2);
	}
	
	private RjsStatus internalPing() {
		final Rengine r = this.rEngine;
		if (r.isAlive()) {
			return RjsStatus.OK_STATUS;
		}
		if (this.mainLoopState != ENGINE_STOPPED) {
			// invalid state
		}
		return new RjsStatus(RjsStatus.WARNING, S_STOPPED);
	}
	
	private RjsComObject internalMainCallbackFromClient(final byte slot, final MainCmdC2SList mainC2SCmdList) {
//		System.out.println("fromClient 1: " + mainC2SCmdList);
//		System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//		System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
		
		if (slot == 0 && this.mainLoopClient0State != CLIENT_OK) {
			return new RjsStatus(RjsStatus.WARNING, S_DISCONNECTED);
		}
		if (this.mainLoopState == ENGINE_WAIT_FOR_CLIENT) {
			if (mainC2SCmdList == null && this.mainLoopS2CNextCommandsFirst[slot] == null) {
				if (slot == 0) {
					if (this.mainLoopS2CAnswerFail < 3) { // retry
						this.mainLoopS2CAnswerFail++;
						this.mainLoopS2CNextCommandsFirst[0] = this.mainLoopS2CNextCommandsLast[0] = this.mainLoopS2CRequest.get(this.mainLoopS2CRequest.size()-1);
						LOGGER.log(Level.WARNING, "Unanswered request - retry: " + this.mainLoopS2CNextCommandsLast[0]);
						// continue ANSWER
					}
					else { // fail
						this.mainLoopC2SCommandFirst = this.mainLoopS2CRequest.get(this.mainLoopS2CRequest.size()-1);
						this.mainLoopC2SCommandFirst.setAnswer(new RjsStatus(RjsStatus.ERROR, 0));
						this.mainLoopS2CNextCommandsFirst[0] = this.mainLoopS2CNextCommandsLast[0] = null;
						LOGGER.log(Level.SEVERE, "Unanswered request - skip: " + this.mainLoopC2SCommandFirst);
						// continue in R
					}
				}
				else {
					return new RjsStatus(RjsStatus.ERROR, RjsStatus.ERROR);
				}
			}
			else { // ok
				this.mainLoopS2CAnswerFail = 0;
			}
		}
		if (mainC2SCmdList != null) {
			this.rni.rniInterrupted = false;
			if (this.mainLoopC2SCommandFirst == null) {
				this.mainLoopC2SCommandFirst = mainC2SCmdList.getItems();
			}
			else {
				MainCmdItem cmd = this.mainLoopC2SCommandFirst;
				while (cmd.next != null) {
					cmd = cmd.next;
				}
				cmd.next = mainC2SCmdList.getItems();
			}
		}
		
//			System.out.println("fromClient 2: " + mainC2SCmdList);
//			System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//			System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
		
		this.mainExchangeR.signalAll();
		if (slot == 0) {
			this.mainLoopClient0State = CLIENT_OK_WAIT;
		}
		while (this.mainLoopS2CNextCommandsFirst[slot] == null
//					&& (this.mainLoopState != ENGINE_STOPPED)
				&& (this.mainLoopState == ENGINE_RUN_IN_R
						|| this.mainLoopC2SCommandFirst != null
						|| this.hotModeRequested)
				&& ((slot > 0)
						|| ( (this.mainLoopClient0State == CLIENT_OK_WAIT)
							&& (this.mainLoopStdOutSize == 0)
							&& (this.mainLoopBusyAtClient == this.mainLoopBusyAtServer) )
				)) {
			this.mainLoopClientListen++;
			try {
				this.mainExchangeClient.await(); // run in R
			}
			catch (final InterruptedException e) {}
			finally {
				this.mainLoopClientListen--;
			}
		}
		if (slot == 0 && this.mainLoopClient0State == CLIENT_OK_WAIT) {
			this.mainLoopClient0State = CLIENT_OK;
		}
		
//			System.out.println("fromClient 3: " + mainC2SCmdList);
//			System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//			System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
		
		// answer
		if (slot > 0 || this.mainLoopClient0State == CLIENT_OK) {
			if (this.mainLoopStdOutSize > 0) {
				internalClearStdOutBuffer();
				this.mainLoopStdOutSize = 0;
			}
			if (this.mainLoopState == ENGINE_STOPPED && this.mainLoopS2CNextCommandsFirst[slot] == null) {
				return new RjsStatus(RjsStatus.INFO, S_STOPPED);
			}
			this.mainLoopBusyAtClient = this.mainLoopBusyAtServer;
			this.mainLoopS2CLastCommands[slot].setBusy(this.mainLoopBusyAtClient);
			this.mainLoopS2CLastCommands[slot].setObjects(this.mainLoopS2CNextCommandsFirst[slot]);
			this.mainLoopS2CNextCommandsFirst[slot] = null;
			return this.mainLoopS2CLastCommands[slot];
		}
		else {
			this.mainLoopClient0State = CLIENT_NONE;
			return new RjsStatus(RjsStatus.CANCEL, S_DISCONNECTED); 
		}
	}
	
	private MainCmdItem internalMainFromR(final MainCmdItem initialItem) {
		MainCmdItem item = initialItem;
		boolean initial = true;
		while (true) {
			this.mainExchangeLock.lock();
			try {
//				System.out.println("fromR 1: " + item);
//				System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//				System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
				
				if (item != null) {
					if (this.mainLoopStdOutSize > 0) {
						internalClearStdOutBuffer();
						this.mainLoopStdOutSize = 0;
					}
					if (this.mainLoopS2CNextCommandsFirst[item.slot] == null) {
						this.mainLoopS2CNextCommandsFirst[item.slot] = this.mainLoopS2CNextCommandsLast[item.slot] = item;
					}
					else {
						this.mainLoopS2CNextCommandsLast[item.slot] = this.mainLoopS2CNextCommandsLast[item.slot].next = item;
					}
				}
				
				this.mainExchangeClient.signalAll();
				
				if (initial) {
					if (initialItem == null || !initialItem.waitForClient()) {
						return null;
					}
					initialItem.requestId = this.mainLoopS2CRequest.size();
					this.mainLoopS2CRequest.add(initialItem);
					initial = false;
				}
				
				// initial != null && initial.waitForClient()
				if (this.mainLoopState == ENGINE_STOPPED) {
					initialItem.setAnswer(new RjsStatus(RjsStatus.ERROR, S_STOPPED));
					return initialItem;
				}
				
				if (this.mainLoopC2SCommandFirst == null) {
					this.mainLoopState = ENGINE_WAIT_FOR_CLIENT;
					final int stackId = ++this.mainLoopServerStack;
					try {
						if (Thread.currentThread() == this.rEngine) {
							while ((this.mainLoopC2SCommandFirst == null && !this.hotModeRequested)
									|| this.mainLoopServerStack > stackId ) {
								int i = 0;
								final ServerRuntimePlugin[] plugins = this.pluginsList;
								this.mainExchangeLock.unlock();
								this.mainInterruptLock.lock();
								try {
									for (; i < plugins.length; i++) {
										plugins[i].rjIdle();
									}
									
									this.rEngine.rniIdle();
								}
								catch (final Throwable e) {
									if (i < plugins.length) {
										handlePluginError(plugins[i], e);
									}
								}
								finally {
									this.mainInterruptLock.unlock();
									this.mainExchangeLock.lock();
								}
								if ((this.mainLoopC2SCommandFirst != null || this.hotModeRequested)
										&& this.mainLoopServerStack <= stackId ) {
									break;
								}
								try {
									this.mainExchangeR.awaitNanos(50000000L);
								}
								catch (final InterruptedException e) {}
							}
						}
						else {
							// TODO log warning
							while (this.mainLoopC2SCommandFirst == null || this.mainLoopServerStack > stackId) {
								try {
									this.mainExchangeR.awaitNanos(50000000L);
								}
								catch (final InterruptedException e) {}
							}
						}
					}
					finally {
						this.mainLoopServerStack--;
						this.mainLoopState = ENGINE_RUN_IN_R;
					}
				}
				
				if (this.hotModeRequested) {
					item = null;
				}
				else {
					// initial != null && initial.waitForClient()
					// && this.mainLoopC2SCommandFirst != null
					item = this.mainLoopC2SCommandFirst;
					this.mainLoopC2SCommandFirst = this.mainLoopC2SCommandFirst.next;
					item.next = null;
					
	//				System.out.println("fromR 2: " + item);
	//				System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
	//				System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
					
					if (item.getCmdType() < MainCmdItem.T_S2C_C2S) {
						// ANSWER
						if (initialItem.requestId == item.requestId) {
							this.mainLoopS2CRequest.remove((initialItem.requestId));
							assert (this.mainLoopS2CRequest.size() == item.requestId);
							return item;
						}
						else {
							item = null;
							continue;
						}
					}
				}
			}
			finally {
				this.mainExchangeLock.unlock();
			}
			
			// initial != null && initial.waitForClient()
			// && this.mainLoopC2SCommandFirst != null
			// && this.mainLoopC2SCommandFirst.getCmdType() < MainCmdItem.T_S2C_C2S
//			System.out.println("fromR evalDATA");
			
			if (item == null) {
				rProcessJEvents(this.rEngine);
				continue;
			}
			switch (item.getCmdType()) {
			
			case MainCmdItem.T_DATA_ITEM:
				item = internalEvalData((DataCmdItem) item);
				continue;
			case MainCmdItem.T_GRAPHICS_OP_ITEM:
				item = internalExecGraOp((GraOpCmdItem) item);
				continue;
			case MainCmdItem.T_DBG_ITEM:
				item = internalEvalDbg((DbgCmdItem) item);
				continue;
				
			default:
				continue;
			
			}
		}
	}
	
	private void internalClearStdOutBuffer() {
		final MainCmdItem item;
		if (this.mainLoopStdOutSingle != null) {
			item = new ConsoleWriteOutCmdItem(this.mainLoopStdOutSingle);
			this.mainLoopStdOutSingle = null;
		}
		else {
			item = new ConsoleWriteOutCmdItem(new String(this.mainLoopStdOutBuffer, 0, this.mainLoopStdOutSize));
		}
		if (this.mainLoopS2CNextCommandsFirst[0] == null) {
			this.mainLoopS2CNextCommandsFirst[0] = this.mainLoopS2CNextCommandsLast[0] = item;
		}
		else {
			this.mainLoopS2CNextCommandsLast[0] = this.mainLoopS2CNextCommandsLast[0].next = item;
		}
	}
	
	
	public boolean inSafeMode() {
		return this.safeMode;
	}
	
	public int beginSafeMode() {
		if (this.safeMode) {
			return 0;
		}
		try {
			// TODO disable
			this.safeMode = true;
			this.dbgSupport.beginSafeMode();
			return 1;
		}
		catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "An error occurred when running 'beginSafeMode' command.", e);
			return -1;
		}
	}
	
	public void endSafeMode(final int mode) {
		if (mode > 0) {
			try {
				this.safeMode = false;
				this.dbgSupport.endSafeMode();
			}
			catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "An error occurred when running 'endSafeMode' command.", e);
			}
		}
	}
	
	/**
	 * Executes an {@link DataCmdItem R data command} (assignment, evaluation, ...).
	 * Returns the result in the cmd object passed in, which is passed back out.
	 * 
	 * @param cmd the data command item
	 * @return the data command item with setted answer
	 */
	private DataCmdItem internalEvalData(final DataCmdItem cmd) {
		final byte savedSlot = this.currentSlot;
		this.currentSlot = cmd.slot;
		final boolean ownLock = this.rEngine.getRsync().safeLock();
		{	final byte depth = cmd.getDepth();
			this.rni.newDataLevel((depth >= 0) ? depth : 128,
					this.rniEnvsMaxLength, this.rniListsMaxLength );
		}
		final int savedProtected = this.rni.saveProtected();
		final int savedSafeMode = beginSafeMode();
		try {
			final String input = cmd.getDataText();
			if (input == null) {
				throw new IllegalStateException("Missing input.");
			}
			final RObject data = cmd.getData();
			final RObject envir = cmd.getRho();
			CMD_OP: switch (cmd.getOp()) {
			
			case DataCmdItem.EVAL_VOID: {
				if (this.rni.rniInterrupted) {
					throw new CancellationException();
				}
				if (data == null) {
					rniEval(input, envir);
				}
				else {
					rniEval(input, (RList) data, envir);
				}
				if (this.rni.rniInterrupted) {
					throw new CancellationException();
				}
				cmd.setAnswer(RjsStatus.OK_STATUS); }
				break CMD_OP;
			
			case DataCmdItem.EVAL_DATA: {
				if (this.rni.rniInterrupted) {
					throw new CancellationException();
				}
				final long objP;
				if (data == null) {
					objP = rniEval(input, envir);
				}
				else {
					objP = rniEval(input, (RList) data, envir);
				}
				if (this.rni.rniInterrupted) {
					throw new CancellationException();
				}
				cmd.setAnswer(this.rni.createDataObject(objP, cmd.getCmdOption()), null);
				break CMD_OP; }
			
			case DataCmdItem.RESOLVE_DATA: {
				final long objP = Long.parseLong(input);
				if (objP != 0) {
					if (this.rni.rniInterrupted) {
						throw new CancellationException();
					}
					cmd.setAnswer(this.rni.createDataObject(objP, cmd.getCmdOption()), null);
				}
				else {
					cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, (CODE_DATA_RESOLVE_DATA | 0x1),
							"Invalid reference." ));
				}
				break CMD_OP; }
			
			case DataCmdItem.ASSIGN_DATA: {
				if (this.rni.rniInterrupted) {
					throw new CancellationException();
				}
				rniAssign(input, data, envir);
				cmd.setAnswer(RjsStatus.OK_STATUS);
				break CMD_OP; }
			
			case DataCmdItem.FIND_DATA: {
				final long[] foundP = rniFind(input, envir, (cmd.getCmdOption() & 0x1000) != 0);
				if (foundP != null) {
					cmd.setAnswer(
							this.rni.createDataObject(foundP[1], cmd.getCmdOption() & 0xfff),
							new JRIEnvironmentImpl(this.rni.getEnvName(foundP[0]), foundP[0],
									null, null, this.rEngine.rniGetLength(foundP[0]),
									this.rEngine.rniGetClassAttrString(foundP[0]) ));
				}
				else {
					cmd.setAnswer(null, null);
				}
				break CMD_OP; }
			
			default:
				throw new IllegalStateException("Unsupported command.");
			
			}
			if (this.rni.rniInterrupted) {
				throw new CancellationException();
			}
		}
		catch (final RjsException e) {
			cmd.setAnswer(e.getStatus());
		}
		catch (final CancellationException e) {
			cmd.setAnswer(RjsStatus.CANCEL_STATUS);
		}
		catch (final Throwable e) {
			final String message = "Eval data failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, (CODE_DATA_COMMON | 0x1),
					"Internal server error (see server log)." ));
		}
		finally {
			this.currentSlot = savedSlot;
			endSafeMode(savedSafeMode);
			this.rni.looseProtected(savedProtected);
			this.rni.exitDataLevel();
			
			if (this.rni.rniInterrupted) {
				this.mainInterruptLock.lock();
				try {
					if (this.rni.rniInterrupted) {
						try {
							Thread.sleep(10);
						}
						catch (final InterruptedException e) {}
						this.rEngine.rniEval(this.rni.p_evalDummyExpr, 0);
						this.rni.rniInterrupted = false;
					}
				}
				finally {
					this.mainInterruptLock.unlock();
					if (ownLock) {
						this.rEngine.getRsync().unlock();
					}
				}
			}
			else {
				if (ownLock) {
					this.rEngine.getRsync().unlock();
				}
			}
		}
		return cmd.waitForClient() ? cmd : null;
	}
	
	private long rniEval(final String expression, final RObject envir) throws RjsException {
		final long envP = this.rni.resolveEnvironment(envir);
		final long exprP = this.rni.resolveExpression(expression);
		return this.rni.evalExpr(exprP, envP, (CODE_DATA_EVAL_DATA | 0x3));
	}
	
	private long rniEval(final String name, final RList args, final RObject envir) throws RjsException {
		final long envP = this.rni.resolveEnvironment(envir);
		final long exprP = this.rni.createFCall(name, args);
		return this.rni.evalExpr(exprP, envP, (CODE_DATA_EVAL_DATA | 0x4));
	}
	
	private long[] rniFind(final String name, final RObject envir, final boolean inherits) throws RjsException {
		long envP = this.rni.resolveEnvironment(envir);
		final long symbolP = this.rEngine.rniInstallSymbol(name);
		long p = this.rEngine.rniGetVarBySym(envP, symbolP);
		if (inherits) {
			while (p == 0 && (envP = this.rEngine.rniParentEnv(envP)) != 0
					&& envP != this.rni.p_EmptyEnv ) {
				p = this.rEngine.rniGetVarBySym(envP, symbolP);
			}
		}
		return (p != 0) ? new long[] { envP, p } : null;
	}
	
	/**
	 * Creates and assigns an {@link RObject RJ R object} to an expression (e.g. symbol) in R.
	 * 
	 * @param expression an expression the R object is assigned to
	 * @param obj an R object to assign
	 * @throws RjException
	*/ 
	private void rniAssign(final String expression, final RObject obj, final RObject envir) throws RjsException {
		final long envP = this.rni.resolveEnvironment(envir); // TODO
		if (obj == null) {
			throw new RjsException((CODE_DATA_ASSIGN_DATA | 0x2),
					"The R object to assign is missing." );
		}
		long exprP = this.rni.protect(this.rni.resolveExpression(expression));
		final long objP = this.rni.assignDataObject(obj);
		exprP = this.rEngine.rniCons(
				this.rni.p_AssignSymbol, this.rEngine.rniCons(
						exprP, this.rEngine.rniCons(
								objP, this.rni.p_NULL,
								0, false ),
						0, false ),
				0, true );
		this.rni.evalExpr(exprP, 0, (CODE_DATA_ASSIGN_DATA | 0x3));
	}
	
	
	/**
	 * Performs a graphics operations
	 * Returns the result in the cmd object passed in, which is passed back out.
	 * 
	 * @param cmd the command item
	 * @return the data command item with setted answer
	 */
	private GraOpCmdItem internalExecGraOp(final GraOpCmdItem cmd) {
		final byte savedSlot = this.currentSlot;
		this.currentSlot = cmd.slot;
		final int savedProtected = this.rni.saveProtected();
		final int savedSafeMode = beginSafeMode();
		try {
			CMD_OP: switch (cmd.getOp()) {
			
			case GraOp.OP_CLOSE:
				cmd.setAnswer(this.graphics.closeGraphic(cmd.getDevId()));
				break CMD_OP;
			case GraOp.OP_REQUEST_RESIZE:
				cmd.setAnswer(this.graphics.resizeGraphic(cmd.getDevId()));
				break CMD_OP;
				
			case GraOp.OP_CONVERT_DEV2USER: {
				final Coord coord = (Coord) cmd.getData();
				final RjsStatus status = this.graphics.convertDev2User(cmd.getDevId(), coord);
				if (status.getSeverity() == RjsStatus.OK) {
					cmd.setAnswer(coord);
				}
				else {
					cmd.setAnswer(status);
				}
				break CMD_OP; }
			case GraOp.OP_CONVERT_USER2DEV: {
				final Coord coord = (Coord) cmd.getData();
				final RjsStatus status = this.graphics.convertUser2Dev(cmd.getDevId(), coord);
				if (status.getSeverity() == RjsStatus.OK) {
					cmd.setAnswer(coord);
				}
				else {
					cmd.setAnswer(status);
				}
				break CMD_OP; }
			
			default:
				throw new IllegalStateException("Unsupported graphics operation " + cmd.toString());
			
			}
			return cmd;
		}
//		catch (final RjsException e) {
//			cmd.setAnswer(e.getStatus());
//			return cmd;
//		}
		catch (final CancellationException e) {
			cmd.setAnswer(RjsStatus.CANCEL_STATUS);
			return cmd;
		}
		catch (final Throwable e) {
			final String message = "Eval data failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, (CODE_DATA_COMMON | 0x1),
					"Internal server error (see server log)." ));
			return cmd;
		}
		finally {
			this.currentSlot = savedSlot;
			endSafeMode(savedSafeMode);
			this.rni.looseProtected(savedProtected);
		}
	}
	
	
	private DbgCmdItem internalEvalDbg(final DbgCmdItem cmd) {
		final byte savedSlot = this.currentSlot;
		this.currentSlot = cmd.slot;
		final boolean ownLock = this.rEngine.getRsync().safeLock();
		this.rni.newDataLevel(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		final int savedProtected = this.rni.saveProtected();
		final int savedSafeMode = beginSafeMode();
		final RMainLoopCallbacks savedCallback = this.rEngine.getMainLoopCallbacks();
		this.rEngine.addMainLoopCallbacks(this.hotModeCallbacks);
		try {
			if (this.rni.rniInterrupted) { // TODO
				this.rEngine.rniEval(this.rni.p_evalDummyExpr, 0);
			}
			CMD_OP: switch (cmd.getOp()) {
			
			case DbgCmdItem.OP_LOAD_FRAME_LIST:
				cmd.setAnswer(this.dbgSupport.loadCallStack());
				break CMD_OP;
			
			case DbgCmdItem.OP_LOAD_FRAME_CONTEXT:
				cmd.setAnswer(this.dbgSupport.loadFrameDetail(
						(FrameContextDetailRequest) cmd.getData() ));
				break CMD_OP;
			
			case DbgCmdItem.OP_SET_DEBUG:
				if (savedSafeMode != 0) { // was already inSafeMode
					cmd.setAnswer(this.dbgSupport.setDebug(
							(SetDebugRequest) cmd.getData() ));
				}
				else {
					cmd.setAnswer(new RjsStatus(RjsStatus.WARNING, 0, "Debug must not be set in data mode."));
				}
				break CMD_OP;
			
			case DbgCmdItem.OP_REQUEST_SUSPEND:
				this.dbgSupport.requestSuspend();
				cmd.setAnswer(RjsStatus.OK_STATUS);
				break CMD_OP;
			
			case DbgCmdItem.OP_INSTALL_TP_POSITIONS:
				cmd.setAnswer(this.dbgSupport.installTracepoints(
						(ElementTracepointInstallationRequest) cmd.getData() ));
				break CMD_OP;
			
			case DbgCmdItem.OP_RESET_FILTER_STATE:
				cmd.setAnswer(this.dbgSupport.setFilterState(
						(DbgFilterState) cmd.getData() ));
				break CMD_OP;
			
			case DbgCmdItem.OP_UPDATE_TP_STATES:
				cmd.setAnswer(this.dbgSupport.updateTracepointStates(
						(TracepointStatesUpdate) cmd.getData() ));
				break CMD_OP;
			
			case DbgCmdItem.OP_SET_ENABLEMENT:
				this.dbgSupport.setEnablement((DbgEnablement) cmd.getData());
				cmd.setAnswer(RjsStatus.OK_STATUS);
				break CMD_OP;
				
			default:
				throw new IllegalStateException("Unsupported command.");
			
			}
			if (this.rni.rniInterrupted) {
				throw new CancellationException();
			}
		}
		catch (final RjsException e) {
			final String message = "Eval dbg failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.WARNING, message, e);
			cmd.setAnswer(e.getStatus());
		}
		catch (final CancellationException e) {
			cmd.setAnswer(RjsStatus.CANCEL_STATUS);
		}
		catch (final Throwable e) {
			final String message = "Eval dbg failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, (CODE_DBG_COMMON | 0x1),
					"Internal server error (see server log)." ));
		}
		finally {
			this.rEngine.addMainLoopCallbacks(savedCallback);
			endSafeMode(savedSafeMode);
			this.rni.looseProtected(savedProtected);
			this.rni.exitDataLevel();
			this.currentSlot = savedSlot;
			
			if (this.rni.rniInterrupted) {
				this.mainInterruptLock.lock();
				try {
					if (this.rni.rniInterrupted) {
						try {
							Thread.sleep(10);
						}
						catch (final InterruptedException e) {
							e.printStackTrace();
						}
						this.rEngine.rniEval(this.rni.p_evalDummyExpr, 0);
						this.rni.rniInterrupted = false;
					}
				}
				finally {
					this.mainInterruptLock.unlock();
					if (ownLock) {
						this.rEngine.getRsync().unlock();
					}
				}
			}
			else {
				if (ownLock) {
					this.rEngine.getRsync().unlock();
				}
			}
		}
		return cmd.waitForClient() ? cmd : null;
	}
	
	private RjsStatus internalAsyncDbg(final byte slot, final DbgCmdItem cmd) {
		switch (cmd.getOp()) {
		
		case DbgCmdItem.OP_RESET_FILTER_STATE:
			return this.dbgSupport.setFilterState(
					(DbgFilterState) cmd.getData() );
		
		case DbgCmdItem.OP_SET_ENABLEMENT:
			this.dbgSupport.setEnablement((DbgEnablement) cmd.getData());
			return RjsStatus.OK_STATUS;
		
		case DbgCmdItem.OP_UPDATE_TP_STATES:
			return this.dbgSupport.updateTracepointStates(
					(TracepointStatesUpdate) cmd.getData() );
		
		}
		return new RjsStatus(RjsStatus.ERROR, CODE_DBG_COMMON | 0x2);
	}
	
	public void handle(final TracepointEvent event) {
		internalMainFromR(new DbgCmdItem(DbgCmdItem.OP_NOTIFY_TP_EVENT, 0, event));
	}
	
	
	public String rReadConsole(final Rengine re, final String prompt, final int addToHistory) {
		if (prompt.startsWith("Browse")) {
			final String input = this.dbgSupport.handleBrowserPrompt(prompt);
			if (input != null) {
				return input;
			}
		}
		else if (inSafeMode()) {
			return "\n";
		}
		
		this.dbgSupport.clearContext();
		
		final MainCmdItem cmd = internalMainFromR(new ConsoleReadCmdItem(
				(addToHistory == 1) ? V_TRUE : V_FALSE, prompt ));
		if (cmd.isOK()) {
			return cmd.getDataText();
		}
		return "\n";
	}
	
	public void rWriteConsole(final Rengine re, final String text, final int type) {
		if (type == 0) {
			this.mainExchangeLock.lock();
			try {
				// first
				if (this.mainLoopStdOutSize == 0) {
					this.mainLoopStdOutSingle = text;
					this.mainLoopStdOutSize = text.length();
				}
				
				// buffer full
				else if (this.mainLoopStdOutSize + text.length() > STDOUT_BUFFER_SIZE) {
					internalClearStdOutBuffer();
					this.mainLoopStdOutSingle = text;
					this.mainLoopStdOutSize = text.length();
				}
				
				// add to buffer
				else {
					if (this.mainLoopStdOutSingle != null) {
						this.mainLoopStdOutSingle.getChars(0, this.mainLoopStdOutSingle.length(),
								this.mainLoopStdOutBuffer, 0 );
						this.mainLoopStdOutSingle = null;
					}
					text.getChars(0, text.length(), this.mainLoopStdOutBuffer, this.mainLoopStdOutSize);
					this.mainLoopStdOutSize += text.length();
				}
				
				if (this.mainLoopClientListen > 0) {
					this.mainExchangeClient.signalAll();
				}
				return;
			}
			finally {
				this.mainExchangeLock.unlock();
			}
		}
		else {
			internalMainFromR(new ConsoleWriteErrCmdItem(text));
		}
	}
	
	public void rFlushConsole(final Rengine re) {
		internalMainFromR(null);
	}
	
	public void rBusy(final Rengine re, final int which) {
		this.mainLoopBusyAtServer = (which == 1);
		internalMainFromR(null);
	}
	
	public void rShowMessage(final Rengine re, final String message) {
		internalMainFromR(new ConsoleMessageCmdItem(message));
	}
	
	public String rChooseFile(final Rengine re, final int newFile) {
		final RList args = this.rObjectFactory.createList(new RObject[] {
				this.rObjectFactory.createVector(this.rObjectFactory.createLogiData(new boolean[] {
						(newFile == 1),
				} )),
		}, new String[] {
				"newResource",
		} );
		final RList answer = execUICommand("common/chooseFile", args, true);
		if (answer != null) {
			final RObject filenameObject = answer.get("filename");
			if (RDataUtil.isSingleString(filenameObject)) {
				return filenameObject.getData().getChar(0);
			}
		}
		return null;
	}
	
	public void rLoadHistory(final Rengine re, final String filename) {
		final RList args = this.rObjectFactory.createList(new RObject[] {
				this.rObjectFactory.createVector(this.rObjectFactory.createCharData(new String[] {
						filename,
				} )),
		}, new String[] {
				"filename",
		} );
		execUICommand("common/loadHistory", args, true);
	}
	
	public void rSaveHistory(final Rengine re, final String filename) {
		final RList args = this.rObjectFactory.createList(new RObject[] {
				this.rObjectFactory.createVector(this.rObjectFactory.createCharData(new String[] {
						filename,
				} )),
		}, new String[] {
				"filename",
		} );
		execUICommand("common/saveHistory", args, true);
	}
	
	public long rExecJCommand(final Rengine re, String commandId, final long argsP, final int options) {
		try {
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, "Executing java command ''{0}''.", commandId);
			}
			final int idx = commandId.indexOf(':');
			if (idx <= 0) {
				return 0;
			}
			final String commandGroup = commandId.substring(0, idx);
			commandId = commandId.substring(idx+1);
			
			if (commandGroup.equals("ui")) {
				final boolean wait = ((options | 1) != 0);
				final RList answer = execUICommand(commandId, createJCommandArgs(argsP), wait);
				return createJCommandAnswer(answer);
			}
			if (commandGroup.equals("dbg") && this.dbgSupport != null) {
				return this.dbgSupport.execRJCommand(commandId, argsP);
			}
		}
		catch (final Exception e) {
			LOGGER.log(Level.WARNING, "An error occurred when executing java command '" + commandId + "'.", e);
		}
		return 0;
	}
	
	private RList createJCommandArgs(final long p) {
		if (p != 0) {
			this.rni.newDataLevel(255, this.rniEnvsMaxLength, this.rniListsMaxLength);
			final int savedProtected = this.rni.saveProtected();
			try {
				this.rni.protect(p);
				final RObject rObject = this.rni.createDataObject(p, 0, EVAL_MODE_DEFAULT);
				if (rObject.getRObjectType() == RObject.TYPE_LIST) {
					return (RList) rObject;
				}
			}
			finally {
				this.rni.looseProtected(savedProtected);
				this.rni.exitDataLevel();
			}
		}
		return null;
	}
	
	private long createJCommandAnswer(final RList answer) throws RjsException {
		if (answer != null) {
			this.rni.newDataLevel(255, this.rniEnvsMaxLength, this.rniListsMaxLength);
			final int savedProtected = this.rni.saveProtected();
			try {
				return this.rni.assignDataObject(answer);
			}
			finally {
				this.rni.looseProtected(savedProtected);
				this.rni.exitDataLevel();
			}
		}
		return 0;
	}
	
	public RList execUICommand(final String command, final RList args, final boolean wait) {
		if (command == null) {
			throw new NullPointerException("command");
		}
		final MainCmdItem answer = internalMainFromR(new ExtUICmdItem(command, 0, args, wait));
		if (wait && answer instanceof ExtUICmdItem && answer.isOK()) {
			return ((ExtUICmdItem) answer).getDataArgs();
		}
		return null;
	}
	
	public void rProcessJEvents(final Rengine re) {
		while (true) {
			this.mainExchangeLock.lock();
			try {
				if (!this.hotModeRequested) {
					return;
				}
				if (this.hotMode || this.mainLoopState == ENGINE_WAIT_FOR_CLIENT
						|| this.rEngine.getMainLoopCallbacks() != JRIServer.this) {
					this.hotModeRequested = false;
					this.hotModeDelayed = true;
					return;
				}
				this.hotModeRequested = false;
				this.hotMode = true;
			}
			finally {
				this.mainExchangeLock.unlock();
			}
			final int savedSafeMode = beginSafeMode();
			try {
				this.rEngine.addMainLoopCallbacks(this.hotModeCallbacks);
				internalMainFromR(new ConsoleReadCmdItem(2, ""));
			}
			catch (final Throwable e) {
				LOGGER.log(Level.SEVERE, "An error occured when running hot mode.", e);
			}
			finally {
				endSafeMode(savedSafeMode);
				this.mainExchangeLock.lock();
				try {
					this.rEngine.addMainLoopCallbacks(JRIServer.this);
					this.hotMode = false;
					if (this.hotModeDelayed) {
						this.hotModeDelayed = false;
						this.hotModeRequested = true;
					}
					if (!this.hotModeRequested) {
						return;
					}
				}
				finally {
					this.mainExchangeLock.unlock();
				}
			}
		}
	}
	
	private void internalClearClient(final Client client) {
		final MainCmdC2SList list = new MainCmdC2SList();
		final int savedClientState = this.mainLoopClient0State;
		this.mainLoopClient0State = CLIENT_OK;
		final byte slot = client.slot;
		try {
			if (slot == 0) {
				try {
					while (this.hotMode) {
						final MainCmdItem cmdItem = this.mainLoopS2CRequest.get(this.mainLoopS2CRequest.size()-1);
						cmdItem.setAnswer(RjsStatus.CANCEL_STATUS);
						list.setObjects(cmdItem);
						if (this.mainLoopS2CNextCommandsFirst[slot] == cmdItem) {
							this.mainLoopS2CNextCommandsFirst[slot] = null;
						}
						else {
							MainCmdItem item = this.mainLoopS2CNextCommandsFirst[slot];
							while (item != null) {
								if (item.next == cmdItem) {
									item.next = null;
									break;
								}
								item = item.next;
							}
						}
						
						internalMainCallbackFromClient((byte) 0, list);
					}
				}
				catch (final Exception e) {
					LOGGER.log(Level.SEVERE, "An error occurrend when trying to cancel hot loop.", e);
				}
			}
		}
		finally {
			this.mainLoopClient0State = savedClientState;
		}
	}
	
	private void internalRStopped() {
		this.mainExchangeLock.lock();
		try {
			if (this.mainLoopState == ENGINE_STOPPED) {
				return;
			}
			this.hotMode = false;
			this.mainLoopState = ENGINE_STOPPED;
			
			this.mainExchangeClient.signalAll();
			while (this.mainLoopS2CNextCommandsFirst != null || this.mainLoopStdOutSize > 0) {
				try {
					this.mainExchangeR.awaitNanos(100000000L);
				}
				catch (final InterruptedException e) {}
				this.mainExchangeClient.signalAll();
			}
		}
		finally {
			this.mainExchangeLock.unlock();
		}
		
		final ServerRuntimePlugin[] plugins;
		synchronized (this.pluginsLock) {
			plugins = this.pluginsList;
			this.pluginsList = new ServerRuntimePlugin[0];
		}
		for (int i = 0; i < plugins.length; i++) {
			try {
				plugins[i].rjStop(V_OK);
			}
			catch (final Throwable e) {
				handlePluginError(plugins[i], e);
			}
		}
		
		synchronized (this) {
			this.serverState = S_STOPPED;
			this.rEngine = null;
		}
	}
	
	@Override
	public void onRExit() {
		internalRStopped();
		super.onRExit();
	}
	
	@Override
	public void registerGraphic(final RjsGraphic graphic) {
		this.graphics.addGraphic(graphic);
	}
	
	@Override
	public void unregisterGraphic(final RjsGraphic graphic) {
		this.graphics.removeGraphic(graphic);
	}
	
	@Override
	public MainCmdItem sendMainCmd(final MainCmdItem cmd) {
		return internalMainFromR(cmd);
	}
	
}
