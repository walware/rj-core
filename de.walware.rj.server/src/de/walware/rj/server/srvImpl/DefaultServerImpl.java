/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvImpl;

import static de.walware.rj.server.srvext.ServerUtil.MISSING_ANSWER_STATUS;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import de.walware.rj.RjException;
import de.walware.rj.data.RObject;
import de.walware.rj.server.BinExchange;
import de.walware.rj.server.DataCmdItem;
import de.walware.rj.server.MainCmdC2SList;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.MainCmdS2CList;
import de.walware.rj.server.RjsComObject;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.Server;
import de.walware.rj.server.ServerInfo;
import de.walware.rj.server.ServerLogin;
import de.walware.rj.server.srvext.Client;
import de.walware.rj.server.srvext.ServerAuthMethod;


public class DefaultServerImpl implements Server, BinExchange.PathResolver {
	
	
	static int[] version() {
		return new int[] { 0, 5, 0 };
	}
	
	
	private static final List<Remote> clients = new CopyOnWriteArrayList<Remote>();
	
	public static void removeClient(final Remote remote) {
		clients.add(remote);
	}
	public static void addClient(final Remote remote) {
		clients.add(remote);
	}
	public static boolean isValid(final Remote remote) {
		return clients.contains(remote);
	}
	
	
	protected static final Logger LOGGER = Logger.getLogger("de.walware.rj.server");
	
	protected final AbstractServerControl control;
	protected InternalEngine internalEngine;
	
	private final String name;
	
	private final String[] userTypes;
	private String[] userNames;
	protected String workingDirectory;
	protected long timestamp;
	
	protected ServerAuthMethod consoleAuthMethod;
	
	protected final Client serverClient = new Client("rservi", "dummy", (byte) 1);
	private final MainCmdC2SList serverC2SList = new MainCmdC2SList();
	
	
	public DefaultServerImpl(final String name, final AbstractServerControl control, final ServerAuthMethod authMethod) {
		if (name == null || control == null) {
			throw new NullPointerException();
		}
		
		this.name = name;
		this.control = control;
		
		this.userTypes = createUserTypes();
		this.userNames = new String[this.userTypes.length];
		setUserName(ServerInfo.USER_OWNER, System.getProperty("user.name"));
		
		this.workingDirectory = System.getProperty("user.dir");
		
		this.consoleAuthMethod = authMethod;
	}
	
	
	protected String[] createUserTypes() {
		return new String[] { ServerInfo.USER_OWNER, ServerInfo.USER_CONSOLE };
	}
	
	protected void setUserName(final String type, final String name) {
		for (int i = 0; i < this.userTypes.length; i++) {
			if (this.userTypes[i].equals(type)) {
				if ((this.userNames[i] != null) ? !this.userNames[i].equals(name) : name != null) {
					final String[] newNames = new String[this.userTypes.length];
					System.arraycopy(this.userNames, 0, newNames, 0, this.userTypes.length);
					newNames[i] = name;
					this.userNames = newNames;
				}
				return;
			}
		}
	}
	
	
	void setEngine(final InternalEngine engine) {
		this.internalEngine = engine;
	}
	
	
	public int getState() throws RemoteException {
		return this.internalEngine.getState();
	}
	
	public int[] getVersion() throws RemoteException {
		return version();
	}
	
	public ServerInfo getInfo() throws RemoteException {
		return new ServerInfo(this.name, this.workingDirectory, this.timestamp,
				this.userTypes, this.userNames,
				this.internalEngine.getState());
	}
	
	
	protected ServerAuthMethod getAuthMethod(final String command) {
		if (command.startsWith("console.")) {
			return this.consoleAuthMethod;
		}
		throw new UnsupportedOperationException();
	}
	
	public final ServerLogin createLogin(final String command) throws RemoteException {
		final ServerAuthMethod authMethod = getAuthMethod(command);
		try {
			return authMethod.createLogin();
		}
		catch (final RjException e) {
			final String message = "Initializing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
	}
	
	protected Client connectClient(final String command, final ServerLogin login) throws RemoteException, LoginException {
		final ServerAuthMethod authMethod = getAuthMethod(command);
		try {
			return authMethod.performLogin(login);
		}
		catch (final RjException e) {
			final String message = "Performing login failed.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RemoteException(message, e);
		}
	}
	
	public Object execute(final String command, final Map<String, ? extends Object> properties, final ServerLogin login) throws RemoteException, LoginException {
		try {
			if (command.equals(C_CONSOLE_START)) {
				final Client client = connectClient(command, login);
				final Object r = this.internalEngine.start(client, properties);
				final Object startupTime = properties.get("rj.session.startup.time");
				if (startupTime instanceof Long) {
					this.timestamp = ((Long) startupTime).longValue();
				}
				return r;
			}
			if (command.equals(C_CONSOLE_CONNECT)) {
				final Client client = connectClient(command, login);
				final Object r = this.internalEngine.connect(client, properties);
				return r;
			}
			return null;
		}
		finally {
			final Client client = this.internalEngine.getCurrentClient();
			setUserName(ServerInfo.USER_CONSOLE, ((client != null) ? client.getUsername() : null));
		}
	}
	
	
	protected RObject runServerLoopCommand(RjsComObject sendCom, final MainCmdItem sendItem) throws RjException {
		DataCmdItem answer = null;
		try {
			this.serverC2SList.setObjects(sendItem);
			sendCom = this.serverC2SList;
			
			RjsComObject receivedCom = this.runMainLoop(sendCom, null);
			
			sendCom = null;
			WAIT_FOR_ANSWER: while (true) {
				COM_TYPE: switch (receivedCom.getComType()) {
				case RjsComObject.T_PING:
					sendCom = RjsStatus.OK_STATUS;
					break COM_TYPE;
				case RjsComObject.T_STATUS:
					final RjsStatus status = (RjsStatus) receivedCom;
					switch (status.getSeverity()) {
					case RjsStatus.OK:
						break COM_TYPE;
					case RjsStatus.INFO:
						if (status.getCode() == RjsStatus.CANCEL) {
							break WAIT_FOR_ANSWER;
						}
						break COM_TYPE;
					default:
						throw new RjException(status.getMessage());
					}
				case RjsComObject.T_MAIN_LIST:
					final MainCmdS2CList list = (MainCmdS2CList) receivedCom;
					answer = (DataCmdItem) list.getItems();
					break COM_TYPE;
				}
				receivedCom = this.runMainLoop(sendCom, null);
				sendCom = null;
			}
			this.serverC2SList.clear();
			if (answer == null || !answer.isOK()) {
				final RjsStatus status = (answer != null) ? answer.getStatus() : MISSING_ANSWER_STATUS;
				throw new RjException("R commands failed ("+status.getMessage()+").");
			}
			return answer.getData();
		}
		catch (final Exception e) {
			if (e instanceof RjException) {
				throw (RjException) e;
			}
			throw new RjException("An error when executing R command.", e);
		}
	}
	
	protected RjsComObject runMainLoop(final RjsComObject com, final Object caller) throws RemoteException {
		return this.internalEngine.runMainLoop(this.serverClient, com);
	}
	
	public File resolve(final Remote ref, final String path) throws RjException {
		final File file = new File(path);
		if (!DefaultServerImpl.isValid(ref)) {
			throw new RjException("Invalid access.");
		}
		if (file.isAbsolute()) {
			return file;
		}
		final RObject rwd = runServerLoopCommand(null, new DataCmdItem(DataCmdItem.EVAL_DATA, 0, "getwd()"));
		return new File(rwd.getData().getChar(0), file.getPath());
	}
	
}
