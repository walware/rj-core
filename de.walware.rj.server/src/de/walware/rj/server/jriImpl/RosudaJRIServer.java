/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jriImpl;

import static de.walware.rj.server.RjsComObject.T_PING;
import static de.walware.rj.server.RjsComObject.V_CANCEL;
import static de.walware.rj.server.RjsComObject.V_ERROR;
import static de.walware.rj.server.RjsComObject.V_FALSE;
import static de.walware.rj.server.RjsComObject.V_INFO;
import static de.walware.rj.server.RjsComObject.V_OK;
import static de.walware.rj.server.RjsComObject.V_TRUE;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.rj.JRClassLoader;

import de.walware.rj.data.RCharacterStore;
import de.walware.rj.data.RComplexStore;
import de.walware.rj.data.RFactorStore;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RLogicalStore;
import de.walware.rj.data.RNumericStore;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RRawStore;
import de.walware.rj.data.defaultImpl.RArrayImpl;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RComplexDataBImpl;
import de.walware.rj.data.defaultImpl.RDataFrameImpl;
import de.walware.rj.data.defaultImpl.REnvironmentImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataStruct;
import de.walware.rj.data.defaultImpl.RFunctionImpl;
import de.walware.rj.data.defaultImpl.RIntegerDataImpl;
import de.walware.rj.data.defaultImpl.RListImpl;
import de.walware.rj.data.defaultImpl.RLogicalDataIntImpl;
import de.walware.rj.data.defaultImpl.RNull;
import de.walware.rj.data.defaultImpl.RNumericDataBImpl;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.data.defaultImpl.ROtherImpl;
import de.walware.rj.data.defaultImpl.RRawDataStruct;
import de.walware.rj.data.defaultImpl.RReferenceImpl;
import de.walware.rj.data.defaultImpl.RS4ObjectImpl;
import de.walware.rj.data.defaultImpl.RVectorImpl;
import de.walware.rj.server.ConsoleCmdItem;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdS2CList;
import de.walware.rj.server.RJ;
import de.walware.rj.server.RjException;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.RjsStatusImpl1;
import de.walware.rj.server.RjsStatusImpl2;
import de.walware.rj.server.Server;
import de.walware.rj.server.ServerInfo;
import de.walware.rj.server.ServerLogin;
import de.walware.rj.server.srvext.ExtServer;
import de.walware.rj.server.srvext.ServerAuthMethod;
import de.walware.rj.server.srvext.ServerRuntimePlugin;


/**
 * Remove server based on
 */
public class RosudaJRIServer extends RJ
		implements Server, RMainLoopCallbacks, ExtServer, Unreferenced {
	
	private static final int ENGINE_NOT_STARTED = 0;
	private static final int ENGINE_RUN_IN_R = 1;
	private static final int ENGINE_WAIT_FOR_CLIENT = 2;
	private static final int ENGINE_STOPPED = 4;
	
	private static final int CLIENT_NONE = 0;
	private static final int CLIENT_OK = 1;
	private static final int CLIENT_CANCEL = 2;
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.jri");
	
	private static final int STDOUT_BUFFER_SIZE = 0x1FFF;
	
	
	static {
		System.loadLibrary("jri");
	}
	
	
	private String name;
	private String ownerUsername;
	private String workingDirectory;
	private String currentUsername;
	
	private JRClassLoader rClassLoader;
	private Rengine rEngine;
	
	private final Object mainLoopLock = new Object();
	private int mainLoopState;
	private boolean mainLoopBusyAtServer = false;
	private boolean mainLoopBusyAtClient = true;
	private int mainLoopClientNext;
	private int mainLoopClientListen;
	private int mainLoopServerStack;
	private final char[] mainLoopStdOutBuffer = new char[STDOUT_BUFFER_SIZE];
	private String mainLoopStdOutSingle;
	private int mainLoopStdOutSize;
	private MainCmdItem mainLoopS2CNextCommandsFirst;
	private MainCmdItem mainLoopS2CNextCommandsLast;
	private final MainCmdS2CList mainLoopS2CLastCommands = new MainCmdS2CList();
	private MainCmdItem mainLoopS2CRequest;
	private MainCmdItem mainLoopC2SCommandFirst;
	private int mainLoopS2CAnswerFail;
	
	private int serverState;
	
	private ServerAuthMethod authMethod;
	private String currentMainClientId;
	private int currentTicket;
	
	private final Object pluginsLock = new Object();
	private ServerRuntimePlugin[] pluginsList = new ServerRuntimePlugin[0];
	
	private int rniTemp;
	private int rniMax;
	
	
	public RosudaJRIServer() {
		this.mainLoopState = ENGINE_NOT_STARTED;
		this.mainLoopClientNext = CLIENT_NONE;
		
		this.serverState = S_NOT_STARTED;
		
		this.currentTicket = new Random().nextInt();
	}
	
	
	public void init(final String name, final ServerAuthMethod authMethod) {
		this.name = name;
		this.authMethod = authMethod;
		this.ownerUsername = System.getProperty("user.name");
		this.workingDirectory = System.getProperty("user.dir");
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
	
	
	public ServerLogin createLogin() throws RemoteException {
		try {
			return this.authMethod.createLogin();
		}
		catch (final RjException e) {
			final String message = "Initializing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
	}
	
	private int connectClient(final ServerLogin login) throws RemoteException, LoginException {
		try {
			final String username = this.authMethod.performLogin(login);
			this.currentMainClientId = RemoteServer.getClientHost();
			this.currentUsername = username;
		}
		catch (final RjException e) {
			final String message = "Performing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
		catch (final ServerNotActiveException e) {
			throw new IllegalStateException(e);
		}
		this.serverState = S_CONNECTED;
		return ++this.currentTicket;
	}
	
	private void disconnectClient() {
		this.currentMainClientId = null;
		this.currentUsername = null;
		this.serverState = S_DISCONNECTED;
		this.currentTicket++;
	}
	
	public void unreferenced() {
		boolean disconnected = false;
		synchronized (this.mainLoopLock) {
			if (this.mainLoopClientNext != CLIENT_NONE) {
				this.mainLoopClientNext = CLIENT_NONE;
				disconnectClient();
				disconnected = true;
			}
		}
		if (disconnected) {
			LOGGER.log(Level.INFO, "R engine is no longer referenced by a client. New Client-State: 'Disconnected'.");
		}
	}
	
	private void checkClient(final long ticket) throws RemoteException {
		final String expectedClient = this.currentMainClientId;
		final String remoteClient;
		try {
			remoteClient = RemoteServer.getClientHost();
		}
		catch (final ServerNotActiveException e) {
			throw new IllegalStateException(e);
		}
		if (this.currentTicket != ticket
				|| expectedClient == null 
				|| !expectedClient.equals(remoteClient)) {
			throw new ConnectException("Not connected.");
		}
	}
	
	
	public int[] getVersion() throws RemoteException {
		return new int[] { 0, 2, 0 };
	}
	
	public ServerInfo getInfo() throws RemoteException {
		return new ServerInfo(this.name, this.ownerUsername, this.workingDirectory,
				this.serverState, this.currentUsername);
	}
	
	public int getState() {
		return this.serverState;
	}
	
	
	public synchronized int start(final ServerLogin login, final String[] args) throws RemoteException, LoginException {
		if (this.mainLoopState != ENGINE_NOT_STARTED) {
			throw new IllegalStateException("R engine is already started.");
		}
		final int ticket = connectClient(login);
		
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			this.rClassLoader = JRClassLoader.getRJavaClassLoader();
			Thread.currentThread().setContextClassLoader(this.rClassLoader);
			
			this.rEngine = new Rengine(checkArgs(args), false, this);
			this.rEngine.setContextClassLoader(this.rClassLoader);
			
			this.mainLoopState = ENGINE_RUN_IN_R;
			this.rEngine.startMainLoop();
			if (!this.rEngine.waitForR()) {
				internalRStopped();
				throw new IllegalThreadStateException("R thread not started");
			}
			synchronized (this.mainLoopLock) {
				this.mainLoopS2CAnswerFail = 0;
				this.mainLoopClientNext = CLIENT_OK;
			}
			LOGGER.log(Level.INFO, "R engine started successfully. New Client-State: 'Connected'.");
		}
		catch (final Throwable e) {
			this.serverState = S_STOPPED;
			final String message = "Could not start the R engine";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
		return ticket;
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
	
	public synchronized int connect(final ServerLogin login) throws RemoteException, LoginException {
		final int ticket = connectClient(login);
		
		synchronized (this.mainLoopLock) {
			this.mainLoopS2CAnswerFail = 0;
			switch (this.mainLoopState) {
			case ENGINE_WAIT_FOR_CLIENT:
				this.mainLoopBusyAtClient = true;
				this.mainLoopClientNext = CLIENT_OK;
				if (this.mainLoopS2CLastCommands.getItems() != null) {
					if (this.mainLoopS2CNextCommandsFirst != null) {
						MainCmdItem last = this.mainLoopS2CLastCommands.getItems();
						while (last.next != null) {
							last = last.next;
						}
						last.next = this.mainLoopS2CNextCommandsFirst;
					}
					this.mainLoopS2CNextCommandsFirst = this.mainLoopS2CLastCommands.getItems();
				}
				if (this.mainLoopS2CNextCommandsFirst == null) {
					this.mainLoopS2CNextCommandsFirst = this.mainLoopS2CNextCommandsLast = this.mainLoopS2CRequest;
				}
				// restore mainLoopS2CNextCommandsLast, if necessary
				if (this.mainLoopS2CNextCommandsLast == null && this.mainLoopS2CNextCommandsFirst != null) {
					this.mainLoopS2CNextCommandsLast = this.mainLoopS2CNextCommandsFirst;
					while (this.mainLoopS2CNextCommandsLast.next != null) {
						this.mainLoopS2CNextCommandsLast = this.mainLoopS2CNextCommandsLast.next;
					}
				}
				// notify listener client
//					try {
//						new RjsStatusImpl2(V_CANCEL, S_DISCONNECTED, 
//								(this.currentUsername != null) ? ("user "+ this.currentUsername) : null);
//					}
//					catch (Throwable e) {}
				return ticket;
			case ENGINE_RUN_IN_R:
				// exit old client
				if (this.mainLoopClientNext == CLIENT_OK) {
					this.mainLoopClientNext = CLIENT_CANCEL;
					this.mainLoopLock.notifyAll();
					while(this.mainLoopClientNext == CLIENT_CANCEL) {
						try {
							this.mainLoopLock.wait(100);
						}
						catch (final InterruptedException e) {
							Thread.interrupted();
						}
					}
				}
				// setup new client
				if (this.mainLoopClientNext != CLIENT_NONE) {
					throw new AssertionError();
				}
				this.mainLoopBusyAtClient = true;
				this.mainLoopClientNext = CLIENT_OK;
				return ticket;
			default:
				throw new IllegalStateException("R engine is not running.");
			}
		}
	}
	
	public synchronized void disconnect(final int ticket) throws RemoteException {
		checkClient(ticket);
		synchronized (this.mainLoopLock) {
			switch (this.mainLoopState) {
			case ENGINE_WAIT_FOR_CLIENT:
				this.mainLoopClientNext = CLIENT_NONE;
				disconnectClient();
				return;
			case ENGINE_RUN_IN_R:
				// exit old client
				this.mainLoopClientNext = CLIENT_CANCEL;
				this.mainLoopLock.notifyAll();
				while(this.mainLoopClientNext == CLIENT_CANCEL) {
					try {
						this.mainLoopLock.wait(100);
					}
					catch (final InterruptedException e) {
						Thread.interrupted();
					}
				}
				// setup new client
				if (this.mainLoopClientNext != CLIENT_NONE) {
					throw new AssertionError();
				}
				disconnectClient();
				return;
			default:
				throw new IllegalStateException("R engine is not running.");
			}
		}
	}
	
	public void interrupt(final int ticket) throws RemoteException {
		checkClient(ticket);
		try {
			this.rEngine.rniStop(1);
		}
		catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "An error occurred when trying to interrupt the R engine.", e);
		}
		return;
	}
	
	public RjsComObject runMainLoop(final int ticket, final RjsComObject command) throws RemoteException {
		checkClient(ticket);
		this.mainLoopS2CLastCommands.clear();
		if (command == null) {
			return internalMainCallbackFromClient(null);
		}
		else {
			switch (command.getComType()) {
			case T_PING:
				return RjsStatus.OK_STATUS;
			case RjsComObject.T_MAIN_LIST:
				return internalMainCallbackFromClient((MainCmdC2SList) command);
			default:
				throw new IllegalArgumentException("Unknown command: " + 
						((command != null) ? "0x"+Integer.toHexString(command.getComType()) : "-"));
			}
		}
	}
	
	
	public RjsComObject runAsync(final int ticket, final RjsComObject command) throws RemoteException {
		checkClient(ticket);
		switch (command.getComType()) {
		case T_PING:
			return internalPing();
		default:
			throw new IllegalArgumentException("Unknown command: " + 
					((command != null) ? "0x"+Integer.toHexString(command.getComType()) : "-"));
		}
	}
	
	
	private RjsStatusImpl1 internalPing() {
		final Rengine r = this.rEngine;
		if (r.isAlive()) {
			return RjsStatus.OK_STATUS;
		}
		if (this.mainLoopState != ENGINE_STOPPED) {
			// invalid state
		}
		return new RjsStatusImpl1(RjsStatusImpl1.V_WARNING, S_STOPPED);
	}
	
	private RjsComObject internalMainCallbackFromClient(final MainCmdC2SList mainC2SCmdList) {
		synchronized (this.mainLoopLock) {
			if (this.mainLoopState == ENGINE_WAIT_FOR_CLIENT) {
				if (mainC2SCmdList == null && this.mainLoopS2CNextCommandsFirst == null) {
					if (this.mainLoopS2CAnswerFail == 0) { // retry
						this.mainLoopS2CAnswerFail++;
						this.mainLoopS2CNextCommandsFirst = this.mainLoopS2CNextCommandsLast = this.mainLoopS2CRequest;
						// continue ANSWER
					}
					else { // fail
						this.mainLoopC2SCommandFirst = this.mainLoopS2CRequest;
						this.mainLoopC2SCommandFirst.setAnswer(RjsComObject.V_ERROR);
						// continue in R
					}
				}
				else { // ok
					this.mainLoopS2CAnswerFail = 0;
					if (mainC2SCmdList != null) {
						this.mainLoopC2SCommandFirst = mainC2SCmdList.getItems();
					}
					// continue in R
				}
			}
			
			this.mainLoopLock.notifyAll();
			while (this.mainLoopS2CNextCommandsFirst == null
					&& (this.mainLoopStdOutSize == 0)
					&& (this.mainLoopBusyAtClient == this.mainLoopBusyAtServer)
					&& (this.mainLoopState != ENGINE_STOPPED)
					&& (this.mainLoopClientNext == CLIENT_OK)) {
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
			
			// answer
			if (this.mainLoopClientNext == CLIENT_OK) {
				if (this.mainLoopStdOutSize > 0) {
					internalClearStdOutBuffer();
					this.mainLoopStdOutSize = 0;
				}
				if (this.mainLoopState == ENGINE_STOPPED && this.mainLoopS2CNextCommandsFirst == null) {
					return new RjsStatusImpl1(V_INFO, S_STOPPED);
				}
				this.mainLoopBusyAtClient = this.mainLoopBusyAtServer;
				this.mainLoopS2CLastCommands.setBusy(this.mainLoopBusyAtClient);
				this.mainLoopS2CLastCommands.setObjects(this.mainLoopS2CNextCommandsFirst);
				this.mainLoopS2CNextCommandsFirst = null;
				return this.mainLoopS2CLastCommands;
			}
			else {
				this.mainLoopClientNext = CLIENT_NONE;
				return new RjsStatusImpl2(V_CANCEL, S_DISCONNECTED, 
						(this.currentUsername != null) ? ("user "+ this.currentUsername) : null);
			}
		}
	}
	
	private void internalMainFromR(MainCmdItem item) {
		while (true) {
			synchronized (this.mainLoopLock) {
				if (item != null) {
					if (this.mainLoopStdOutSize > 0) {
						internalClearStdOutBuffer();
						this.mainLoopStdOutSize = 0;
					}
					if (this.mainLoopS2CNextCommandsFirst == null) {
						this.mainLoopS2CNextCommandsFirst = this.mainLoopS2CNextCommandsLast = item;
					}
					else {
						this.mainLoopS2CNextCommandsLast = this.mainLoopS2CNextCommandsLast.next = item;
					}
				}
				
				this.mainLoopLock.notifyAll();
				if (item != null && item.waitForClient()) {
					if (this.mainLoopState == ENGINE_STOPPED) {
						item.setAnswer(V_ERROR);
						this.mainLoopC2SCommandFirst = item;
						return;
					}
					// wait for client
					this.mainLoopC2SCommandFirst = null;
					if (item.getCmdType() < MainCmdItem.T_S2C_C2S) {
						this.mainLoopS2CRequest = item;
					}
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
					}
					this.mainLoopState = ENGINE_RUN_IN_R;
				}
			}
			
			if (this.mainLoopC2SCommandFirst != null && this.mainLoopC2SCommandFirst.getCmdType() > MainCmdItem.T_S2C_C2S) {
				final MainCmdItem command = this.mainLoopC2SCommandFirst;
				this.mainLoopC2SCommandFirst = this.mainLoopC2SCommandFirst.next;
				item = internalEvalData((DataCmdItem) command);
				continue;
			}
			else {
				break;
			}
		}
	}
	
	private void internalClearStdOutBuffer() {
		final MainCmdItem item;
		if (this.mainLoopStdOutSingle != null) {
			item = new ConsoleCmdItem.Write(V_OK, this.mainLoopStdOutSingle);
			this.mainLoopStdOutSingle = null;
		}
		else {
			item = new ConsoleCmdItem.Write(V_OK, new String(this.mainLoopStdOutBuffer, 0, this.mainLoopStdOutSize));
		}
		if (this.mainLoopS2CNextCommandsFirst == null) {
			this.mainLoopS2CNextCommandsFirst = this.mainLoopS2CNextCommandsLast = item;
		}
		else {
			this.mainLoopS2CNextCommandsLast = this.mainLoopS2CNextCommandsLast.next = item;
		}
	}
	
	private DataCmdItem internalEvalData(final DataCmdItem cmd) {
		int depth = cmd.getDepth();
		if (depth < 1) {
			depth = 128;
		}
		final boolean ownLock = this.rEngine.getRsync().safeLock();
		this.rniMax += depth;
		try {
			String input = cmd.getDataText();
			switch (cmd.getEvalType()) {
			case DataCmdItem.EVAL_VOID: {
				if (input != null) {
					input = "tryCatch(expr="+input+",error=function(x){assign(x=\".rj_eval.error\",envir=as.environment(\".GlobalEnv\"),value=x);e<-raw(5);class(e)<-\".rj_eval.error\";e})";
					final long expP = this.rEngine.rniParse(input, 1);
					if (expP != 0) {
						this.rEngine.rniEval(expP, 0);
						cmd.setAnswer(RjsStatus.OK);
						break;
					}
				}
				cmd.setAnswer(RjsStatus.ERROR); }
			case DataCmdItem.EVAL_DATA: {
				if (input != null) {
					input = "tryCatch(expr="+input+",error=function(x){assign(x=\".rj_eval.error\",envir=as.environment(\".GlobalEnv\"),value=x);e<-raw(5);class(e)<-\".rj_eval.error\";e})";
					final long expP = this.rEngine.rniParse(input, 1);
					if (expP != 0) {
						final long objP = this.rEngine.rniEval(expP, 0);
						if (objP != 0) {
							final RObject obj = rniCreateDataObject(objP, null, false);
							cmd.setAnswer(obj);
							break;
						}
					}
				}
				cmd.setAnswer(RjsStatus.ERROR);
				break; }
			case DataCmdItem.EVAL_STRUCT: {
				if (input != null) {
					input = "tryCatch(expr="+input+",error=function(x){assign(x=\".rj_eval.error\",envir=as.environment(\".GlobalEnv\"),value=x);e<-raw(5);class(e)<-\".rj_eval.error\";e})";
					final long expP = this.rEngine.rniParse(input, 1);
					if (expP != 0) {
						final long objP = this.rEngine.rniEval(expP, 0);
						if (objP != 0) {
							final RObject obj = rniCreateDataObject(objP, null, true);
							cmd.setAnswer(obj);
							break;
						}
					}
				}
				cmd.setAnswer(RjsStatus.ERROR);
				break; }
			case DataCmdItem.RESOLVE_STRUCT: {
				if (input != null) {
					final long objP = Long.parseLong(input);
					if (objP != 0) {
						final RObject obj = rniCreateDataObject(objP, null, true);
						cmd.setAnswer(obj);
						break;
					}
				}
				cmd.setAnswer(RjsStatus.ERROR);
				break; }
			default:
				throw new RjException("Unsupported EvalCmd");
			}
			return cmd;
		}
		catch (final Throwable e) {
			final String message = "Eval data failed. Cmd:\n" + ((cmd != null) ? cmd.toString() : "<missing>");
			LOGGER.log(Level.SEVERE, message, e);
			cmd.setAnswer(RjsStatus.ERROR);
			return cmd;
		}
		finally {
			this.rniMax -= depth;
			if (ownLock) {
				this.rEngine.getRsync().unlock();
			}
		}
	}
	
	private RObject rniCreateDataObject(final long objP, String objTmp, final boolean structOnly) {
		if (objP == 0 || this.rniTemp > 512 || this.rniTemp > this.rniMax) {
			return null;
		}
		boolean tmpAssigned;
		if (objTmp == null) {
			objTmp = ".rj_eval.temp"+this.rniTemp++;
			this.rEngine.rniAssign(objTmp, objP, 0);
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
			case REXP.REALSXP: { // numeric vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				final int[] dim = rniGetDim(objP);
				if (dim != null) {
					return new RArrayImpl<RNumericStore>((structOnly) ?
							RObjectFactoryImpl.NUM_STRUCT_DUMMY :
							RNumericDataBImpl.createForServer(this.rEngine.rniGetDoubleArray(objP)),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new RVectorImpl<RNumericStore>(
									RObjectFactoryImpl.NUM_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new RVectorImpl<RNumericStore>(
									RNumericDataBImpl.createForServer(this.rEngine.rniGetDoubleArray(objP)),
									className1, rniGetNames(objP));
				} }
			case REXP.STRSXP: { // character vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				final int[] dim = rniGetDim(objP);
				if (dim != null) {
					return new RArrayImpl<RCharacterStore>((structOnly) ?
							RObjectFactoryImpl.CHR_STRUCT_DUMMY :
							new RCharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new RVectorImpl<RCharacterStore>(
									RObjectFactoryImpl.CHR_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new RVectorImpl<RCharacterStore>(
									new RCharacterDataImpl(this.rEngine.rniGetStringArray(objP)),
									className1, rniGetNames(objP));
				} }
			case REXP.INTSXP: { // integer vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				if (this.rEngine.rniInherits(objP, "factor")) {
					final String[] levels = rniGetLevels(objP);
					if (levels != null) {
						final String[] names = rniGetNames(objP);
						final boolean isOrdered = this.rEngine.rniInherits(objP, "ordered");
						final RFactorStore factorData = (structOnly) ?
								new RFactorDataStruct(rniGetLength(objTmp), isOrdered, levels.length) :
								new RFactorDataImpl(this.rEngine.rniGetIntArray(objP), isOrdered, levels);
						return new RVectorImpl<RFactorStore>(factorData, className1, names);
					}
				}
				final int[] dim = rniGetDim(objP);
				if (dim != null) {
					return new RArrayImpl<RIntegerStore>((structOnly) ?
							RObjectFactoryImpl.INT_STRUCT_DUMMY :
							RIntegerDataImpl.createForServer(this.rEngine.rniGetIntArray(objP)),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new RVectorImpl<RIntegerStore>(
									RObjectFactoryImpl.INT_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new RVectorImpl<RIntegerStore>(
									RIntegerDataImpl.createForServer(this.rEngine.rniGetIntArray(objP)),
									className1, rniGetNames(objP));
				} }
			case REXP.LGLSXP: { // logical vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				final int[] dim = rniGetDim(objP);
				if (dim != null) {
					return new RArrayImpl<RLogicalStore>((structOnly) ?
							RObjectFactoryImpl.LOGI_STRUCT_DUMMY :
							RLogicalDataIntImpl.createOtherTrue(this.rEngine.rniGetBoolArrayI(objP), 0, 2),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new RVectorImpl<RLogicalStore>(
									RObjectFactoryImpl.LOGI_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new RVectorImpl<RLogicalStore>(
									RLogicalDataIntImpl.createOtherTrue(this.rEngine.rniGetBoolArrayI(objP), 0, 2),
									className1, rniGetNames(objP));
				} }
			case REXP.CPLXSXP: { // complex vector / array
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				final int[] dim = rniGetDim(objP);
				if (dim != null) {
					return new RArrayImpl<RComplexStore>((structOnly) ?
							RObjectFactoryImpl.CPLX_STRUCT_DUMMY :
							RComplexDataBImpl.createForServer(rniGetComplexRe(objTmp), rniGetComplexIm(objTmp)),
							className1, dim);
				}
				else {
					return (structOnly) ?
							new RVectorImpl<RComplexStore>(
									RObjectFactoryImpl.CPLX_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null) :
							new RVectorImpl<RComplexStore>(
									RComplexDataBImpl.createForServer(rniGetComplexRe(objTmp), rniGetComplexIm(objTmp)),
									className1, rniGetNames(objP));
				} }
			case REXP.RAWSXP: { // raw/byte vector
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String className1 = rniGetClass1Save(objTmp);
				final int[] dim = rniGetDim(objP);
				final RRawStore rawData = /*(structOnly) ?*/
						new RRawDataStruct(rniGetLength(objTmp)) /*:
						RRawDataImpl.createForServer(rniGetRawArray(objP))*/;
				if (rawData.getLength() == 5 && className1.equals(".rj_eval.error")) {
					return null;
				}
				if (dim != null) {
					return new RArrayImpl<RRawStore>(/*(structOnly) ?*/
							RObjectFactoryImpl.RAW_STRUCT_DUMMY /*:
							RComplexDataBImpl.createForServer(rniGetComplexRe(objTmp), rniGetComplexIm(objTmp))*/,
							className1, dim);
				}
				else {
					return /*(structOnly) ?*/
							new RVectorImpl<RRawStore>(
									RObjectFactoryImpl.RAW_STRUCT_DUMMY,
									rniGetLength(objTmp), className1, null)/* :
							new RVectorImpl<RRawStore>(
									,
									className1, rniGetNames(objP))*/;
				} }
			case REXP.VECSXP: { // generic vector / list
				final long[] itemP = this.rEngine.rniGetVector(objP);
				final String[] itemNames = rniGetNames(objP);
				if (itemP.length > 16 && !tmpAssigned) {
					objTmp = ".rj_eval.temp"+(this.rniTemp-1);
					this.rEngine.rniAssign(objTmp, objP, 0);
					tmpAssigned = true;
				}
				if (this.rEngine.rniInherits(objP, "data.frame")) {
					final String[] rowNames = (structOnly) ? null : rniGetRowNames(objP);
					final RObject[] itemObjects = new RObject[itemP.length];
					for (int i = 0; i < itemP.length; i++) {
						final String itemTmp = objTmp+"[["+(i+1)+"]]";
						itemObjects[i] = rniCreateDataObject(itemP[i], itemTmp, structOnly);
					}
					return RDataFrameImpl.createForServer(itemObjects, rniGetClass1Save(objTmp), itemNames, rowNames);
				}
				else {
					final RObject[] itemObjects = new RObject[itemP.length];
					for (int i = 0; i < itemP.length; i++) {
						final String itemTmp = objTmp+"[["+(i+1)+"]]";
						itemObjects[i] = rniCreateDataObject(itemP[i], itemTmp, structOnly);
					}
					return new RListImpl(itemObjects, rniGetClass1Save(objTmp), itemNames);
				} }
			case REXP.LISTSXP:   // pairlist
			/*case REXP.LANGSXP: */{
				long cdr = objP;
				final int length = rniGetLength(objTmp);
				final String[] itemNames = new String[length];
				final RObject[] itemObjects = new RObject[length];
				for (int i = 0; i < length; i++) {
					final long car = this.rEngine.rniCAR(cdr);
					final long tag = this.rEngine.rniTAG(cdr);
					itemNames[i] = (tag != 0 && this.rEngine.rniExpType(tag) == REXP.SYMSXP) ?
							this.rEngine.rniGetSymbolName(tag) : null;
					itemObjects[i] = rniCreateDataObject(car, null, structOnly);
					cdr = this.rEngine.rniCDR(cdr);
					if (cdr == 0 || this.rEngine.rniExpType(cdr) != REXP.LISTSXP) {
						break;
					}
				} 
				return new RListImpl(itemObjects, rniGetClass1Save(objTmp), itemNames); }
			case REXP.ENVSXP: {
				if (this.rniTemp > 1) {
					return new RReferenceImpl(objP, RObject.TYPE_REFERENCE, "environment");
				}
				final long lsP = this.rEngine.rniEval(this.rEngine.rniParse("ls(name="+objTmp+",all.names=TRUE)", 1), 0);
				if (lsP != 0 && this.rEngine.rniExpType(lsP) == REXP.STRSXP) {
					// env name
					final long nameP = this.rEngine.rniEval(this.rEngine.rniParse("environmentName(env="+objTmp+')', 1), 0);
					final String name = (nameP != 0 && this.rEngine.rniExpType(nameP) == REXP.STRSXP) ? this.rEngine.rniGetString(nameP) : "";
					final boolean isAutoloadEnv = "Autoloads".equals(name); // TODO newer JRI provides direct access
					
					if (isAutoloadEnv) {
						return REnvironmentImpl.createForServer(name, objP, new RObject[0], new String[0], 0,
								rniGetClass1Save(objTmp));
					}
					
					final String[] names = this.rEngine.rniGetStringArray(lsP);
					if (names.length > 16 && !tmpAssigned) {
						objTmp = ".rj_eval.temp"+(this.rniTemp-1);
						this.rEngine.rniAssign(objTmp, objP, 0);
						tmpAssigned = true;
					}
					final RObject[] itemObjects = new RObject[names.length];
					int idx = 0;
					for (int i = 0; i < names.length; i++) {
						if (names[i] == null || names[i].startsWith(".rj_eval.temp")) {
							continue;
						}
						names[idx] = names[i];
						final String itemTmp = objTmp+"$`"+names[i]+'`';
						final long itemP = this.rEngine.rniEval(this.rEngine.rniParse(itemTmp, 1), 0);
						itemObjects[idx] = rniCreateDataObject(itemP, itemTmp, structOnly);
//						if (itemObjects[idx] == null) {
//							System.out.println("type="+ this.rEngine.rniExpType(itemP) + ", name=" + names[i]);
//						}
						idx++;
					}
					
					return REnvironmentImpl.createForServer(name, objP, itemObjects, names, idx,
							rniGetClass1Save(objTmp));
				}
				return null; }
			case REXP.CLOSXP: {
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				}
				final String header = rniGetFHeader(objTmp);
				return RFunctionImpl.createForServer(header); }
			case REXP.SPECIALSXP:
			case REXP.BUILTINSXP: {
				final String header = rniGetFHeader(objTmp);
				return RFunctionImpl.createForServer(header); }
			case REXP.S4SXP: {
				final RObject s4Obj = rniCheckAndCreateS4Obj(objTmp, structOnly);
				if (s4Obj != null) {
					return s4Obj;
				} }
			}
			
			return new ROtherImpl(rniGetClass1Save(objTmp));
		}
		finally {
			this.rniTemp--;
			if (tmpAssigned) {
				this.rEngine.rniEval(this.rEngine.rniParse("rm("+objTmp+");", 1), 0);
			}
		}
	}
	
	private RObject rniCheckAndCreateS4Obj(final String objTmp, final boolean structOnly) {
		final long classP = this.rEngine.rniEval(this.rEngine.rniParse("if (isS4("+objTmp+")) class("+objTmp+")", 1), 0);
		if (classP != 0 && this.rEngine.rniExpType(classP) == REXP.STRSXP) {
			final String className = this.rEngine.rniGetString(classP);
			if (className != null) {
				final long slotNamesP = this.rEngine.rniEval(this.rEngine.rniParse("slotNames(class("+objTmp+"))", 1), 0);
				if (slotNamesP != 0 && this.rEngine.rniExpType(slotNamesP) == REXP.STRSXP) {
					final String[] slotNames = this.rEngine.rniGetStringArray(slotNamesP);
					final RObject[] slotValues = new RObject[slotNames.length];
					for (int i = 0; i < slotNames.length; i++) {
						final String itemTmp = objTmp + "@`" + slotNames[i] + '`';
						final long slotValueP = this.rEngine.rniEval(this.rEngine.rniParse(itemTmp, 1), 0);
						slotValues[i] = rniCreateDataObject(slotValueP, itemTmp, structOnly);
					}
					return new RS4ObjectImpl(className, slotNames, slotValues);
				}
			}
		}
		return null;
	}
	
	private int[] rniGetDim(final long pointer) {
		final long dimP = this.rEngine.rniGetAttr(pointer, "dim");
		if (dimP != 0 && this.rEngine.rniExpType(dimP) == REXP.INTSXP) {
			return this.rEngine.rniGetIntArray(dimP);
		}
		return null;
	}
	
	private String[] rniGetNames(final long pointer) {
		final long namesP = this.rEngine.rniGetAttr(pointer, "names");
		if (namesP != 0 && this.rEngine.rniExpType(namesP) == REXP.STRSXP) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private String[] rniGetRowNames(final long pointer) {
		final long namesP = this.rEngine.rniGetAttr(pointer, "row.names");
		if (namesP != 0 && this.rEngine.rniExpType(namesP) == REXP.STRSXP) {
			return this.rEngine.rniGetStringArray(namesP);
		}
		return null;
	}
	
	private String[] rniGetLevels(final long pointer) {
		final long levelsP = this.rEngine.rniGetAttr(pointer, "levels");
		if (levelsP != 0 && this.rEngine.rniExpType(levelsP) == REXP.STRSXP) {
			return this.rEngine.rniGetStringArray(levelsP);
		}
		return null;
	}
	
	private int rniGetLength(final String tmp) {
		final long lengthP = this.rEngine.rniEval(this.rEngine.rniParse("length("+tmp+")", 1), 0);
		if (lengthP != 0 && this.rEngine.rniExpType(lengthP) == REXP.INTSXP) {
			final int[] length = this.rEngine.rniGetIntArray(lengthP);
			if (length.length == 1) {
				return length[0];
			}
		}
		return 0;
	}
	
	private double[] rniGetComplexRe(final String tmp) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniParse("Re("+tmp+")", 1), 0);
		if (numP != 0 && this.rEngine.rniExpType(numP) == REXP.REALSXP) {
			return this.rEngine.rniGetDoubleArray(numP);
		}
		throw new UnsupportedOperationException();
	}
	
	private double[] rniGetComplexIm(final String tmp) {
		final long numP = this.rEngine.rniEval(this.rEngine.rniParse("Im("+tmp+")", 1), 0);
		if (numP != 0 && this.rEngine.rniExpType(numP) == REXP.REALSXP) {
			return this.rEngine.rniGetDoubleArray(numP);
		}
		throw new UnsupportedOperationException();
	}
	
	private String rniGetClass1Save(final String tmp) {
		final long lengthP = this.rEngine.rniEval(this.rEngine.rniParse("class("+tmp+")", 1), 0);
		if (lengthP != 0 && this.rEngine.rniExpType(lengthP) == REXP.STRSXP) {
			return this.rEngine.rniGetString(lengthP);
		}
		return "<unknown>";
	}
	
	private String rniGetFHeader(final String tmp) {
		final long argsP = this.rEngine.rniEval(this.rEngine.rniParse("paste(deparse(expr= args("+tmp+"), width.cutoff= 500), collapse=\"\")", 1), 0);
		if (argsP != 0 && this.rEngine.rniExpType(argsP) == REXP.STRSXP) {
			final String args = this.rEngine.rniGetString(argsP);
			if (args != null && args.length() >= 11) { // "function ()".length
				return args;
			}
//			System.out.println("filtered header: " + args);
		}
		return null;
	}
	
	
	public String rReadConsole(final Rengine engine, final String prompt, final int addToHistory) {
		internalMainFromR(new ConsoleCmdItem.Read(
				(addToHistory == 1) ? V_TRUE : V_FALSE, prompt));
		if (this.mainLoopC2SCommandFirst.getStatus() == V_OK) {
			return this.mainLoopC2SCommandFirst.getDataText();
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
			internalMainFromR(new ConsoleCmdItem.Write(
					V_ERROR, text));
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
		internalMainFromR(new ConsoleCmdItem.Message(
				0, message));
	}
	
	public String rChooseFile(final Rengine engine, final int newFile) {
		internalMainFromR(new ExtUICmdItem(ExtUICmdItem.C_CHOOSE_FILE,
				(newFile == 1) ? ExtUICmdItem.O_NEW : 0, true));
		if (this.mainLoopC2SCommandFirst.getStatus() == V_OK) {
			return this.mainLoopC2SCommandFirst.getDataText();
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
		internalMainFromR(new ExtUICmdItem(command, 0, arg, wait));
		if (wait && this.mainLoopC2SCommandFirst instanceof ExtUICmdItem
				&& this.mainLoopC2SCommandFirst.getStatus() == V_OK) {
			return this.mainLoopC2SCommandFirst.getDataText();
		}
		return null;
	}
	
}
