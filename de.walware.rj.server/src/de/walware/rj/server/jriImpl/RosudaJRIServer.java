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
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.rj.JRClassLoader;

import de.walware.rj.server.ConsoleCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdList;
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
	private boolean mainLoopBusyAtClient = false;
	private int mainLoopClientNext;
	private int mainLoopClientListen;
	private int mainLoopServerStack;
	private final char[] mainLoopStdOutBuffer = new char[STDOUT_BUFFER_SIZE];
	private String mainLoopStdOutSingle;
	private int mainLoopStdOutSize;
	private final ArrayList<MainCmdItem> mainLoopClientNextCommands = new ArrayList<MainCmdItem>();
	private final MainCmdList mainLoopClientLastCommands = new MainCmdList();
	private MainCmdItem mainLoopClientRequest;
	private MainCmdItem mainLoopClientAnswer;
	private int mainLoopClientAnswerFail;
	
	private int serverState;
	
	private ServerAuthMethod authMethod;
	private String currentMainClientId;
	private int currentTicket;
	
	private final Object pluginsLock = new Object();
	private ServerRuntimePlugin[] pluginsList = new ServerRuntimePlugin[0];
	
	
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
		catch (RjException e) {
			String message = "Initializing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
	}
	
	private int connectClient(ServerLogin login) throws RemoteException, LoginException {
		try {
			String username = this.authMethod.performLogin(login);
			this.currentMainClientId = RemoteServer.getClientHost();
			this.currentUsername = username;
		}
		catch (RjException e) {
			final String message = "Performing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
		catch (ServerNotActiveException e) {
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
	
	private void checkClient(long ticket) throws RemoteException {
		final String expectedClient = this.currentMainClientId;
		final String remoteClient;
		try {
			remoteClient = RemoteServer.getClientHost();
		}
		catch (ServerNotActiveException e) {
			throw new IllegalStateException(e);
		}
		if (this.currentTicket != ticket
				|| expectedClient == null 
				|| !expectedClient.equals(remoteClient)) {
			throw new ConnectException("Not connected.");
		}
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
			
			this.rEngine.startMainLoop();
			if (!this.rEngine.waitForR()) {
				throw new IllegalThreadStateException("R thread not started");
			}
			synchronized (this.mainLoopLock) {
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
		for (String arg : args) {
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
			switch (this.mainLoopState) {
			case ENGINE_WAIT_FOR_CLIENT:
				this.mainLoopBusyAtClient = false;
				this.mainLoopClientNext = CLIENT_OK;
				this.mainLoopClientNextCommands.addAll(0, Arrays.asList(this.mainLoopClientLastCommands.getItems()));
				if (this.mainLoopClientNextCommands.isEmpty()) {
					this.mainLoopClientNextCommands.add(this.mainLoopClientRequest);
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
				this.mainLoopBusyAtClient = false;
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
		this.mainLoopClientLastCommands.clear();
		if (command == null) {
			return internalMainCallbackFromClient(null);
		}
		else {
			switch (command.getComType()) {
			case T_PING:
				return RjsStatus.OK_STATUS;
			case RjsComObject.T_CONSOLE_READ_ITEM:
			case RjsComObject.T_CONSOLE_WRITE_ITEM:
			case RjsComObject.T_MESSAGE_ITEM:
			case RjsComObject.T_EXTENDEDUI_ITEM:
				return internalMainCallbackFromClient((MainCmdItem) command);
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
	
	private RjsComObject internalMainCallbackFromClient(final MainCmdItem clientCommand) {
		synchronized (this.mainLoopLock) {
			if (this.mainLoopState == ENGINE_WAIT_FOR_CLIENT) {
				if (clientCommand == null && this.mainLoopClientNextCommands.isEmpty()) {
					if (this.mainLoopClientAnswerFail == 0) {
						this.mainLoopClientAnswerFail++;
						return this.mainLoopClientRequest;
					}
					else {
						this.mainLoopClientAnswer = this.mainLoopClientRequest;
						this.mainLoopClientAnswer.setAnswer(RjsComObject.V_ERROR);
					}
				}
				else {
					this.mainLoopClientAnswerFail = 0;
					this.mainLoopClientAnswer = clientCommand;
				}
			}
			
			this.mainLoopLock.notifyAll();
			while (this.mainLoopClientNextCommands.isEmpty()
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
			
			if (this.mainLoopClientNext == CLIENT_OK) {
				if (this.mainLoopStdOutSize > 0) {
					internalClearStdOutBuffer();
					this.mainLoopStdOutSize = 0;
				}
				final int size = this.mainLoopClientNextCommands.size();
				if (size == 0 && this.mainLoopState == ENGINE_STOPPED) {
					return new RjsStatusImpl1(V_INFO, S_STOPPED);
				}
				this.mainLoopBusyAtClient = this.mainLoopBusyAtServer;
				this.mainLoopClientLastCommands.setBusy(this.mainLoopBusyAtClient);
				this.mainLoopClientLastCommands.setObjects(
						this.mainLoopClientNextCommands.toArray(new MainCmdItem[size]));
				this.mainLoopClientNextCommands.clear();
				if (size > 1024) {
					this.mainLoopClientNextCommands.trimToSize();
					this.mainLoopClientNextCommands.ensureCapacity(64);
				}
				return this.mainLoopClientLastCommands;
			}
			else {
				this.mainLoopClientNext = CLIENT_NONE;
				return new RjsStatusImpl2(V_CANCEL, S_DISCONNECTED, 
						(this.currentUsername != null) ? ("user "+ this.currentUsername) : null);
			}
		}
	}
	
	private void internalMainFromR(final MainCmdItem addCommand) {
		synchronized (this.mainLoopLock) {
			if (addCommand != null) {
				if (this.mainLoopStdOutSize > 0) {
					internalClearStdOutBuffer();
					this.mainLoopStdOutSize = 0;
				}
				this.mainLoopClientNextCommands.add(addCommand);
			}
			
			this.mainLoopLock.notifyAll();
			if (addCommand != null && addCommand.waitForClient()) {
				if (this.mainLoopState == ENGINE_STOPPED) {
					addCommand.setAnswer(V_ERROR);
					this.mainLoopClientAnswer = addCommand;
					return;
				}
				// wait for client
				this.mainLoopClientAnswer = null;
				this.mainLoopClientRequest = addCommand;
				this.mainLoopState = ENGINE_WAIT_FOR_CLIENT;
				final int stackId = ++this.mainLoopServerStack;
				try {
					if (Thread.currentThread() == this.rEngine) {
						while (this.mainLoopClientAnswer == null || this.mainLoopServerStack > stackId) {
							int i = 0;
							final ServerRuntimePlugin[] plugins = this.pluginsList;
							try {
								for (; i < plugins.length; i++) {
									plugins[i].rjIdle();
								}
								
								this.rEngine.rniIdle();
								
								this.mainLoopLock.wait(this.mainLoopServerStack > stackId ? 100 : 50);
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
						while (this.mainLoopClientAnswer == null || this.mainLoopServerStack > stackId) {
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
	}
	
	private void internalClearStdOutBuffer() {
		if (this.mainLoopStdOutSingle != null) {
			this.mainLoopClientNextCommands.add(new ConsoleCmdItem.Write(
					V_OK, this.mainLoopStdOutSingle));
			this.mainLoopStdOutSingle = null;
		}
		else {
			this.mainLoopClientNextCommands.add(new ConsoleCmdItem.Write(
					V_OK, new String(this.mainLoopStdOutBuffer, 0, this.mainLoopStdOutSize)));
		}
	}
	
	public String rReadConsole(final Rengine engine, final String prompt, final int addToHistory) {
		internalMainFromR(new ConsoleCmdItem.Read(
				(addToHistory == 1) ? V_TRUE : V_FALSE, prompt));
		if (this.mainLoopClientAnswer.getStatus() == V_OK) {
			return this.mainLoopClientAnswer.getDataText();
		}
		return "\n";
	}
	
	public void rWriteConsole(final Rengine engine, final String text, final int type) {
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
		if (this.mainLoopClientAnswer.getStatus() == V_OK) {
			return this.mainLoopClientAnswer.getDataText();
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
			while (this.mainLoopClientNextCommands.size() > 0 || this.mainLoopStdOutSize > 0) {
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
		if (wait && this.mainLoopClientAnswer instanceof ExtUICmdItem
				&& this.mainLoopClientAnswer.getStatus() == V_OK) {
			return this.mainLoopClientAnswer.getDataText();
		}
		return null;
	}
	
}
