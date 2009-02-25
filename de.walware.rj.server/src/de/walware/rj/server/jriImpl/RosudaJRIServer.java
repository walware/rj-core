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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.rj.JRClassLoader;

import de.walware.rj.server.ConsoleCmdItem;
import de.walware.rj.server.ExtUICmdItem;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdList;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.server.ServerLocalExtension;
import de.walware.rj.server.ServerLocalPlugin;


/**
 * Remove server based on
 */
public class RosudaJRIServer implements Server, RMainLoopCallbacks, ServerLocalExtension {
	
	private static final int STATE_NOT_STARTED = 0;
	private static final int STATE_RUN_IN_R = 1;
	private static final int STATE_WAIT_FOR_CLIENT = 2;
	private static final int STATE_STOPPED = 4;
	
	private static final int CLIENT_NONE = 0;
	private static final int CLIENT_OK = 1;
	private static final int CLIENT_CANCEL = 2;
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server.jri");
	
	private static final int STDOUT_BUFFER_SIZE = 0x1FFF;
	
	
	static {
		System.loadLibrary("jri");
	}
	
	
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
	private MainCmdItem mainLoopClientAnswer;
	
	private long codeword;
	private int currentTicket;
	
	private final Object pluginsLock = new Object();
	private ServerLocalPlugin[] pluginsList = new ServerLocalPlugin[0];
	
	
	public RosudaJRIServer() {
		this.mainLoopState = STATE_NOT_STARTED;
		this.mainLoopClientNext = CLIENT_NONE;
	}
	
	
	public void addPlugin(final ServerLocalPlugin plugin) {
		if (plugin == null) {
			throw new IllegalArgumentException();
		}
		synchronized (this.pluginsLock) {
			final int oldSize = this.pluginsList.length;
			final ServerLocalPlugin[] newList = new ServerLocalPlugin[oldSize + 1];
			System.arraycopy(this.pluginsList, 0, newList, 0, oldSize);
			newList[oldSize] = plugin;
			this.pluginsList= newList;
		}
	}
	
	public void removePlugin(final ServerLocalPlugin plugin) {
		if (plugin == null) {
			return;
		}
		synchronized (this.pluginsLock) {
			final int oldSize = this.pluginsList.length;
			for (int i = 0; i < oldSize; i++) {
				if (this.pluginsList[i] == plugin) {
					final ServerLocalPlugin[] newList = new ServerLocalPlugin[oldSize - 1];
					System.arraycopy(this.pluginsList, 0, newList, 0, i);
					System.arraycopy(this.pluginsList, i + 1, newList, i, oldSize - i - 1);
					this.pluginsList = newList;
					return;
				}
			}
		}
	}
	
	private void handlePluginError(final ServerLocalPlugin plugin, final Throwable error) {
		// log and disable
		LOGGER.log(Level.SEVERE, "An error occured in plug-in '"+plugin.getSymbolicName()+"', plug-in will be disabled.", error);
		removePlugin(plugin);
		try {
			plugin.rjStop(V_ERROR);
		}
		catch (final Throwable stopError) {
			LOGGER.log(Level.WARNING, "An error occured when trying to disable plug-in '"+plugin.getSymbolicName()+"'.", error);
		}
	}
	
	
	private int newClient() {
		return ++this.currentTicket;
	}
	
	private RemoteException wrongClient() {
		return new RemoteException("Not connected.");
	}
	
	
	public synchronized int start(final long codeword, final String[] args) throws RemoteException {
		if (this.codeword != codeword) {
			throw new RemoteException("Wrong code word");
		}
		if (this.mainLoopState != STATE_NOT_STARTED) {
			throw new RemoteException("Illegal call: server already started");
		}
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			this.rClassLoader = JRClassLoader.getRJavaClassLoader();
			Thread.currentThread().setContextClassLoader(this.rClassLoader);
			
			this.rEngine = new Rengine(checkArgs(args), false, this);
			this.rEngine.setContextClassLoader(this.rClassLoader);
			
			this.rEngine.startMainLoop();
			if (!this.rEngine.waitForR()) {
				throw new IllegalStateException();
			}
			synchronized (this.mainLoopLock) {
				this.mainLoopClientNext = CLIENT_OK;
			}
		}
		catch (final Throwable e) {
			final String message = "Could not start the R engine";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
		return newClient();
	}
	
	private String[] checkArgs(final String[] args) {
		final List<String> checked = new ArrayList<String>(args.length+1);
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && args[i].length() > 0) {
				// add other checks here
				checked.add(args[i]);
			}
		}
		checked.add("--save");
		return checked.toArray(new String[checked.size()]);
	}
	
	public synchronized int connect(final long codeword) throws RemoteException {
		if (this.codeword != codeword) {
			throw new RemoteException("Wrong code word");
		}
		synchronized (this.mainLoopLock) {
			switch (this.mainLoopState) {
			case STATE_WAIT_FOR_CLIENT:
				this.mainLoopBusyAtClient = false;
				this.mainLoopClientNextCommands.addAll(0, Arrays.asList(this.mainLoopClientLastCommands.getItems()));
				this.currentTicket = newClient();
				return this.currentTicket;
			case STATE_RUN_IN_R:
				// exit old client
				if (this.mainLoopClientNext == CLIENT_OK) {
					this.mainLoopClientNext = CLIENT_CANCEL;
					this.mainLoopLock.notifyAll();
					while(this.mainLoopClientNext == CLIENT_CANCEL) {
						try {
							this.mainLoopLock.wait(100);
						} catch (final InterruptedException e) {
							Thread.interrupted();
						}
					}
				}
				// setup new client
				if (this.mainLoopClientNext != CLIENT_NONE) {
					throw new RemoteException();
				}
				this.mainLoopClientNext = CLIENT_OK;
				this.mainLoopBusyAtClient = false;
				return newClient();
			default:
				throw new RemoteException();
			}
		}
	}
	
	public void disconnect(final int ticket) throws RemoteException {
		if (ticket != this.currentTicket) {
			throw wrongClient();
		}
		synchronized (this.mainLoopLock) {
			switch (this.mainLoopState) {
			case STATE_WAIT_FOR_CLIENT:
				newClient();
				this.mainLoopClientNext = CLIENT_NONE;
				return;
			case STATE_RUN_IN_R:
				// exit old client
				this.mainLoopLock.notifyAll();
				while(this.mainLoopClientNext == CLIENT_CANCEL) {
					try {
						this.mainLoopLock.wait(100);
					} catch (final InterruptedException e) {
						Thread.interrupted();
					}
				}
				// setup new client
				if (this.mainLoopClientNext != CLIENT_NONE) {
					throw new RemoteException();
				}
				newClient();
				return;
			default:
				throw new RemoteException();
			}
		}
	}
	
	public void interrupt(final int ticket) throws RemoteException {
		if (ticket != this.currentTicket) {
			throw wrongClient();
		}
		try {
			this.rEngine.rniStop(1);
		}
		catch (final Throwable e) {
			LOGGER.log(Level.SEVERE, "An error occurred when trying to interrupt the R engine.", e);
		}
		return;
	}
	
	public RjsComObject runMainLoop(final int ticket, final RjsComObject command) throws RemoteException {
		if (ticket != this.currentTicket) {
			throw wrongClient();
		}
		this.mainLoopClientLastCommands.clear();
		if (command == null) {
			return internalMainCallbackFromClient(null);
		}
		switch (command.getComType()) {
		case T_PING:
			return RjsStatus.OK_STATUS;
		case RjsComObject.T_CONSOLE_READ_ITEM:
		case RjsComObject.T_CONSOLE_WRITE_ITEM:
		case RjsComObject.T_MESSAGE_ITEM:
		case RjsComObject.T_EXTENDEDUI_ITEM:
			return internalMainCallbackFromClient((MainCmdItem) command);
		}
		throw new RemoteException("Illegal call: unknown command");
	}
	
	
	public RjsComObject runAsync(final int ticket, final RjsComObject command) throws RemoteException {
		if (ticket != this.currentTicket) {
			throw wrongClient();
		}
		switch (command.getComType()) {
		case T_PING:
			return internalPing();
		}
		throw new RemoteException("Illegal call: unknown command");
	}
	
	
	private RjsStatus internalPing() {
		final Rengine r = this.rEngine;
		if (r.isAlive()) {
			return RjsStatus.OK_STATUS;
		}
		if (this.mainLoopState != STATE_STOPPED) {
			// invalid state
		}
		return new RjsStatus(RjsStatus.V_WARNING, S_STOPPED);
	}
	
	private RjsComObject internalMainCallbackFromClient(final MainCmdItem clientCommand) {
		synchronized (this.mainLoopLock) {
			if (this.mainLoopState == STATE_WAIT_FOR_CLIENT) {
				this.mainLoopClientAnswer = clientCommand;
			}
			
			this.mainLoopLock.notifyAll();
			while (this.mainLoopClientNextCommands.isEmpty()
					&& (this.mainLoopStdOutSize == 0)
					&& (this.mainLoopBusyAtClient == this.mainLoopBusyAtServer)
					&& (this.mainLoopState != STATE_STOPPED)) {
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
				if (size == 0 && this.mainLoopState == STATE_STOPPED) {
					return new RjsStatus(V_INFO, S_STOPPED);
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
				return new RjsStatus(V_CANCEL, S_DISCONNECTED);
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
				if (this.mainLoopState == STATE_STOPPED) {
					addCommand.setAnswer(V_ERROR);
					this.mainLoopClientAnswer = addCommand;
					return;
				}
				// wait for client
				this.mainLoopClientAnswer = null;
				this.mainLoopState = STATE_WAIT_FOR_CLIENT;
				final int stackId = ++this.mainLoopServerStack;
				try {
					if (Thread.currentThread() == this.rEngine) {
						while (this.mainLoopClientAnswer == null || this.mainLoopServerStack > stackId) {
							int i = 0;
							final ServerLocalPlugin[] plugins = this.pluginsList;
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
				this.mainLoopState = STATE_RUN_IN_R;
			}
		}
	}
	
	private void internalClearStdOutBuffer() {
		if (this.mainLoopStdOutSingle != null) {
			this.mainLoopClientNextCommands.add(new ConsoleCmdItem(
					RjsComObject.T_CONSOLE_WRITE_ITEM,
					V_OK, false, this.mainLoopStdOutSingle));
			this.mainLoopStdOutSingle = null;
		}
		else {
			this.mainLoopClientNextCommands.add(new ConsoleCmdItem(
					RjsComObject.T_CONSOLE_WRITE_ITEM,
					V_OK, false, new String(this.mainLoopStdOutBuffer, 0, this.mainLoopStdOutSize)));
		}
	}
	
	public String rReadConsole(final Rengine engine, final String prompt, final int addToHistory) {
		internalMainFromR(new ConsoleCmdItem(RjsComObject.T_CONSOLE_READ_ITEM,
				(addToHistory == 1) ? V_TRUE : V_FALSE,
						true, prompt));
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
		internalMainFromR(new ConsoleCmdItem(
				RjsComObject.T_CONSOLE_WRITE_ITEM,
				V_ERROR, false, text));
	}
	
	public void rFlushConsole(final Rengine engine) {
		internalMainFromR(null);
	}
	
	public void rBusy(final Rengine engine, final int which) {
		this.mainLoopBusyAtServer = (which == 1);
		internalMainFromR(null);
	}
	
	public void rShowMessage(final Rengine engine, final String message) {
		internalMainFromR(new ConsoleCmdItem(RjsComObject.T_MESSAGE_ITEM,
				0, false, message));
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
		internalMainFromR(new ExtUICmdItem(ExtUICmdItem.C_HISTORY_LOAD,
				0, true, filename));
	}
	
	public void rSaveHistory(final Rengine engine, final String filename) {
		internalMainFromR(new ExtUICmdItem(ExtUICmdItem.C_HISTORY_SAVE,
				0, true, filename));
	}
	
	public void rStopped() {
		synchronized (this.mainLoopLock) {
			if (this.mainLoopState == STATE_STOPPED) {
				return;
			}
			this.mainLoopState = STATE_STOPPED;
			
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
		
		final ServerLocalPlugin[] plugins;
		synchronized (this.pluginsLock) {
			plugins = this.pluginsList;
			this.pluginsList = new ServerLocalPlugin[0];
		}
		for (int i = 0; i < plugins.length; i++) {
			try {
				plugins[i].rjStop(V_OK);
			}
			catch (final Throwable e) {
				handlePluginError(plugins[i], e);
			}
		}
		
		this.rEngine = null;
	}
	
}
