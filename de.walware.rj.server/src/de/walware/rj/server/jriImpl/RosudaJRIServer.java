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

package de.walware.rj.server.jriImpl;

import static de.walware.rj.data.RObjectFactory.F_ONLY_STRUCT;
import static de.walware.rj.server.RjsComObject.V_ERROR;
import static de.walware.rj.server.RjsComObject.V_FALSE;
import static de.walware.rj.server.RjsComObject.V_OK;
import static de.walware.rj.server.RjsComObject.V_TRUE;
import static de.walware.rj.server.Server.S_CONNECTED;
import static de.walware.rj.server.Server.S_DISCONNECTED;
import static de.walware.rj.server.Server.S_NOT_STARTED;
import static de.walware.rj.server.Server.S_STOPPED;

import java.lang.reflect.Field;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.logging.Logger;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.rj.JRClassLoader;

import de.walware.rj.RjException;
import de.walware.rj.RjInitFailedException;
import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RS4Object;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataStruct;
import de.walware.rj.data.defaultImpl.RFunctionImpl;
import de.walware.rj.data.defaultImpl.RMissing;
import de.walware.rj.data.defaultImpl.RNull;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.data.defaultImpl.ROtherImpl;
import de.walware.rj.data.defaultImpl.RReferenceImpl;
import de.walware.rj.data.defaultImpl.RS4ObjectImpl;
import de.walware.rj.data.defaultImpl.SimpleRListImpl;
import de.walware.rj.server.ConsoleEngine;
import de.walware.rj.server.ConsoleMessageCmdItem;
import de.walware.rj.server.ConsoleReadCmdItem;
import de.walware.rj.server.ConsoleWriteErrCmdItem;
import de.walware.rj.server.ConsoleWriteOutCmdItem;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdS2CList;
import de.walware.rj.server.RJ;
import de.walware.rj.server.RjsComConfig;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsException;
import de.walware.rj.server.RjsGraphic;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.server.srvImpl.ConsoleEngineImpl;
import de.walware.rj.server.srvImpl.DefaultServerImpl;
import de.walware.rj.server.srvImpl.InternalEngine;
import de.walware.rj.server.srvext.Client;
import de.walware.rj.server.srvext.ExtServer;
import de.walware.rj.server.srvext.ServerRuntimePlugin;


/**
 * Remove server based on
 */
public class RosudaJRIServer extends RJ
		implements InternalEngine, RMainLoopCallbacks, ExtServer {
	
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
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.jri");
	
	private static final int STDOUT_BUFFER_SIZE = 0x1FFF;
	
	private static final long REQUIRED_JRI_API = 0x0109;
	
	private static final String EVAL_TEMP_SYMBOL = "eval.temp";
	private static final byte EVAL_MODE_DEFAULT = 0;
	private static final byte EVAL_MODE_FORCE = 1;
	private static final byte EVAL_MODE_DATASLOT = 2;
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final RObject[] EMPTY_ROBJECT_ARRAY = new RObject[0];
	
	
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
	
	
	static {
		System.loadLibrary("jri");
	}
	
	
	private class InitCallbacks implements RMainLoopCallbacks {
		public String rReadConsole(final Rengine engine, final String prompt, final int addToHistory) {
			initEngine(engine);
			return RosudaJRIServer.this.rReadConsole(engine, prompt, addToHistory);
		}
		public void rWriteConsole(final Rengine engine, final String text, final int oType) {
			initEngine(engine);
			RosudaJRIServer.this.rWriteConsole(engine, text, oType);
		}
		public void rFlushConsole(final Rengine engine) {
			initEngine(engine);
			RosudaJRIServer.this.rFlushConsole(engine);
		}
		public void rBusy(final Rengine engine, final int which) {
			initEngine(engine);
			RosudaJRIServer.this.rBusy(engine, which);
		}
		public void rShowMessage(final Rengine engine, final String message) {
			initEngine(engine);
			RosudaJRIServer.this.rShowMessage(engine, message);
		}
		public String rChooseFile(final Rengine engine, final int newFile) {
			initEngine(engine);
			return RosudaJRIServer.this.rChooseFile(engine, newFile);
		}
		public void rLoadHistory(final Rengine engine, final String filename) {
			initEngine(engine);
			RosudaJRIServer.this.rLoadHistory(engine, filename);
		}
		public void rSaveHistory(final Rengine engine, final String filename) {
			initEngine(engine);
			RosudaJRIServer.this.rSaveHistory(engine, filename);
		}
	}
	
	
	private String name;
	
	private Server publicServer;
	private JRClassLoader rClassLoader;
	private Rengine rEngine;
	private List<String> rArgs;
	private long rCSSize;
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
	
	private int serverState;
	
	private final ReentrantReadWriteLock[] clientLocks = new ReentrantReadWriteLock[] {
			new ReentrantReadWriteLock(), new ReentrantReadWriteLock() };
	private Client client0;
	private ConsoleEngine client0Engine;
	private ConsoleEngine client0ExpRef;
	private ConsoleEngine client0PrevExpRef;
	private volatile long client0LastPing;
	
	private byte currentSlot;
	
	private final Object pluginsLock = new Object();
	private ServerRuntimePlugin[] pluginsList = new ServerRuntimePlugin[0];
	
	private int rniTemp;
	private boolean rniEvalTempAssigned;
	private int rniMaxDepth;
	private boolean rniInterrupted;
	private int rniProtectedCounter;
	private int rniListsMaxLength = 10000;
	private int rniEnvsMaxLength = 10000;
	
	private long rniP_NULL;
	private long rniP_Unbound;
	private long rniP_MissingArg;
	private long rniP_BaseEnv;
	private long rniP_AutoloadEnv;
	private long rniP_RJTempEnv;
	
	private long rniP_functionSymbol;
	private long rniP_ifSymbol;
	private long rniP_AssignSymbol;
	private long rniP_xSymbol;
	private long rniP_zSymbol;
	private long rniP_objectSymbol;
	private long rniP_envSymbol;
	private long rniP_nameSymbol;
	private long rniP_realSymbol;
	private long rniP_imaginarySymbol;
	private long rniP_newSymbol;
	private long rniP_ClassSymbol;
	private long rniP_exprSymbol;
	private long rniP_errorSymbol;
	
	private long rniP_factorClassString;
	private long rniP_orderedClassString;
	private long rniP_dataframeClassString;
	private long rniP_lengthFun;
	private long rniP_complexFun;
	private long rniP_envNameFun;
	private long rniP_headerFun;
	private long rniP_s4classFun;
	private long rniP_slotNamesFun;
	private long rniP_ReFun;
	private long rniP_ImFun;
	private long rniP_ItemFun;
	private long rniP_tryCatchFun;
	
	private long rniP_evalTryCatch_errorExpr;
	private long rniP_evalTemp_classExpr;
	private long rniP_evalTemp_rmExpr;
	private long rniP_evalDummyExpr;
	
	private RjsGraphic graphicLast;
	private final List<RjsGraphic> graphicList = new ArrayList<RjsGraphic>();
	
	private Map<String, String> platformDataCommands;
	private final Map<String, Object> platformDataValues = new HashMap<String, Object>();
	
	
	public RosudaJRIServer() {
		this.rCSSize = s2long(System.getProperty("jri.threadCStackSize"), 16 * MEGA);
		
		this.mainLoopState = ENGINE_NOT_STARTED;
		this.mainLoopClient0State = CLIENT_NONE;
		
		this.serverState = S_NOT_STARTED;
	}
	
	
	public void init(final String name, final Server publicServer) throws Exception {
		this.name = name;
		this.publicServer = publicServer;
		
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
			DefaultServerImpl.removeClient(this.client0ExpRef);
			this.client0 = null;
			this.client0Engine = null;
			this.client0PrevExpRef = this.client0ExpRef;
			this.client0ExpRef = null;
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
			final ConsoleEngine export = (ConsoleEngine) UnicastRemoteObject.exportObject(consoleEngine, 0);
			
			final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
			try {
				this.rClassLoader = JRClassLoader.getRJavaClassLoader();
				Thread.currentThread().setContextClassLoader(this.rClassLoader);
				
				if (Rengine.getVersion() < REQUIRED_JRI_API) {
					final String message = "Unsupported JRI version (API found: 0x" + Long.toHexString(Rengine.getVersion()) + ", required: 0x" + Long.toHexString(REQUIRED_JRI_API) + ").";
					LOGGER.log(Level.SEVERE, message);
					internalRStopped();
					throw new RjInitFailedException(message);
				}
				
				final String[] args = checkArgs((String[]) properties.get("args"));
				System.setProperty("jri.threadCStackSize", Long.toString(this.rCSSize));
				if (System.getProperty("jri.initCStackLimit") == null) {
					System.setProperty("jri.initCStackLimit", "yes");
				}
				
				this.mainLoopState = ENGINE_RUN_IN_R;
				final Rengine engine = new Rengine(args, true, new InitCallbacks());
				
				while (this.rEngine != engine) {
					Thread.sleep(100);
				}
				if (!engine.waitForR()) {
					internalRStopped();
					throw new IllegalThreadStateException("R thread not started");
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
	}
	
	private void initEngine(final Rengine engine) {
		this.rEngine = engine;
		this.rEngine.setContextClassLoader(this.rClassLoader);
		this.rEngine.addMainLoopCallbacks(RosudaJRIServer.this);
		RjsComConfig.setDefaultRObjectFactory(new JRIObjectFactory());
		
		this.rniP_NULL = this.rEngine.rniSpecialObject(Rengine.SO_NilValue);
		this.rEngine.rniPreserve(this.rniP_NULL);
		this.rniP_Unbound = this.rEngine.rniSpecialObject(Rengine.SO_UnboundValue);
		this.rEngine.rniPreserve(this.rniP_Unbound);
		this.rniP_MissingArg = this.rEngine.rniSpecialObject(Rengine.SO_MissingArg);
		this.rEngine.rniPreserve(this.rniP_MissingArg);
		this.rniP_BaseEnv = this.rEngine.rniSpecialObject(Rengine.SO_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_BaseEnv);
		
		this.rniP_AutoloadEnv = this.rEngine.rniEval(this.rEngine.rniInstallSymbol(".AutoloadEnv"), this.rniP_BaseEnv);
		if ((this.rniP_AutoloadEnv < 0L && this.rniP_AutoloadEnv >= -4L)
				|| this.rEngine.rniExpType(this.rniP_AutoloadEnv) != REXP.ENVSXP) {
			this.rniP_AutoloadEnv = 0L;
		}
		
		this.rniP_functionSymbol = this.rEngine.rniInstallSymbol("function");
		this.rEngine.rniPreserve(this.rniP_functionSymbol);
		this.rniP_ifSymbol = this.rEngine.rniInstallSymbol("if");
		this.rEngine.rniPreserve(this.rniP_ifSymbol);
		this.rniP_AssignSymbol = this.rEngine.rniInstallSymbol("<-");
		this.rEngine.rniPreserve(this.rniP_AssignSymbol);
		this.rniP_xSymbol = this.rEngine.rniInstallSymbol("x");
		this.rEngine.rniPreserve(this.rniP_xSymbol);
		this.rniP_zSymbol = this.rEngine.rniInstallSymbol("z");
		this.rEngine.rniPreserve(this.rniP_zSymbol);
		this.rniP_objectSymbol = this.rEngine.rniInstallSymbol("object");
		this.rEngine.rniPreserve(this.rniP_objectSymbol);
		this.rniP_envSymbol = this.rEngine.rniInstallSymbol("env");
		this.rEngine.rniPreserve(this.rniP_envSymbol);
		this.rniP_nameSymbol = this.rEngine.rniInstallSymbol("name");
		this.rEngine.rniPreserve(this.rniP_nameSymbol);
		this.rniP_realSymbol = this.rEngine.rniInstallSymbol("real");
		this.rEngine.rniPreserve(this.rniP_realSymbol);
		this.rniP_imaginarySymbol = this.rEngine.rniInstallSymbol("imaginary");
		this.rEngine.rniPreserve(this.rniP_imaginarySymbol);
		this.rniP_newSymbol = this.rEngine.rniInstallSymbol("new");
		this.rEngine.rniPreserve(this.rniP_newSymbol);
		this.rniP_ClassSymbol = this.rEngine.rniInstallSymbol("Class");
		this.rEngine.rniPreserve(this.rniP_ClassSymbol);
		this.rniP_exprSymbol = this.rEngine.rniInstallSymbol("expr");
		this.rEngine.rniPreserve(this.rniP_exprSymbol);
		this.rniP_errorSymbol = this.rEngine.rniInstallSymbol("error");
		this.rEngine.rniPreserve(this.rniP_errorSymbol);
		
		this.rniP_RJTempEnv = this.rEngine.rniEval(this.rEngine.rniParse("new.env()", 1), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_RJTempEnv);
		this.rniP_orderedClassString = this.rEngine.rniPutStringArray(new String[] { "ordered", "factor" });
		this.rEngine.rniPreserve(this.rniP_orderedClassString);
		this.rniP_factorClassString = this.rEngine.rniPutString("factor");
		this.rEngine.rniPreserve(this.rniP_factorClassString);
		this.rniP_dataframeClassString = this.rEngine.rniPutString("data.frame");
		this.rEngine.rniPreserve(this.rniP_dataframeClassString);
		
		this.rniP_lengthFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("length"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_lengthFun);
		this.rniP_complexFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("complex"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_complexFun);
		this.rniP_envNameFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("environmentName"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_envNameFun);
		this.rniP_ReFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("Re"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_ReFun);
		this.rniP_ImFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("Im"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_ImFun);
		this.rniP_ItemFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("$"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_ItemFun);
		this.rniP_tryCatchFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("tryCatch"), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_tryCatchFun);
		
		{	// function(x)paste(deparse(expr=args(name=x),control=c("keepInteger", "keepNA"),width.cutoff=500),collapse="")
			final long pasteFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("paste"), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(pasteFun);
			final long deparseFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("deparse"), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(deparseFun);
			final long argsFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("args"), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(argsFun);
			final long exprSymbol = this.rEngine.rniInstallSymbol("expr");
			this.rEngine.rniPreserve(exprSymbol);
			final long controlSymbol = this.rEngine.rniInstallSymbol("control");
			this.rEngine.rniPreserve(controlSymbol);
			final long widthcutoffSymbol = this.rEngine.rniInstallSymbol("width.cutoff");
			this.rEngine.rniPreserve(widthcutoffSymbol);
			final long collapseSymbol = this.rEngine.rniInstallSymbol("collapse");
			this.rEngine.rniPreserve(collapseSymbol);
			
			final long argList = this.rEngine.rniCons(this.rniP_MissingArg, this.rniP_NULL, this.rniP_xSymbol, false);
			this.rEngine.rniPreserve(argList);
			final long argsCall = this.rEngine.rniCons(argsFun, this.rEngine.rniCons(this.rniP_xSymbol,
					this.rniP_NULL, this.rniP_nameSymbol, false), 0L, true);
			this.rEngine.rniPreserve(argsCall);
			final long deparseControlValue = this.rEngine.rniPutStringArray(new String[] { "keepInteger", "keepNA" });
			this.rEngine.rniPreserve(deparseControlValue);
			final long deparseWidthcutoffValue = this.rEngine.rniPutIntArray(new int[] { 500 });
			this.rEngine.rniPreserve(deparseWidthcutoffValue);
			final long deparseCall = this.rEngine.rniCons(deparseFun, this.rEngine.rniCons(argsCall,
					this.rEngine.rniCons(deparseControlValue, this.rEngine.rniCons(deparseWidthcutoffValue,
					this.rniP_NULL, widthcutoffSymbol, false), controlSymbol, false), exprSymbol, false), 0L, true);
			this.rEngine.rniPreserve(deparseCall);
			final long collapseValue = this.rEngine.rniPutString("");
			this.rEngine.rniPreserve(collapseValue);
			final long body = this.rEngine.rniCons(pasteFun, this.rEngine.rniCons(deparseCall,
					this.rEngine.rniCons(collapseValue, this.rniP_NULL, collapseSymbol, false), 0L, false), 0L, true);
			
			this.rniP_headerFun = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_functionSymbol, this.rEngine.rniCons(argList,
					this.rEngine.rniCons(body, this.rniP_NULL, 0L, false)), 0L, true), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(this.rniP_headerFun);
		}
		{	// function(x)if(isS4(x))class(x)
			final long isS4Fun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("isS4"), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(isS4Fun);
			final long classFun = this.rEngine.rniEval(this.rEngine.rniInstallSymbol("class"), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(classFun);
			
			final long argList = this.rEngine.rniCons(this.rniP_MissingArg, this.rniP_NULL, this.rniP_xSymbol, false);
			this.rEngine.rniPreserve(argList);
			final long isS4Call = this.rEngine.rniCons(isS4Fun, this.rEngine.rniCons(this.rniP_xSymbol, this.rniP_NULL, this.rniP_objectSymbol, false), 0L, true);
			this.rEngine.rniPreserve(isS4Call);
			final long classCall = this.rEngine.rniCons(classFun, this.rEngine.rniCons(this.rniP_xSymbol, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true);
			this.rEngine.rniPreserve(classCall);
			final long body = this.rEngine.rniCons(this.rniP_ifSymbol, this.rEngine.rniCons(isS4Call, this.rEngine.rniCons(classCall, this.rniP_NULL, 0L, false), 0L, false), 0L, true);
			this.rEngine.rniPreserve(body);
			
			this.rniP_s4classFun = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_functionSymbol, this.rEngine.rniCons(argList,
					this.rEngine.rniCons(body, this.rniP_NULL, 0L, false)), 0L, true), this.rniP_BaseEnv);
			this.rEngine.rniPreserve(this.rniP_s4classFun);
		}
		this.rniP_slotNamesFun = this.rEngine.rniEval(this.rEngine.rniParse("methods::.slotNames", 1), this.rniP_BaseEnv);
		this.rEngine.rniPreserve(this.rniP_slotNamesFun);
		
		this.rniP_evalTryCatch_errorExpr = this.rEngine.rniCons(this.rEngine.rniEval(this.rEngine.rniParse("function(e){" +
				"s<-raw(5);" +
				"class(s)<-\".rj.eval.error\";" +
				"attr(s,\"error\")<-e;" +
				"attr(s,\"output\")<-paste(capture.output(print(e)),collapse=\"\\n\");" +
				"invisible(s);}", 1), 0L), this.rniP_NULL, this.rniP_errorSymbol, false);
		this.rEngine.rniPreserve(this.rniP_evalTryCatch_errorExpr);
		
		this.rniP_evalTemp_classExpr = this.rEngine.rniParse("class("+EVAL_TEMP_SYMBOL+");", 1);
		this.rEngine.rniPreserve(this.rniP_evalTemp_classExpr);
		this.rniP_evalTemp_rmExpr = this.rEngine.rniParse("rm("+EVAL_TEMP_SYMBOL+");", 1);
		this.rEngine.rniPreserve(this.rniP_evalTemp_rmExpr);
		
		this.rniP_evalDummyExpr = this.rEngine.rniParse("1+1;", 1);
		this.rEngine.rniPreserve(this.rniP_evalDummyExpr);
		
		if (LOGGER.isLoggable(Level.FINER)) {
			final StringBuilder sb = new StringBuilder("Pointers:");
			
			final Field[] fields = getClass().getDeclaredFields();
			for (final Field field : fields) {
				final String name = field.getName();
				if (name.startsWith("rniP_") && Long.TYPE.equals(field.getType())) {
					sb.append("\n\t");
					sb.append(name.substring(5));
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
		
		if (this.rniP_tryCatchFun <= 0L && this.rniP_tryCatchFun >= -4L) {
			LOGGER.log(Level.SEVERE, "Failed to initialize engine: Base functions are missing (check 'Renviron').");
			System.exit(4001);
			return;
		}
		
		loadPlatformData();
		
		if (this.rClassLoader.getOSType() == JRClassLoader.OS_WIN) {
			if (this.rArgs.contains("--internet2")) {
				this.rEngine.rniEval(this.rEngine.rniParse("utils::setInternet2(use=TRUE)", 1), 0L);
			}
			if (this.rMemSize != 0) {
				final long rniP = this.rEngine.rniEval(this.rEngine.rniParse("utils::memory.limit()", 1), 0L);
				if (rniP != 0) {
					final long memSizeMB = this.rMemSize / MEGA;
					final double[] array = this.rEngine.rniGetDoubleArray(rniP);
					if (array != null && array.length == 1 && memSizeMB > array[0]) {
						this.rEngine.rniEval(this.rEngine.rniParse("utils::memory.limit(size="+memSizeMB+")", 1), 0L);
					}
				}
			}
		}
	}
	
	private void loadPlatformData() {
		try {
			for (final Entry<String, String> dataEntry : this.platformDataCommands.entrySet()) {
				final DataCmdItem dataCmd = internalEvalData(new DataCmdItem(DataCmdItem.EVAL_DATA, 0, dataEntry.getValue()));
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
						this.rCSSize = size;
					}
				}
				else if (arg.startsWith("--max-mem-size=")) {
					long size = s2long(arg.substring(15), 0);
					if (size > 0) {
						this.rMemSize = size;
					}
				}
				checked.add(arg);
			}
		}
		if (!saveState && this.rClassLoader.getOSType() != JRClassLoader.OS_WIN) {
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
			final ConsoleEngine export = (ConsoleEngine) UnicastRemoteObject.exportObject(consoleEngine, 0);
			
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
							catch (final InterruptedException e) {
								Thread.interrupted();
							}
						}
						// setup new client
						if (this.mainLoopClient0State != CLIENT_NONE) {
							throw new AssertionError();
						}
					}
					
					this.mainLoopBusyAtClient = true;
					this.mainLoopClient0State = CLIENT_OK;
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
					if (this.mainLoopS2CNextCommandsFirst[0] == null) {
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
						catch (final InterruptedException e) {
							Thread.interrupted();
						}
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
	
	public boolean interrupt(final Client client) throws RemoteException {
		this.clientLocks[client.slot].readLock().lock();
		try {
			checkClient(client);
			try {
				if (this.mainInterruptLock.tryLock(1L, TimeUnit.SECONDS)) {
					try {
						this.rniInterrupted = true;
						this.rEngine.rniStop(0);
						return true;
					}
					catch (final Throwable e) {
						LOGGER.log(Level.SEVERE, "An error occurred when trying to interrupt the R engine.", e);
					}
					finally {
						this.mainInterruptLock.unlock();
					}
				}
			}
			catch (final InterruptedException e) {
				Thread.interrupted();
			}
			return false;
		}
		finally {
			this.clientLocks[client.slot].readLock().unlock();
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
	
	private RjsComObject internalMainCallbackFromClient(final byte slot, final MainCmdC2SList mainC2SCmdList) throws RemoteException {
//		System.out.println("fromClient 1: " + mainC2SCmdList);
//		System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//		System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
		
		if (slot == 0 && this.mainLoopClient0State != CLIENT_OK) {
			return new RjsStatus(RjsStatus.WARNING, S_DISCONNECTED);
		}
		if (this.mainLoopState == ENGINE_WAIT_FOR_CLIENT) {
			if (mainC2SCmdList == null && this.mainLoopS2CNextCommandsFirst[slot] == null) {
				if (slot == 0) {
					if (this.mainLoopS2CAnswerFail == 0) { // retry
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
			this.rniInterrupted = false;
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
				&& (this.mainLoopState == ENGINE_RUN_IN_R || this.mainLoopC2SCommandFirst != null)
				&& ((slot > 0)
						|| ( (this.mainLoopClient0State == CLIENT_OK_WAIT)
							&& (this.mainLoopStdOutSize == 0)
							&& (this.mainLoopBusyAtClient == this.mainLoopBusyAtServer) )
				)) {
			this.mainLoopClientListen++;
			try {
				this.mainExchangeClient.await(); // run in R
			}
			catch (final InterruptedException e) {
				Thread.interrupted();
			}
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
					initialItem.requestId = (byte) this.mainLoopS2CRequest.size();
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
							while (this.mainLoopC2SCommandFirst == null || this.mainLoopServerStack > stackId) {
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
								if (this.mainLoopC2SCommandFirst != null && this.mainLoopServerStack <= stackId) {
									break;
								}
								try {
									this.mainExchangeR.awaitNanos(50000000L);
								}
								catch (final InterruptedException e) {
									Thread.interrupted();
								}
							}
						}
						else {
							// TODO log warning
							while (this.mainLoopC2SCommandFirst == null || this.mainLoopServerStack > stackId) {
								try {
									this.mainExchangeR.awaitNanos(50000000L);
								}
								catch (final InterruptedException e) {
									Thread.interrupted();
								}
							}
						}
					}
					finally {
						this.mainLoopServerStack--;
						this.mainLoopState = ENGINE_RUN_IN_R;
					}
				}
				
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
						this.mainLoopS2CRequest.remove((initialItem.requestId & 0xff));
						return item;
					}
					else {
						continue;
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
			item = internalEvalData((DataCmdItem) item);
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
	
	/**
	 * Executes an {@link DataCmdItem R data command} (assignment, evaluation, ...).
	 * Returns the result in the cmd object passed in, which is passed back out.
	 * 
	 * @param cmd the data command item
	 * @return the data command item with setted answer
	 */
	private DataCmdItem internalEvalData(final DataCmdItem cmd) {
		final byte previousSlot = this.currentSlot;
		this.currentSlot = cmd.slot;
		final boolean ownLock = this.rEngine.getRsync().safeLock();
		final int prevMaxDepth = this.rniMaxDepth;
		{	final byte depth = cmd.getDepth();
			this.rniMaxDepth = this.rniTemp + ((depth >= 1) ? depth : 128);
		}
		final int savedProtectedCounter = this.rniProtectedCounter;
		try {
			final String input = cmd.getDataText();
			if (input == null) {
				throw new IllegalStateException("Missing input.");
			}
			DATA_CMD: switch (cmd.getEvalType()) {
			case DataCmdItem.EVAL_VOID:
				{	if (this.rniInterrupted) {
						throw new CancellationException();
					}
					rniEval(input);
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					cmd.setAnswer(RjsStatus.OK_STATUS);
				}
				break DATA_CMD;
			case DataCmdItem.EVAL_DATA:
				{	if (this.rniInterrupted) {
						throw new CancellationException();
					}
					final long objP = rniEval(input);
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					cmd.setAnswer(rniCreateDataObject(objP, cmd.getCmdOption(), EVAL_MODE_FORCE));
				}
				break DATA_CMD;
			case DataCmdItem.RESOLVE_DATA:
				{	final long objP = Long.parseLong(input);
					if (objP != 0L) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						cmd.setAnswer(rniCreateDataObject(objP, cmd.getCmdOption(), EVAL_MODE_FORCE));
					}
					else {
						cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, 0x1021, "Invalid reference."));
					}
				}
				break DATA_CMD;
			case DataCmdItem.ASSIGN_DATA:
				{	if (this.rniInterrupted) {
						throw new CancellationException();
					}
					rniAssign(input, cmd.getData());
					cmd.setAnswer(RjsStatus.OK_STATUS);
				}
				break DATA_CMD;
			default:
				throw new IllegalStateException("Unsupported command.");
			}
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			return cmd;
		}
		catch (final RjsException e) {
			cmd.setAnswer(e.getStatus());
			return cmd;
		}
		catch (final CancellationException e) {
			cmd.setAnswer(RjsStatus.CANCEL_STATUS);
			return cmd;
		}
		catch (final Throwable e) {
			final String message = "Eval data failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, 0x1001, "Internal server error (see server log)."));
			return cmd;
		}
		finally {
			this.currentSlot = previousSlot;
			if (this.rniProtectedCounter > savedProtectedCounter) {
				this.rEngine.rniUnprotect(this.rniProtectedCounter - savedProtectedCounter);
				this.rniProtectedCounter = savedProtectedCounter;
			}
			this.rniMaxDepth = prevMaxDepth;
			
			if (this.rniInterrupted || this.rniEvalTempAssigned) {
				this.mainInterruptLock.lock();
				try {
					if (this.rniInterrupted) {
						try {
							Thread.sleep(10);
						}
						catch (final InterruptedException e) {
							Thread.interrupted();
						}
						this.rEngine.rniEval(this.rniP_evalDummyExpr, 0L);
						this.rniInterrupted = false;
					}
					if (this.rniEvalTempAssigned) {
						this.rEngine.rniEval(this.rniP_evalTemp_rmExpr, this.rniP_RJTempEnv);
						this.rniEvalTempAssigned = false;
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
	}
	
	private long rniEval(final String expression) throws RjsException {
		final long exprP = rniResolveExpression(expression);
		return rniEvalExpr(exprP, 0x1002);
	}
	
	/**
	 * Assigns an {@link RObject RJ R object} to an expression (e.g. symbol) in R.
	 * 
	 * @param expression an expression the R object is assigned to
	 * @param obj an R object to assign
	 * @throws RjException
	*/ 
	private void rniAssign(final String expression, final RObject obj) throws RjsException {
		if (obj == null) {
			throw new RjsException(0x1032, "The R object to assign is missing.");
		}
		long exprP = rniResolveExpression(expression);
		this.rEngine.rniProtect(exprP);
		this.rniProtectedCounter++;
		final long objP = rniAssignDataObject(obj);
		exprP = this.rEngine.rniCons(this.rniP_AssignSymbol, this.rEngine.rniCons(exprP,
				this.rEngine.rniCons(objP, this.rniP_NULL, 0, false), 0, false), 0, true);
		rniEvalExpr(exprP, 0x1033);
	}
	
	private long rniResolveExpression(final String expression) throws RjsException {
		final long exprP = this.rEngine.rniParse(expression, 1);
		if (exprP == 0L || this.rEngine.rniExpType(exprP) != REXP.EXPRSXP) {
			throw new RjsException(0x1031, "The specified expression is invalid (syntax error).");
		}
		final long[] expressionsP = this.rEngine.rniGetVector(exprP);
		if (expressionsP == null || expressionsP.length != 1 || expressionsP[0] == this.rniP_NULL) {
			throw new RjsException(0x1031, "The specified expression is invalid (not a single expression).");
		}
		return expressionsP[0];
	}
	
	private long rniEvalExpr(long exprP, final int code) throws RjsException {
		exprP = this.rEngine.rniCons(this.rniP_tryCatchFun, this.rEngine.rniCons(exprP,
				this.rniP_evalTryCatch_errorExpr, this.rniP_exprSymbol, false), 0, true);
		final long objP = this.rEngine.rniEval(exprP, 0L);
		if (objP <= 0L && objP > -4L) {
			if (this.rniInterrupted) {
				throw new CancellationException();
			}
			throw new IllegalStateException("JRI returned error code " + objP + " (pointer = 0x" + Long.toHexString(exprP) + ")");
		}
		this.rEngine.rniProtect(objP);
		this.rniProtectedCounter++;
		if (this.rEngine.rniExpType(objP) == REXP.RAWSXP) {
			final long classP = this.rEngine.rniGetAttr(objP, "class");
			if (classP != 0
					&& ".rj.eval.error".equals(this.rEngine.rniGetString(classP))) {
				String message = null;
				final long outputP = this.rEngine.rniGetAttr(objP, "output");
				if (outputP != 0L) {
					message = this.rEngine.rniGetString(outputP);
				}
				if (message == null) {
					message = "<no information available>";
				}
				switch (code) {
				case 0x1002:
					message = "An error occurred when evaluation the specified expression in R " + message + ".";
					break;
				case 0x1033:
					message = "An error occurred when assigning the value to the specified expression in R" + message + ".";
					break;
				case 0x1038:
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
	 * Put an {@link RObject RJ R object} into JRI, and get back the pointer to the object.
	 * 
	 * @param obj an R object
	 * @return long R pointer
	 * @throws RjsException 
	 */ 
	private long rniAssignDataObject(final RObject obj) throws RjsException {
		RStore names;
		long objP;
		switch(obj.getRObjectType()) {
		case RObject.TYPE_NULL:
		case RObject.TYPE_MISSING:
			return this.rniP_NULL;
		case RObject.TYPE_VECTOR:
			objP = rniAssignDataStore(obj.getData());
			return objP;
		case RObject.TYPE_ARRAY:
			objP = rniAssignDataStore(obj.getData());
			this.rEngine.rniSetAttr(objP, "dim",
					this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
			return objP;
		case RObject.TYPE_DATAFRAME: {
			final JRIDataFrameImpl list = (JRIDataFrameImpl) obj;
			final int length = list.getLength();
			final long[] itemPs = new long[length];
			for (int i = 0; i < length; i++) {
				itemPs[i] = rniAssignDataStore(list.getColumn(i));
			}
			objP = this.rEngine.rniPutVector(itemPs);
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			names = list.getNames();
			if (names != null) {
				this.rEngine.rniSetAttr(objP, "names", rniAssignDataStore(names));
			}
			names = list.getRowNames();
			if (names != null) {
				this.rEngine.rniSetAttr(objP, "row.names", rniAssignDataStore(names));
			}
			else {
				final int[] rownames = new int[list.getRowCount()];
				for (int i = 0; i < rownames.length; ) {
					rownames[i] = ++i;
				}
				this.rEngine.rniSetAttr(objP, "row.names", this.rEngine.rniPutIntArray(rownames));
			}
			this.rEngine.rniSetAttr(objP, "class", this.rniP_dataframeClassString);
			return objP; }
		case RObject.TYPE_LIST: {
			final JRIListImpl list = (JRIListImpl) obj;
			final int length = list.getLength();
			final long[] itemPs = new long[length];
			for (int i = 0; i < length; i++) {
				itemPs[i] = rniAssignDataObject(list.get(i));
			}
			objP = this.rEngine.rniPutVector(itemPs);
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			names = list.getNames();
			if (names != null) {
				this.rEngine.rniSetAttr(objP, "names", rniAssignDataStore(names));
			}
			return objP; }
		case RObject.TYPE_REFERENCE:
			return ((RReference) obj).getHandle();
		case RObject.TYPE_S4OBJECT: {
			final RS4Object s4obj = (RS4Object) obj;
			objP = this.rniP_NULL;
			for (int i = s4obj.getLength()-1; i >= 0; i--) {
				final RObject slotObj = s4obj.get(i);
				if (slotObj != null && slotObj.getRObjectType() != RObject.TYPE_MISSING) {
					objP = this.rEngine.rniCons(rniAssignDataObject(slotObj), objP,
							this.rEngine.rniInstallSymbol(s4obj.getName(i)), false);
					this.rEngine.rniProtect(objP);
					this.rniProtectedCounter++;
				}
			}
			objP = rniEvalExpr(this.rEngine.rniCons(this.rniP_newSymbol, this.rEngine.rniCons(
					this.rEngine.rniPutString(s4obj.getRClassName()), objP, this.rniP_ClassSymbol, false),
					0L, true), 0x1038);
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP; }
		default:
			throw new RjsException(0x1037, "The assignment for R objects of type " + RDataUtil.getObjectTypeName(obj.getRObjectType()) + " is not yet supported.");
		}
	}
	
	private long rniAssignDataStore(final RStore data) {
		long objP;
		switch (data.getStoreType()) {
		case RStore.LOGICAL:
			objP = this.rEngine.rniPutBoolArrayI(
					((JRILogicalDataImpl) data).getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP;
		case RStore.INTEGER:
			objP = this.rEngine.rniPutIntArray(
					((JRIIntegerDataImpl) data).getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP;
		case RStore.NUMERIC:
			objP = this.rEngine.rniPutDoubleArray(
					((JRINumericDataImpl) data).getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP;
		case RStore.COMPLEX: {
			final JRIComplexDataImpl complex = (JRIComplexDataImpl) data;
			objP = this.rEngine.rniPutDoubleArray(complex.getJRIRealValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			final long iP = this.rEngine.rniPutDoubleArray(complex.getJRIImaginaryValueArray());
			this.rEngine.rniProtect(iP);
			this.rniProtectedCounter++;
			objP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_complexFun,
					this.rEngine.rniCons(objP, this.rEngine.rniCons(iP, this.rniP_NULL,
							this.rniP_imaginarySymbol, false), this.rniP_realSymbol, false),
							0L, true),
					this.rniP_BaseEnv);
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP; }
		case RStore.CHARACTER:
			objP = this.rEngine.rniPutStringArray(
					((JRICharacterDataImpl) data).getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP;
		case RStore.RAW:
			objP = this.rEngine.rniPutRawArray(
					((JRIRawDataImpl) data).getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			return objP;
		case RStore.FACTOR: {
			final JRIFactorDataImpl factor = (JRIFactorDataImpl) data;
			objP = this.rEngine.rniPutIntArray(factor.getJRIValueArray());
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			this.rEngine.rniSetAttr(objP, "levels",
					this.rEngine.rniPutStringArray(factor.getJRILevelsArray()));
			this.rEngine.rniSetAttr(objP, "class",
					factor.isOrdered() ? this.rniP_orderedClassString : this.rniP_factorClassString);
			return objP; }
		default:
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Returns {@link RObject RJ R object} for the given R pointer.
	 * 
	 * @param objP a valid pointer to an object in R
	 * @param objTmp an optional R expression pointing to the same object in R
	 * @param flags to configure the data to create
	 * @param force forces the creation of the object (ignoring the depth etc.)
	 * @return new created R object
	 */ 
	private RObject rniCreateDataObject(final long objP, final int flags, final byte mode) {
		if (mode == EVAL_MODE_DEFAULT && (this.rniTemp > 512 || this.rniTemp >= this.rniMaxDepth)) {
			return null;
		}
		this.rniTemp++;
		try {
			final int rType = this.rEngine.rniExpType(objP);
			switch (rType) {
			case REXP.NILSXP:
				return RNull.INSTANCE;
			
			case REXP.LGLSXP: { // logical vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.INTSXP: { // integer vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				if (className1 != null
						&& (className1.equals("factor") || this.rEngine.rniInherits(objP, "factor")) ) {
					final String[] levels;
					{	final long levelsP = this.rEngine.rniGetAttr(objP, "levels");
						levels = (levelsP != 0L) ? this.rEngine.rniGetStringArray(levelsP) : null;
					}
					if (levels != null) {
						final boolean isOrdered = className1.equals("ordered") || this.rEngine.rniInherits(objP, "ordered");
						final RFactorStore factorData = ((flags & F_ONLY_STRUCT) != 0) ?
								new RFactorDataStruct(isOrdered, levels.length) :
								new RFactorDataImpl(this.rEngine.rniGetIntArray(objP), isOrdered, levels);
						return ((flags & F_ONLY_STRUCT) != 0) ?
								new JRIVectorImpl<RIntegerStore>(
										factorData,
										rniGetLength(objP), className1, null) :
								new JRIVectorImpl<RIntegerStore>(
										factorData,
										className1, rniGetNames(objP));
					}
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.REALSXP: { // numeric vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.CPLXSXP: { // complex vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
						new JRIArrayImpl<RComplexStore>(
								RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
								className1, dim) :
						new JRIArrayImpl<RComplexStore>(
								new JRIComplexDataImpl(rniGetComplexRe(objP), rniGetComplexIm(objP)),
								className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RComplexStore>(
									RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RComplexStore>(
									new JRIComplexDataImpl(rniGetComplexRe(objP), rniGetComplexIm(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.STRSXP: { // character vector / array
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.RAWSXP: { // raw/byte vector
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIArrayImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, dim, rniGetDimNames(objP, dim.length));
				}
				else {
					return ((flags & F_ONLY_STRUCT) != 0) ?
							new JRIVectorImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									rniGetLength(objP), className1, null) :
							new JRIVectorImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.VECSXP: { // generic vector / list
				final String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				else {
					className1 = null;
				}
				
				final String[] itemNames;
				{	final long namesP = this.rEngine.rniGetAttr(objP, "names");
					itemNames = (namesP != 0L) ? this.rEngine.rniGetStringArray(namesP) : null;
				}
				
				final long[] itemP = this.rEngine.rniGetVector(objP);
				final RObject[] itemObjects = new RObject[itemP.length];
				DATA_FRAME: if (itemNames != null && className1 != null &&
						(className1.equals("data.frame") || this.rEngine.rniInherits(objP, "data.frame")) ) {
					int length = -1;
					for (int i = 0; i < itemP.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
						itemObjects[i] = rniCreateDataObject(itemP[i], flags, EVAL_MODE_FORCE);
						if (itemObjects[i] == null || itemObjects[i].getRObjectType() != RObject.TYPE_VECTOR) {
							break DATA_FRAME;
						}
						else if (length == -1) {
							length = itemObjects[i].getLength();
						}
						else if (length != itemObjects[i].getLength()){
							break DATA_FRAME;
						}
					}
					final String[] rowNames = ((flags & F_ONLY_STRUCT) != 0) ? null : rniGetRowNames(objP);
					if (rowNames != null && length != -1 && rowNames.length != length) {
						break DATA_FRAME;
					}
					return new JRIDataFrameImpl(itemObjects, className1, itemNames, rowNames);
				}
				if ((flags & F_ONLY_STRUCT) != 0 && itemP.length > this.rniListsMaxLength) {
					return new JRIListImpl(itemP.length, className1, itemNames);
				}
				for (int i = 0; i < itemP.length; i++) {
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					itemObjects[i] = rniCreateDataObject(itemP[i], flags, EVAL_MODE_DEFAULT);
				}
				return new JRIListImpl(itemObjects, className1, itemNames);
			}
			case REXP.LISTSXP:   // pairlist
			/*case REXP.LANGSXP: */{
				String className1;
				if (mode != EVAL_MODE_DATASLOT) {
					final long classP = this.rEngine.rniGetAttr(objP, "class");
					if (classP == 0L
							|| (className1 = this.rEngine.rniGetString(classP)) == null) {
						className1 = RObject.CLASSNAME_PAIRLIST;
					}
				}
				else {
					className1 = RObject.CLASSNAME_PAIRLIST;
				}
				
				long cdr = objP;
				final int length = rniGetLength(objP);
				final String[] itemNames = new String[length];
				final RObject[] itemObjects = new RObject[length];
				for (int i = 0; i < length; i++) {
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					final long car = this.rEngine.rniCAR(cdr);
					final long tag = this.rEngine.rniTAG(cdr);
					itemNames[i] = (tag != 0L) ? this.rEngine.rniGetSymbolName(tag) : null;
					itemObjects[i] = rniCreateDataObject(car, flags, EVAL_MODE_DEFAULT);
					cdr = this.rEngine.rniCDR(cdr);
					if (cdr == 0L || this.rEngine.rniExpType(cdr) != REXP.LISTSXP) {
						break;
					}
				}
				return new JRIListImpl(itemObjects, className1, itemNames);
			}
			case REXP.ENVSXP: {
				if (this.rniTemp > 1 && (flags & 0x8) == 0) {
					return new RReferenceImpl(objP, RObject.TYPE_REFERENCE, "environment");
				}
				final String[] names = this.rEngine.rniGetStringArray(this.rEngine.rniListEnv(objP, true));
				if (names != null) {
					final String className1;
					if (mode != EVAL_MODE_DATASLOT) {
						final long classP = this.rEngine.rniGetAttr(objP, "class");
						className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
					}
					else {
						className1 = null;
					}
					
					// env name
					final long nameP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_envNameFun,
							this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_envSymbol, false), 0L, true),
							this.rniP_BaseEnv);
					String name;
					if ((nameP <= 0L && nameP >= -4L)
							|| (name = this.rEngine.rniGetString(nameP)) == null) {
						name = "";
					}
					
					if ((objP == this.rniP_AutoloadEnv)
							|| names.length > this.rniEnvsMaxLength) {
						return new JRIEnvironmentImpl(name, objP, null, null, names.length, className1);
					}
					
					final RObject[] itemObjects = new RObject[names.length];
					for (int i = 0; i < names.length; i++) {
						if (this.rniInterrupted) {
							throw new CancellationException();
						}
//						final long itemPa = this.rEngine.rniFindVar(names[i], objP);
						final long itemP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_ItemFun,
								this.rEngine.rniCons(objP, this.rEngine.rniCons(this.rEngine.rniInstallSymbol(names[i]),
										this.rniP_NULL, 0L, false), 0L, false), 0L, true),
								this.rniP_BaseEnv);
						if (itemP > 0L || itemP < -4L) {
							itemObjects[i] = rniCreateDataObject(itemP, flags, EVAL_MODE_DEFAULT);
							continue;
						}
						else {
							itemObjects[i] = RMissing.INSTANCE;
							continue;
						}
					}
					return new JRIEnvironmentImpl(name, objP, itemObjects, names, names.length, className1);
				}
				break;
			}
			case REXP.CLOSXP: {
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
				}
				
				final String header = rniGetFHeader(objP);
				return RFunctionImpl.createForServer(header);
			}
			case REXP.SPECIALSXP:
			case REXP.BUILTINSXP: {
				final String header = rniGetFHeader(objP);
				return RFunctionImpl.createForServer(header);
			}
			case REXP.S4SXP: {
				if (mode != EVAL_MODE_DATASLOT) {
					final RObject s4Obj = rniCheckAndCreateS4Obj(objP, flags);
					if (s4Obj != null) {
						return s4Obj;
					}
				}
				break;
			}
			case REXP.SYMSXP: {
				String className1;
				{	final long classP;
					if (mode == EVAL_MODE_DATASLOT
							|| (classP = this.rEngine.rniGetAttr(objP, "class")) == 0L
							|| (className1 = this.rEngine.rniGetString(classP)) == null ) {
						className1 = "name";
					}
				}
				return new ROtherImpl(className1);
			}
			case REXP.EXPRSXP: {
				String className1;
				{	final long classP;
					if (mode == EVAL_MODE_DATASLOT
							|| (classP = this.rEngine.rniGetAttr(objP, "class")) == 0L
							|| (className1 = this.rEngine.rniGetString(classP)) == null ) {
						className1 = "expression";
					}
				}
				return new ROtherImpl(className1);
			}
			case REXP.EXTPTRSXP: {
				String className1;
				{	final long classP;
					if (mode == EVAL_MODE_DATASLOT
							|| (classP = this.rEngine.rniGetAttr(objP, "class")) == 0L
							|| (className1 = this.rEngine.rniGetString(classP)) == null ) {
						className1 = "externalptr";
					}
				}
				return new ROtherImpl(className1);
			}
			}
			
//				final long classP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_classFun,
//						this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true),
//						this.rniP_BaseEnv);
			{	this.rEngine.rniAssign(EVAL_TEMP_SYMBOL, objP, this.rniP_RJTempEnv);
				this.rniEvalTempAssigned = true;
				
				String className1;
				final long classP = this.rEngine.rniEval(this.rniP_evalTemp_classExpr, this.rniP_RJTempEnv);
				if ((classP <= 0L && classP >= -4L)
						|| (className1 = this.rEngine.rniGetString(classP)) == null) {
					className1 = "<unknown>";
				}
				
//				System.out.println(this.rEngine.rniExpType(objP));
//				System.out.println(this.rEngine.rniGetAttr(objP, "class"));
//				System.out.println(className1);
//				System.out.println();
				
				return new ROtherImpl(className1);
			}
		}
		finally {
			this.rniTemp--;
		}
	}
	
	private RObject rniCheckAndCreateS4Obj(final long objP, final int flags) {
		final long classP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_s4classFun,
				this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true),
				0L);
		final String className;
		if ((classP > 0L || classP < -4L) && classP != this.rniP_NULL
				&& (className = this.rEngine.rniGetString(classP)) != null) {
			final long slotNamesP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_slotNamesFun,
					this.rEngine.rniCons(classP, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true),
					0L);
			final String[] slotNames = (slotNamesP > 0L || slotNamesP < -4L) ? this.rEngine.rniGetStringArray(slotNamesP) : null;
			if (slotNames != null && slotNames.length > 0) {
				final RObject[] slotValues = new RObject[slotNames.length];
				for (int i = 0; i < slotNames.length; i++) {
					if (this.rniInterrupted) {
						throw new CancellationException();
					}
					if (".Data".equals(slotNames[i])) {
						slotValues[i] = rniCreateDataObject(objP, flags, EVAL_MODE_DATASLOT);
						continue;
					}
					else {
						final long slotValueP = this.rEngine.rniGetAttr(objP, slotNames[i]);
						if (slotValueP > 0L || slotValueP < -4L) {
							slotValues[i] = rniCreateDataObject(slotValueP, flags, EVAL_MODE_FORCE);
							continue;
						}
						else {
							slotValues[i] = RMissing.INSTANCE;
							continue;
						}
					}
				}
				return new RS4ObjectImpl(className, slotNames, slotValues);
			}
			else {
				return new RS4ObjectImpl(className, EMPTY_STRING_ARRAY, EMPTY_ROBJECT_ARRAY);
			}
		}
		return null;
	}
	
	private String[] rniGetNames(final long objP) {
		final long namesP = this.rEngine.rniGetAttr(objP, "names");
		if (namesP != 0L) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private SimpleRListImpl<RStore> rniGetDimNames(final long objP, final int length) {
		final long namesP = this.rEngine.rniGetAttr(objP, "dimnames");
		if (namesP != 0L) {
			final long[] names1P = this.rEngine.rniGetVector(namesP);
			if (names1P != null && names1P.length == length) {
				String[] s = rniGetNames(namesP);
				final RCharacterStore names0 = (s != null) ? new RCharacterDataImpl(s) :
					new RCharacterDataImpl(names1P.length);
				final RCharacterStore[] names1 = new RCharacterStore[names1P.length];
				for (int i = 0; i < names1P.length; i++) {
					s = this.rEngine.rniGetStringArray(names1P[i]);
					if (s != null) {
						names1[i] = new RCharacterDataImpl(s);
					}
				}
				return new SimpleRListImpl<RStore>(names0, names1);
			}
		}
		return null;
	}
	
	private String[] rniGetRowNames(final long objP) {
		final long namesP = this.rEngine.rniGetAttr(objP, "row.names");
		if (namesP != 0L) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private int rniGetLength(final long objP) {
		final int[] length;
		final long lengthP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_lengthFun,
				this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true),
				this.rniP_BaseEnv);
		if ((lengthP > 0L || lengthP < -4L)
				&& (length = this.rEngine.rniGetIntArray(lengthP)) != null && length.length == 1) {
			return length[0];
		}
		return 0;
	}
	
	private double[] rniGetComplexRe(final long objP) {
		final double[] num;
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_ReFun,
				this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_zSymbol, false), 0L, true),
				this.rniP_BaseEnv);
		if (numP <= 0L && numP > -4L) {
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
	
	private double[] rniGetComplexIm(final long objP) {
		final double[] num;
		final long numP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_ImFun,
				this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_zSymbol, false), 0L, true),
				this.rniP_BaseEnv);
		if (numP <= 0L && numP > -4L) {
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
	
	private String rniGetFHeader(final long objP) {
		final long argsP = this.rEngine.rniEval(this.rEngine.rniCons(this.rniP_headerFun, 
				this.rEngine.rniCons(objP, this.rniP_NULL, this.rniP_xSymbol, false), 0L, true),
				this.rniP_BaseEnv);
		final String args;
		if ((argsP > 0L || argsP < -4L)
				&& (args = this.rEngine.rniGetString(argsP)) != null && args.length() >= 11) { // "function ()".length
//			return args.substring(9,);
			return args;
		}
		return null;
	}
	
	
	public String rReadConsole(final Rengine engine, final String prompt, final int addToHistory) {
		final MainCmdItem cmd = internalMainFromR(new ConsoleReadCmdItem(
				(addToHistory == 1) ? V_TRUE : V_FALSE, prompt));
		if (cmd.isOK()) {
			return cmd.getDataText();
		}
		return "\n";
	}
	
	public void rWriteConsole(final Rengine engine, final String text, final int type) {
//		try {
//			Thread.sleep(100);
//		}
//		catch (final InterruptedException e) {
//		}
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
						this.mainLoopStdOutSingle.getChars(0, this.mainLoopStdOutSingle.length(), this.mainLoopStdOutBuffer, 0);
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
	
	public void rFlushConsole(final Rengine engine) {
		internalMainFromR(null);
	}
	
	public void rBusy(final Rengine engine, final int which) {
		this.mainLoopBusyAtServer = (which == 1);
		internalMainFromR(null);
	}
	
	public void rShowMessage(final Rengine engine, final String message) {
		internalMainFromR(new ConsoleMessageCmdItem(message));
	}
	
	public String rChooseFile(final Rengine engine, final int newFile) {
		final Map<String, Object> answer = execUICommand(ExtUICmdItem.C_CHOOSE_FILE,
				Collections.singletonMap("newResource", (Object) (newFile == 1)), true);
		if (answer != null) {
			return (String) answer.get("filename");
		}
		else {
			return null;
		}
	}
	
	public void rLoadHistory(final Rengine engine, final String filename) {
		execUICommand(ExtUICmdItem.C_LOAD_HISTORY,
				Collections.singletonMap("filename", (Object) filename), true);
	}
	
	public void rSaveHistory(final Rengine engine, final String filename) {
		execUICommand(ExtUICmdItem.C_SAVE_HISTORY,
				Collections.singletonMap("filename", (Object) filename), true);
	}
	
	private void internalRStopped() {
		this.mainExchangeLock.lock();
		try {
			if (this.mainLoopState == ENGINE_STOPPED) {
				return;
			}
			this.mainLoopState = ENGINE_STOPPED;
			
			this.mainExchangeClient.signalAll();
			while (this.mainLoopS2CNextCommandsFirst != null || this.mainLoopStdOutSize > 0) {
				try {
					this.mainExchangeR.awaitNanos(100000000L);
				}
				catch (final InterruptedException e) {
					Thread.interrupted();
				}
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
	public Map<String, Object> execUICommand(final String command, final Map<String, Object> args, final boolean wait) {
		if (command == null) {
			throw new NullPointerException("command");
		}
		final MainCmdItem answer = internalMainFromR(new ExtUICmdItem(command, 0, args, wait));
		if (wait && answer instanceof ExtUICmdItem && answer.isOK()) {
			return ((ExtUICmdItem) answer).getDataMap();
		}
		return null;
	}
	
	@Override
	public void registerGraphic(final RjsGraphic graphic) {
		this.graphicLast = graphic;
		this.graphicList.add(graphic);
		graphic.setSlot(this.currentSlot);
	}
	
	public void initLastGraphic(final int devId, final String target) {
		if (this.graphicLast == null
				|| (this.graphicLast.getDevId() > 0 && this.graphicLast.getDevId() != devId) ) {
			return;
		}
		this.graphicLast.deferredInit(devId, target);
		this.graphicLast = null;
	}
	
	@Override
	public void unregisterGraphic(final RjsGraphic graphic) {
		this.graphicList.remove(graphic);
	}
	
	@Override
	public double[] execGDCommand(final GDCmdItem cmd) {
		final MainCmdItem answer = internalMainFromR(cmd);
		if (cmd.waitForClient() && answer instanceof GDCmdItem
				&& answer.isOK()) {
			return ((GDCmdItem) answer).getData();
		}
		return null;
	}
	
}
