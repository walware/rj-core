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

import static de.walware.rj.server.RjsComObject.T_PING;
import static de.walware.rj.server.RjsComObject.V_ERROR;
import static de.walware.rj.server.RjsComObject.V_FALSE;
import static de.walware.rj.server.RjsComObject.V_OK;
import static de.walware.rj.server.RjsComObject.V_TRUE;
import static de.walware.rj.server.Server.S_CONNECTED;
import static de.walware.rj.server.Server.S_DISCONNECTED;
import static de.walware.rj.server.Server.S_NOT_STARTED;
import static de.walware.rj.server.Server.S_STOPPED;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.RComplexDataBImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataStruct;
import de.walware.rj.data.defaultImpl.RFunctionImpl;
import de.walware.rj.data.defaultImpl.RNull;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.data.defaultImpl.ROtherImpl;
import de.walware.rj.data.defaultImpl.RReferenceImpl;
import de.walware.rj.data.defaultImpl.RS4ObjectImpl;
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
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.jri");
	
	private static final int STDOUT_BUFFER_SIZE = 0x1FFF;
	
	private static final long REQUIRED_JRI_API = 0x0109;
	
	
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
		public void rShowMessage(final Rengine engine, final String message) {
			initEngine(engine);
			RosudaJRIServer.this.rShowMessage(engine, message);
		}
		public void rLoadHistory(final Rengine engine, final String filename) {
			initEngine(engine);
			RosudaJRIServer.this.rLoadHistory(engine, filename);
		}
		public void rSaveHistory(final Rengine engine, final String filename) {
			initEngine(engine);
			RosudaJRIServer.this.rSaveHistory(engine, filename);
		}
		public String rChooseFile(final Rengine engine, final int newFile) {
			initEngine(engine);
			return RosudaJRIServer.this.rChooseFile(engine, newFile);
		}
		public void rBusy(final Rengine engine, final int which) {
			initEngine(engine);
			RosudaJRIServer.this.rBusy(engine, which);
		}
	}
	
	
	private String name;
	
	private Server publicServer;
	private JRClassLoader rClassLoader;
	private Rengine rEngine;
	
	private final Object mainLoopLock = new Object();
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
	
	private Client client0;
	private ConsoleEngine client0Engine;
	private ConsoleEngine client0ExpRef;
	private ConsoleEngine client0PrevExpRef;
	
	private byte currentSlot;
	
	private final Object pluginsLock = new Object();
	private ServerRuntimePlugin[] pluginsList = new ServerRuntimePlugin[0];
	
	private int rniTemp;
	private int rniMaxDepth;
	private boolean rniInterrupted;
	private int rniProtectedCounter;
	private int rniListsMaxLength = 10000;
	private int rniEnvsMaxLength = 10000;
	
	private long rniP_NULL;
	private long rniP_Unbound;
	private long rniP_factorClass;
	private long rniP_orderedClass;
	private long rniP_dataframeClass;
	
	private RjsGraphic graphicLast;
	private final List<RjsGraphic> graphicList = new ArrayList<RjsGraphic>();
	
	
	public RosudaJRIServer() {
		this.mainLoopState = ENGINE_NOT_STARTED;
		this.mainLoopClient0State = CLIENT_NONE;
		
		this.serverState = S_NOT_STARTED;
	}
	
	
	public void init(final String name, final Server publicServer) throws Exception {
		this.name = name;
		this.publicServer = publicServer;
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
		LOGGER.log(Level.SEVERE, "[Plugins] An error occured in plug-in '"+plugin.getSymbolicName()+"', plug-in will be disabled.", error);
		removePlugin(plugin);
		try {
			plugin.rjStop(V_ERROR);
		}
		catch (final Throwable stopError) {
			LOGGER.log(Level.WARNING, "[Plugins] An error occured when trying to disable plug-in '"+plugin.getSymbolicName()+"'.", error);
		}
	}
	
	
	private void disconnectClient() {
		this.serverState = S_DISCONNECTED;
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
	
	public void unreferenced() {
		boolean disconnected = false;
		synchronized (this.mainLoopLock) {
			if (this.mainLoopClient0State != CLIENT_NONE) {
				this.mainLoopClient0State = CLIENT_NONE;
				disconnectClient();
				disconnected = true;
			}
		}
		if (disconnected) {
			LOGGER.log(Level.INFO, "R engine is no longer referenced by a client. New Client-State: 'Disconnected'.");
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
		return this.serverState;
	}
	
	public synchronized ConsoleEngine start(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		assert (client.slot == 0);
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
				final String message = "Unsupported JRI version (API found: " + Long.toHexString(Rengine.getVersion()) + ", required: " + Long.toHexString(REQUIRED_JRI_API) + ").";
				LOGGER.log(Level.SEVERE, message);
				internalRStopped();
				throw new RjInitFailedException(message);
			}
			
			this.mainLoopState = ENGINE_RUN_IN_R;
			final Rengine engine = new Rengine(checkArgs((String[]) properties.get("args")), true, new InitCallbacks());
			
			while (this.rEngine != engine) {
				Thread.sleep(100);
			}
			if (!engine.waitForR()) {
				internalRStopped();
				throw new IllegalThreadStateException("R thread not started");
			}
			
			synchronized (this.mainLoopLock) {
				this.mainLoopS2CAnswerFail = 0;
				this.mainLoopClient0State = CLIENT_OK;
			}
			
			setProperties(client.slot, properties, true);
			
			LOGGER.log(Level.INFO, "R engine started successfully. New Client-State: 'Connected'.");
			
			this.client0 = client;
			this.client0Engine = consoleEngine;
			this.client0ExpRef = export;
			DefaultServerImpl.addClient(export);
			this.serverState = S_CONNECTED;
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
	
	public synchronized void setProperties(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		checkClient(client);
		setProperties(client.slot, properties, false);
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
			{	final Object id = properties.get(RjsComConfig.RJ_COM_S2C_ID_PROPERTY_ID);
				if (id instanceof Integer) {
					this.mainLoopS2CLastCommands[slot].setId(((Integer) id).intValue());
				}
				else {
					this.mainLoopS2CLastCommands[slot].setId(0);
				}
			}
		}
	}
	
	private void initEngine(final Rengine engine) {
		this.rEngine = engine;
		this.rEngine.setContextClassLoader(this.rClassLoader);
		
		this.rniP_NULL = this.rEngine.rniSpecialObject(Rengine.SO_NilValue);
		this.rniP_Unbound = this.rEngine.rniSpecialObject(Rengine.SO_UnboundValue);
		this.rniP_orderedClass = this.rEngine.rniPutStringArray(new String[] { "ordered", "factor" });
		this.rEngine.rniPreserve(this.rniP_orderedClass);
		this.rniP_factorClass = this.rEngine.rniPutString("factor");
		this.rEngine.rniPreserve(this.rniP_factorClass);
		this.rniP_dataframeClass = this.rEngine.rniPutString("data.frame");
		this.rEngine.rniPreserve(this.rniP_dataframeClass);
		
		RjsComConfig.setDefaultRObjectFactory(new JRIObjectFactory());
		
		this.rEngine.addMainLoopCallbacks(RosudaJRIServer.this);
	}
	
	private String[] checkArgs(final String[] args) {
		final List<String> checked = new ArrayList<String>(args.length+1);
		boolean saveState = false;
		for (final String arg : args) {
			if (arg != null && arg.length() > 0) {
				// add other checks here
				if (arg.equals("--save") || arg.equals("--no-save") || arg.equals("--vanilla")) {
					saveState = true;
				}
				checked.add(arg);
			}
		}
		if (!saveState) {
			checked.add("--no-save");
		}
		return checked.toArray(new String[checked.size()]);
	}
	
	public synchronized ConsoleEngine connect(final Client client, final Map<String, ? extends Object> properties) throws RemoteException {
		assert (client.slot == 0);
		final ConsoleEngine consoleEngine = new ConsoleEngineImpl(this.publicServer, this, client);
		final ConsoleEngine export = (ConsoleEngine) UnicastRemoteObject.exportObject(consoleEngine, 0);
		
		try {
			synchronized (this.mainLoopLock) {
				this.mainLoopS2CAnswerFail = 0;
				switch (this.mainLoopState) {
				case ENGINE_WAIT_FOR_CLIENT:
				case ENGINE_RUN_IN_R:
					// exit old client
					if (this.mainLoopClient0State == CLIENT_OK_WAIT) {
						this.mainLoopClient0State = CLIENT_CANCEL;
						this.mainLoopLock.notifyAll();
						while (this.mainLoopClient0State == CLIENT_CANCEL) {
							try {
								this.mainLoopLock.wait(100);
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
					
					this.client0 = client;
					this.client0Engine = consoleEngine;
					this.client0ExpRef = export;
					DefaultServerImpl.addClient(export);
					return export;
				default:
					throw new IllegalStateException("R engine is not running.");
				}
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
	}
	
	public synchronized void disconnect(final Client client) throws RemoteException {
		synchronized (this.mainLoopLock) {
			if (client != null) {
				checkClient(client);
			}
			if (this.mainLoopClient0State == CLIENT_OK_WAIT) {
				// exit old client
				this.mainLoopClient0State = CLIENT_CANCEL;
				while (this.mainLoopClient0State == CLIENT_CANCEL) {
					this.mainLoopLock.notifyAll();
					try {
						this.mainLoopLock.wait(100);
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
			disconnectClient();
		}
	}
	
	public void interrupt(final Client client) throws RemoteException {
		try {
			synchronized(this.mainLoopLock) {
				if (client != null) {
					checkClient(client);
				}
				this.rniInterrupted = true;
				this.rEngine.rniStop(1);
			}
		}
		catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "An error occurred when trying to interrupt the R engine.", e);
		}
		return;
	}
	
	public RjsComObject runMainLoop(final Client client, final RjsComObject command) throws RemoteException {
		this.mainLoopS2CLastCommands[client.slot].clear();
		if (command == null) {
			return internalMainCallbackFromClient(client, null);
		}
		else {
			switch (command.getComType()) {
			case T_PING:
				return RjsStatus.OK_STATUS;
			case RjsComObject.T_MAIN_LIST:
				return internalMainCallbackFromClient(client, (MainCmdC2SList) command);
			case RjsComObject.T_FILE_EXCHANGE:
				return command;
			default:
				throw new IllegalArgumentException("Unknown command: " + "0x"+Integer.toHexString(command.getComType()) + ".");
			}
		}
	}
	
	
	public RjsComObject runAsync(final Client client, final RjsComObject command) throws RemoteException {
		if (command == null) {
			throw new IllegalArgumentException("Missing command.");
		}
		switch (command.getComType()) {
		case T_PING:
			return internalPing();
		case RjsComObject.T_FILE_EXCHANGE:
			return command;
		default:
			throw new IllegalArgumentException("Unknown command: " + "0x"+Integer.toHexString(command.getComType()) + ".");
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
	
	private RjsComObject internalMainCallbackFromClient(final Client client, final MainCmdC2SList mainC2SCmdList) throws RemoteException {
		if (client.slot > 0 && mainC2SCmdList != null) {
			MainCmdItem item = mainC2SCmdList.getItems();
			while (item != null) {
				item.slot = client.slot;
				item = item.next;
			}
		}
		synchronized (this.mainLoopLock) {
//			System.out.println("fromClient 1: " + mainC2SCmdList);
//			System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//			System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
			
			checkClient(client);
			if (client.slot == 0 && this.mainLoopClient0State != CLIENT_OK) {
				return new RjsStatus(RjsStatus.WARNING, S_DISCONNECTED);
			}
			if (this.mainLoopState == ENGINE_WAIT_FOR_CLIENT) {
				if (mainC2SCmdList == null && this.mainLoopS2CNextCommandsFirst[client.slot] == null) {
					if (client.slot == 0) {
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
						return new RjsStatus(RjsStatus.INFO, RjsStatus.CANCEL);
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
			
			this.mainLoopLock.notifyAll();
			this.mainLoopClient0State = CLIENT_OK_WAIT;
			while (this.mainLoopS2CNextCommandsFirst[client.slot] == null
//					&& (this.mainLoopState != ENGINE_STOPPED)
					&& (this.mainLoopState == ENGINE_RUN_IN_R || this.mainLoopC2SCommandFirst != null)
					&& ((client.slot > 0)
							|| ( (this.mainLoopClient0State == CLIENT_OK_WAIT)
								&& (this.mainLoopStdOutSize == 0)
								&& (this.mainLoopBusyAtClient == this.mainLoopBusyAtServer) )
					)) {
				this.mainLoopClientListen++;
				try {
					this.mainLoopLock.wait(); // run in R
				}
				catch (final InterruptedException e) {
					Thread.interrupted();
				}
				finally {
					this.mainLoopClientListen--;
				}
			}
			if (this.mainLoopClient0State == CLIENT_OK_WAIT) {
				this.mainLoopClient0State = CLIENT_OK;
			}
			
//			System.out.println("fromClient 3: " + mainC2SCmdList);
//			System.out.println("C2S: " + this.mainLoopC2SCommandFirst);
//			System.out.println("S2C: " + this.mainLoopS2CNextCommandsFirst[1]);
			
			// answer
			if (client.slot > 0 || this.mainLoopClient0State == CLIENT_OK) {
				if (this.mainLoopStdOutSize > 0) {
					internalClearStdOutBuffer();
					this.mainLoopStdOutSize = 0;
				}
				if (this.mainLoopState == ENGINE_STOPPED && this.mainLoopS2CNextCommandsFirst[client.slot] == null) {
					return new RjsStatus(RjsStatus.INFO, S_STOPPED);
				}
				this.mainLoopBusyAtClient = this.mainLoopBusyAtServer;
				this.mainLoopS2CLastCommands[client.slot].setBusy(this.mainLoopBusyAtClient);
				this.mainLoopS2CLastCommands[client.slot].setObjects(this.mainLoopS2CNextCommandsFirst[client.slot]);
				this.mainLoopS2CNextCommandsFirst[client.slot] = null;
				return this.mainLoopS2CLastCommands[client.slot];
			}
			else {
				this.mainLoopClient0State = CLIENT_NONE;
				return new RjsStatus(RjsStatus.CANCEL, S_DISCONNECTED); 
			}
		}
	}
	
	private MainCmdItem internalMainFromR(final MainCmdItem initialItem) {
		MainCmdItem item = initialItem;
		boolean initial = true;
		while (true) {
			synchronized (this.mainLoopLock) {
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
				
				this.mainLoopLock.notifyAll();
				
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
								try {
									for (; i < plugins.length; i++) {
										plugins[i].rjIdle();
									}
									
									this.rEngine.rniIdle();
									
									this.mainLoopLock.wait(50);
								}
								catch (final InterruptedException e) {
									Thread.interrupted();
								}
								catch (final Throwable e) {
									if (i < plugins.length) {
										handlePluginError(plugins[i], e);
									}
								}
							}
						}
						else {
							// TODO log warning
							while (this.mainLoopC2SCommandFirst == null || this.mainLoopServerStack > stackId) {
								try {
									this.mainLoopLock.wait(this.mainLoopServerStack > stackId ? 100 : 50);
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
			
			// initial != null && initial.waitForClient()
			// && this.mainLoopC2SCommandFirst != null
			// && this.mainLoopC2SCommandFirst.getCmdType() < MainCmdItem.T_S2C_C2S
//			System.out.println("fromR evalDATA");
			item = internalEvalData((DataCmdItem) item);
			this.rniInterrupted = false;
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
			case DataCmdItem.EVAL_VOID: {
				rniEval(input);
				cmd.setAnswer(RjsStatus.OK_STATUS);
				break DATA_CMD;
			}
			case DataCmdItem.EVAL_DATA: {
				final long objP = rniEval(input);
				final RObject obj = rniCreateDataObject(objP, null, false, true);
				cmd.setAnswer(obj);
				break DATA_CMD;
			}
			case DataCmdItem.EVAL_STRUCT: {
				final long objP = rniEval(input);
				final RObject obj = rniCreateDataObject(objP, null, true, true);
				cmd.setAnswer(obj);
				break DATA_CMD;
			}
			case DataCmdItem.RESOLVE_DATA: {
				final long objP = Long.parseLong(input);
				if (objP != 0L) {
					final RObject obj = rniCreateDataObject(objP, null, false, true);
					cmd.setAnswer(obj);
					break DATA_CMD;
				}
				else {
					cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, 1021, "Invalid reference."));
					break DATA_CMD;
				}
			}
			case DataCmdItem.RESOLVE_STRUCT: {
				final long objP = Long.parseLong(input);
				if (objP != 0L) {
					final RObject obj = rniCreateDataObject(objP, null, true, true);
					cmd.setAnswer(obj);
					break DATA_CMD;
				}
				else {
					cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, 1021, "Invalid reference."));
					break DATA_CMD;
				}
			}
			case DataCmdItem.ASSIGN_DATA: {
				rniAssign(input, cmd.getData());
				break DATA_CMD;
			}
			default:
				throw new IllegalStateException("Unsupported command.");
			}
			if (this.rniInterrupted) {
				cmd.setAnswer(RjsStatus.CANCEL_STATUS);
			}
			return cmd;
		}
		catch (final RjsException e) {
			cmd.setAnswer(e.getStatus());
			return cmd;
		}
		catch (final Throwable e) {
			final String message = "Eval data failed. Cmd:\n" + cmd.toString() + ".";
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(new RjsStatus(RjsStatus.ERROR, 101, "Internal server error (see server log)."));
			return cmd;
		}
		finally {
			this.currentSlot = previousSlot;
			if (this.rniProtectedCounter > savedProtectedCounter) {
				this.rEngine.rniUnprotect(this.rniProtectedCounter - savedProtectedCounter);
				this.rniProtectedCounter = savedProtectedCounter;
			}
			this.rniMaxDepth = prevMaxDepth;
			if (ownLock) {
				this.rEngine.getRsync().unlock();
			}
		}
	}
	
	private long rniEval(final String expression) throws RjsException {
		final long expP = this.rEngine.rniParse("tryCatch(expr="+expression+",error=function(e){s<-raw(5);class(s)<-\".rj.eval.error\";attr(s,\"error\")<-e;attr(s,\"output\")<-paste(capture.output(print(e)),collapse=\"\\n\");s})", 1);
		if (expP == 0L || expP == this.rniP_NULL) {
			throw new RjsException(1001, "Invalid expression.");
		}
		final long objP = this.rEngine.rniEval(expP, 0L);
		if (objP <= 0L && objP > -4L) {
			throw new IllegalStateException("JRI returns error code " + objP);
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
				throw new RjsException(1002, "Evaluation failed: " + message);
			}
		}
		return objP;
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
			throw new RjsException(1032, "Missing data to assign.");
		}
		this.rEngine.rniAssign("rj.eval.adtmp", rniAssignDataObject(obj), 0L);
		final long assignEvalP = this.rEngine.rniParse("tryCatch(expr={"+expression+"<-.rj.getTmp(\"rj.eval.adtmp\");NULL},error=function(e){s<-raw(5);class(s)<-\".rj.eval.error\";attr(s,\"error\")<-e;attr(s,\"output\")<-paste(capture.output(print(e)),collapse=\"\\n\");s})", 1);
		if (assignEvalP == 0L || assignEvalP == this.rniP_NULL) {
			throw new RjsException(1031, "Invalid expression.");
		}
		long returnP = this.rEngine.rniEval(assignEvalP, 0L);
		if (returnP != this.rniP_NULL) {
			if (returnP <= 0L && returnP > -4L) {
				throw new IllegalStateException("JRI returns error code " + returnP);
			}
			this.rEngine.rniProtect(returnP);
			this.rniProtectedCounter++;
			String message = null;
			if (this.rEngine.rniExpType(returnP) == REXP.RAWSXP) {
				final long classP = this.rEngine.rniGetAttr(returnP, "class");
				if (classP != 0
						&& ".rj.eval.error".equals(this.rEngine.rniGetString(classP))) {
					final long outputP = this.rEngine.rniGetAttr(returnP, "output");
					if (outputP != 0L) {
						message = this.rEngine.rniGetString(outputP);
					}
				}
			}
			if (message == null) {
				message = "<no information available>";
			}
			throw new RjsException(1033, "Assignment failed: " + message);
		}
	}
	
	/**
	 * Put an {@link RObject RJ R object} into JRI, and get back the pointer to the object.
	 * 
	 * @param obj an R object
	 * @return long R pointer
	 */ 
	private long rniAssignDataObject(final RObject obj) {
		final RStore data;
		final long objP;
		CREATE_P: switch(obj.getRObjectType()) {
		case RObject.TYPE_NULL:
			return this.rniP_NULL;
		case RObject.TYPE_REFERENCE:
			return ((RReference) obj).getHandle();
		case RObject.TYPE_VECTOR:
			data = obj.getData();
			switch (data.getStoreType()) {
			case RStore.LOGICAL:
				objP = this.rEngine.rniPutBoolArrayI(
						((JRILogicalDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				break CREATE_P;
			case RStore.INTEGER:
				objP = this.rEngine.rniPutIntArray(
						((JRIIntegerDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				break CREATE_P;
			case RStore.NUMERIC:
				objP = this.rEngine.rniPutDoubleArray(
						((JRINumericDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				break CREATE_P;
//			case RStore.COMPLEX
			case RStore.CHARACTER:
				objP = this.rEngine.rniPutStringArray(
						((JRICharacterDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				break CREATE_P;
			case RStore.RAW:
				objP = this.rEngine.rniPutRawArray(
						((JRIRawDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				break CREATE_P;
			case RStore.FACTOR: {
				final JRIFactorDataImpl factor = (JRIFactorDataImpl) data;
				objP = this.rEngine.rniPutIntArray(factor.getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "levels",
						this.rEngine.rniPutStringArray(factor.getJRILevelsArray()));
				this.rEngine.rniSetAttr(objP, "class",
						factor.isOrdered() ? this.rniP_orderedClass : this.rniP_factorClass);
				break CREATE_P; }
			default:
				throw new UnsupportedOperationException();
			}
		case RObject.TYPE_ARRAY:
			data = obj.getData();
			switch (data.getStoreType()) {
			case RStore.LOGICAL:
				objP = this.rEngine.rniPutBoolArrayI(
						((JRILogicalDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P;
			case RStore.INTEGER:
				objP = this.rEngine.rniPutIntArray(
						((JRIIntegerDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P;
			case RStore.NUMERIC:
				objP = this.rEngine.rniPutDoubleArray(
						((JRINumericDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P;
//			case RStore.COMPLEX
			case RStore.CHARACTER:
				objP = this.rEngine.rniPutStringArray(
						((JRICharacterDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P;
			case RStore.RAW:
				objP = this.rEngine.rniPutRawArray(
						((JRIRawDataImpl) data).getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P;
			case RStore.FACTOR: {
				final JRIFactorDataImpl factor = (JRIFactorDataImpl) data;
				objP = this.rEngine.rniPutIntArray(factor.getJRIValueArray());
				this.rEngine.rniProtect(objP);
				this.rniProtectedCounter++;
				this.rEngine.rniSetAttr(objP, "levels",
						this.rEngine.rniPutStringArray(factor.getJRILevelsArray()));
				this.rEngine.rniSetAttr(objP, "class",
						factor.isOrdered() ? this.rniP_orderedClass : this.rniP_factorClass);
				this.rEngine.rniSetAttr(objP, "dim",
						this.rEngine.rniPutIntArray(((JRIArrayImpl<?>) obj).getJRIDimArray()));
				break CREATE_P; }
			default:
				throw new UnsupportedOperationException();
			}
		case RObject.TYPE_DATAFRAME: {
			final JRIDataFrameImpl list = (JRIDataFrameImpl) obj;
			final int length = list.getLength();
			final long[] itemPs = new long[length];
			for (int i = 0; i < length; i++) {
				itemPs[i] = rniAssignDataObject(list.get(i));
			}
			objP = this.rEngine.rniPutVector(itemPs);
			this.rEngine.rniProtect(objP);
			this.rniProtectedCounter++;
			this.rEngine.rniSetAttr(objP, "names",
					this.rEngine.rniPutStringArray(list.getJRINamesArray()));
			final int[] rownames = new int[list.getRowCount()];
			for (int i = 0; i < rownames.length; ) {
				rownames[i] = ++i;
			}
			this.rEngine.rniSetAttr(objP, "row.names",
					this.rEngine.rniPutIntArray(rownames));
			this.rEngine.rniSetAttr(objP, "class",
					this.rniP_dataframeClass);
			break CREATE_P; }
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
			this.rEngine.rniSetAttr(objP, "names",
					this.rEngine.rniPutStringArray(list.getJRINamesArray()));
			break CREATE_P; }
		
		default:
			throw new UnsupportedOperationException("assign of type " + obj.getRObjectType() + " is not yet supported.");
		}
		
		return objP;
	}
	
	/**
	 * Returns {@link RObject RJ R object} for the given R pointer.
	 * 
	 * @param objP a valid pointer to an object in R
	 * @param objTmp an optional R expression pointing to the same object in R
	 * @param structOnly enables {@link RObjectFactory#F_ONLY_STRUCT}
	 * @param force forces the creation of the object (ignoring the depth etc.)
	 * @return new created R object
	 */ 
	private RObject rniCreateDataObject(final long objP, String objTmp, final boolean structOnly, final boolean force) throws RjException {
		if (!force && (this.rniTemp > 512 || this.rniTemp >= this.rniMaxDepth || this.rniInterrupted)) {
			return null;
		}
		boolean tmpAssigned;
		if (objTmp == null) {
			objTmp = ".rj.eval.temp"+this.rniTemp++;
			this.rEngine.rniAssign(objTmp, objP, 0L);
			tmpAssigned = true;
		}
		else {
			this.rniTemp++;
			tmpAssigned = false;
		}
		try {
			final int rType = this.rEngine.rniExpType(objP);
			switch (rType) {
			case REXP.NILSXP:
				return RNull.INSTANCE;
			
			case REXP.LGLSXP: { // logical vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return (structOnly) ?
							new JRIArrayImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RLogicalStore>(
									new JRILogicalDataImpl(this.rEngine.rniGetBoolArrayI(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.INTSXP: { // integer vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				if (className1 != null
						&& (className1.equals("factor") || this.rEngine.rniInherits(objP, "factor")) ) {
					final String[] levels = rniGetLevels(objP);
					if (levels != null) {
						final boolean isOrdered = className1.equals("ordered") || this.rEngine.rniInherits(objP, "ordered");
						final RFactorStore factorData = (structOnly) ?
								new RFactorDataStruct(rniGetLength(objTmp), isOrdered, levels.length) :
								new RFactorDataImpl(this.rEngine.rniGetIntArray(objP), isOrdered, levels);
						return (structOnly) ?
								new JRIVectorImpl<RIntegerStore>(
										factorData,
										rniGetLength(objTmp), className1, null) :
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
					return (structOnly) ?
							new JRIArrayImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RIntegerStore>(
									new JRIIntegerDataImpl(this.rEngine.rniGetIntArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.REALSXP: { // numeric vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return (structOnly) ?
							new JRIArrayImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RNumericStore>(
									new JRINumericDataImpl(this.rEngine.rniGetDoubleArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.CPLXSXP: { // complex vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return new JRIArrayImpl<RComplexStore>((structOnly) ?
							RObjectFactoryImpl.CPLX_STRUCT_DUMMY :
							RComplexDataBImpl.createFromJRI(rniGetComplexRe(objTmp), rniGetComplexIm(objTmp)),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RComplexStore>(
									RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RComplexStore>(
									RComplexDataBImpl.createFromJRI(rniGetComplexRe(objTmp), rniGetComplexIm(objTmp)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.STRSXP: { // character vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return (structOnly) ?
							new JRIArrayImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RCharacterStore>(
									new JRICharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.RAWSXP: { // raw/byte vector
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final int[] dim;
				{	final long dimP = this.rEngine.rniGetAttr(objP, "dim");
					dim = (dimP != 0L) ? this.rEngine.rniGetIntArray(dimP) : null;
				}
				
				if (dim != null) {
					return (structOnly) ?
							new JRIArrayImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									className1, dim) :
							new JRIArrayImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, dim);
				}
				else {
					return (structOnly) ?
							new JRIVectorImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new JRIVectorImpl<RRawStore>(
									new JRIRawDataImpl(this.rEngine.rniGetRawArray(objP)),
									className1, rniGetNames(objP));
				}
			}
			case REXP.VECSXP: { // generic vector / list
				final String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
				}
				final String[] itemNames;
				{	final long namesP = this.rEngine.rniGetAttr(objP, "names");
					itemNames = (namesP != 0L) ? this.rEngine.rniGetStringArray(namesP) : null;
				}
				
				final long[] itemP = this.rEngine.rniGetVector(objP);
				if (itemP.length > 16 && !tmpAssigned) {
					objTmp = ".rj.eval.temp"+(this.rniTemp-1);
					this.rEngine.rniAssign(objTmp, objP, 0L);
					tmpAssigned = true;
				}
				if (className1 != null &&
						(className1.equals("data.frame") || this.rEngine.rniInherits(objP, "data.frame")) ) {
					final String[] rowNames = (structOnly) ? null : rniGetRowNames(objP);
					final RObject[] itemObjects = new RObject[itemP.length];
					for (int i = 0; i < itemP.length; i++) {
						itemObjects[i] = rniCreateDataObject(itemP[i], null, structOnly, true);
					}
					return new JRIDataFrameImpl(itemObjects, className1, itemNames, rowNames);
				}
				else {
					if (structOnly && itemP.length > this.rniListsMaxLength) {
						return new JRIListImpl(itemP.length, className1, itemNames);
					}
					final RObject[] itemObjects = new RObject[itemP.length];
					for (int i = 0; i < itemP.length; i++) {
						itemObjects[i] = rniCreateDataObject(itemP[i], null, structOnly, false);
					}
					return new JRIListImpl(itemObjects, className1, itemNames);
				}
			}
			case REXP.LISTSXP:   // pairlist
			/*case REXP.LANGSXP: */{
				String className1;
				{	final long classP = this.rEngine.rniGetAttr(objP, "class");
					if (classP == 0L
							|| (className1 = this.rEngine.rniGetString(classP)) == null) {
						className1 = RObject.CLASSNAME_PAIRLIST;
					}
				}
				
				long cdr = objP;
				final int length = rniGetLength(objTmp);
				final String[] itemNames = new String[length];
				final RObject[] itemObjects = new RObject[length];
				for (int i = 0; i < length; i++) {
					final long car = this.rEngine.rniCAR(cdr);
					final long tag = this.rEngine.rniTAG(cdr);
					itemNames[i] = (tag != 0L) ? this.rEngine.rniGetSymbolName(tag) : null;
					itemObjects[i] = rniCreateDataObject(car, null, structOnly, false);
					cdr = this.rEngine.rniCDR(cdr);
					if (cdr == 0L || this.rEngine.rniExpType(cdr) != REXP.LISTSXP) {
						break;
					}
				}
				return new JRIListImpl(itemObjects, className1, itemNames);
			}
			case REXP.ENVSXP: {
				if (this.rniTemp > 1) {
					return new RReferenceImpl(objP, RObject.TYPE_REFERENCE, "environment");
				}
				final String[] names = this.rEngine.rniGetStringArray(this.rEngine.rniListEnv(objP, true));
				if (names != null) {
					final String className1;
					{	final long classP = this.rEngine.rniGetAttr(objP, "class");
						className1 = (classP != 0L) ? this.rEngine.rniGetString(classP) : null;
					}
					
					// env name
					final long nameP = this.rEngine.rniEval(this.rEngine.rniParse("environmentName(env="+objTmp+')', 1), 0L);
					String name;
					if ((nameP <= 0L && nameP >= -4L)
							|| (name = this.rEngine.rniGetString(nameP)) == null) {
						name = "";
					}
					boolean isAutoloadEnv = "Autoloads".equals(name); // TODO newer JRI provides direct access
					
					if (isAutoloadEnv || names.length > this.rniEnvsMaxLength) {
						return new JRIEnvironmentImpl(name, objP, null, null, names.length, className1);
					}
					
					if (names.length > 16 && !tmpAssigned) {
						objTmp = ".rj.eval.temp"+(this.rniTemp-1);
						this.rEngine.rniAssign(objTmp, objP, 0L);
						tmpAssigned = true;
					}
					final RObject[] itemObjects = new RObject[names.length];
					int idx = 0;
					for (int i = 0; i < names.length; i++) {
						if (names[i] == null || names[i].startsWith(".rj.eval.temp")) {
							continue;
						}
						names[idx] = names[i];
//						final long itemPa = this.rEngine.rniFindVar(names[i], objP);
						final String itemTmp = objTmp+"$`"+names[i]+'`';
						final long itemP = this.rEngine.rniEval(this.rEngine.rniParse(itemTmp, 1), 0L);
						if (itemP > 0L || itemP < -4L) {
							itemObjects[idx] = rniCreateDataObject(itemP, itemTmp, structOnly, false);
						}
//						if (itemObjects[idx] == null) {
//							System.out.println("type="+ this.rEngine.rniExpType(itemP) + ", name=" + names[i]);
//						}
						idx++;
					}
					
					return new JRIEnvironmentImpl(name, objP, itemObjects, names, idx, className1);
				}
				return null;
			}
			case REXP.CLOSXP: {
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				
				final String header = rniGetFHeader(objTmp);
				return RFunctionImpl.createForServer(header);
			}
			case REXP.SPECIALSXP:
			case REXP.BUILTINSXP: {
				final String header = rniGetFHeader(objTmp);
				return RFunctionImpl.createForServer(header);
			}
			case REXP.S4SXP: {
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
			}
			}
			
			{	String className1;
				final long classP = this.rEngine.rniEval(this.rEngine.rniParse("class("+objTmp+")", 1), 0L);
				if ((classP <= 0L && classP >= -4L)
						|| (className1 = this.rEngine.rniGetString(classP)) == null) {
					className1 = "<unknown>";
				}
				return new ROtherImpl(className1);
			}
		}
		finally {
			this.rniTemp--;
			if (tmpAssigned) {
				this.rEngine.rniEval(this.rEngine.rniParse("rm("+objTmp+");", 1), 0L);
			}
		}
	}
	
	private RObject rniCheckAndCreateS4Obj(final String objTmp, final boolean structOnly) throws RjException {
		final long classP = this.rEngine.rniEval(this.rEngine.rniParse("if (isS4("+objTmp+")) class("+objTmp+")", 1), 0L);
		final String className;
		if ((classP > 0L || classP < -4L)
				&& (className = this.rEngine.rniGetString(classP)) != null) {
			final long slotNamesP = this.rEngine.rniEval(this.rEngine.rniParse("slotNames('"+className+"')", 1), 0L);
			if (slotNamesP != 0L && this.rEngine.rniExpType(slotNamesP) == REXP.STRSXP) {
				final String[] slotNames = this.rEngine.rniGetStringArray(slotNamesP);
				final RObject[] slotValues = new RObject[slotNames.length];
				for (int i = 0; i < slotNames.length; i++) {
					final String itemTmp = objTmp + "@`" + slotNames[i] + '`';
					final long slotValueP = this.rEngine.rniEval(this.rEngine.rniParse(itemTmp, 1), 0L);
					if (slotValueP > 0L || slotValueP < -4L) {
						if (".Data".equals(slotNames[i])) {
							this.rEngine.rniProtect(slotValueP);
							this.rniProtectedCounter++;
							slotValues[i] = rniCreateDataObject(slotValueP, itemTmp, structOnly, true);
							this.rEngine.rniUnprotect(1);
							this.rniProtectedCounter--;
						}
						else {
							slotValues[i] = rniCreateDataObject(slotValueP, itemTmp, structOnly, true);
						}
					}
				}
				return new RS4ObjectImpl(className, slotNames, slotValues);
			}
		}
		return null;
	}
	
	private String[] rniGetNames(final long pointer) {
		final long namesP = this.rEngine.rniGetAttr(pointer, "names");
		if (namesP != 0L) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private String[] rniGetRowNames(final long pointer) {
		final long namesP = this.rEngine.rniGetAttr(pointer, "row.names");
		if (namesP != 0L) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private String[] rniGetLevels(final long pointer) {
		final long levelsP = this.rEngine.rniGetAttr(pointer, "levels");
		if (levelsP != 0L) {
			return this.rEngine.rniGetStringArray(levelsP);
		}
		return null;
	}
	
	private int rniGetLength(final String tmp) {
		final long lengthP = this.rEngine.rniEval(this.rEngine.rniParse("length("+tmp+")", 1), 0L);
		final int[] length;
		if ((lengthP > 0L || lengthP < -4L)
				&& (length = this.rEngine.rniGetIntArray(lengthP)) != null && length.length == 1) {
			return length[0];
		}
		return 0;
	}
	
	private double[] rniGetComplexRe(final String tmp) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniParse("Re("+tmp+")", 1), 0L);
		if ((numP > 0L || numP < -4L)
				&& this.rEngine.rniExpType(numP) == REXP.REALSXP) {
			return this.rEngine.rniGetDoubleArray(numP);
		}
		throw new IllegalStateException();
	}
	
	private double[] rniGetComplexIm(final String tmp) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniParse("Im("+tmp+")", 1), 0L);
		if ((numP > 0L || numP < -4L)
				&& this.rEngine.rniExpType(numP) == REXP.REALSXP) {
			return this.rEngine.rniGetDoubleArray(numP);
		}
		throw new IllegalStateException();
	}
	
	private String rniGetFHeader(final String tmp) {
		final long argsP = this.rEngine.rniEval(this.rEngine.rniParse("paste(deparse(expr= args("+tmp+"), width.cutoff= 500), collapse=\"\")", 1), 0L);
		final String args;
		if ((argsP > 0L || argsP < -4L)
				&& (args = this.rEngine.rniGetString(argsP)) != null && args.length() >= 11) { // "function ()".length
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
			synchronized (this.mainLoopLock) {
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
					this.mainLoopLock.notifyAll();
				}
				return;
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
		final MainCmdItem cmd = internalMainFromR(new ExtUICmdItem(ExtUICmdItem.C_CHOOSE_FILE,
				(newFile == 1) ? ExtUICmdItem.O_NEW : 0, true));
		if (cmd.isOK()) {
			return cmd.getDataText();
		}
		else {
			return null;
		}
	}
	
	public void rLoadHistory(final Rengine engine, final String filename) {
		execUICommand(ExtUICmdItem.C_LOAD_HISTORY, filename, true);
	}
	
	public void rSaveHistory(final Rengine engine, final String filename) {
		execUICommand(ExtUICmdItem.C_SAVE_HISTORY, filename, true);
	}
	
	private void internalRStopped() {
		synchronized (this.mainLoopLock) {
			if (this.mainLoopState == ENGINE_STOPPED) {
				return;
			}
			this.mainLoopState = ENGINE_STOPPED;
			
			this.mainLoopLock.notifyAll();
			while (this.mainLoopS2CNextCommandsFirst != null || this.mainLoopStdOutSize > 0) {
				try {
					this.mainLoopLock.wait(100);
				} catch (final InterruptedException e) {
					Thread.interrupted();
				}
				this.mainLoopLock.notifyAll();
			}
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
	public String execUICommand(final String command, final String arg, final boolean wait) {
		if (command == null) {
			throw new NullPointerException("command");
		}
		final MainCmdItem answer = internalMainFromR(new ExtUICmdItem(command, 0, arg, wait));
		if (wait && answer instanceof ExtUICmdItem
				&& answer.isOK()) {
			return answer.getDataText();
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
