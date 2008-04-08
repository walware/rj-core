/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.walware.rj.server.jriImpl.RosudaJRILoader;


/**
 * Provides java and command line interface to control the RMI server.
 */
public class RMIServerControl {
	
	
	private static final int EXIT_INVALID_ARGS = 1;
	private static final int EXIT_REGISTRY_PROBLEM = 2;
	private static final int EXIT_INIT_PROBLEM = 3;
	
	private static final Logger LOGGER = Logger.getLogger("de.walware.rj.server");
	
	
	public static void main(final String[] args) {
		if (args.length == 0) {
			cliInvalidArgs("Missing server command");
		}
		final String command = args[0].toLowerCase();
		if ("start".equals(command)) {
			final String name = cliGetName(args);
			final RMIServerControl server = new RMIServerControl(name, cliGetArgs(args));
			server.start();
		}
		else if ("clean".equals(command)) {
			final String name = cliGetName(args);
			final RMIServerControl server = new RMIServerControl(name, cliGetArgs(args));
			server.clean();
		}
	}
	
	private static String cliGetName(final String[] args) {
		if (args.length <= 1) {
			cliInvalidArgs("Missing server name");
		}
		return args[1];
	}
	
	private static Map<String, String> cliGetArgs(final String[] args) {
		final Map<String, String> resolved = new HashMap<String, String>();
		for (int i = 2; i < args.length; i++) {
			if (args[i].length() == 0 || args[i].charAt(0) != '-') {
				continue;
			}
			final int split = args[i].indexOf(':');
			if (split > 1) {
				resolved.put(args[i].substring(1, split), args[i].substring(split+1));
			}
		}
		if (System.getProperty("de.walware.rj.verbose", "false").equals("true")) {
			resolved.put("verbose", "true");
		}
		return resolved;
	}
	
	private static void cliInvalidArgs(final String message) {
		System.err.println(message);
		cliPrintHelp();
		System.exit(EXIT_INVALID_ARGS);
	}
	
	private static void cliPrintHelp() {
		System.out.println("= Usage of RJserver command line control ================");
		System.out.println("commands:");
		System.out.println("  start <name>   adds server to RMIregistry");
		System.out.println("  clean <name>   stops server/remove from RMIregistry");
		System.out.println("name:            unique name in RMIregistry");
		System.out.println("options:");
		System.out.println("  verbose        verbose logging");
		System.out.println("  plugins:<..>   list of plugins");
	}
	
	public static void initVerbose() {
		LOGGER.setLevel(Level.ALL); // default is Level.INFO
		RemoteServer.setLog(System.err);
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
		
		Logger logger = LOGGER;
		SEARCH_CONSOLE: while (logger != null) {
			for (final Handler handler : logger.getHandlers()) {
				if (handler instanceof ConsoleHandler) {
					handler.setLevel(Level.ALL);
					break SEARCH_CONSOLE;
				}
			}
			if (!logger.getUseParentHandlers()) {
				break SEARCH_CONSOLE;
			}
			logger = logger.getParent();
		}
		
		LOGGER.log(Level.CONFIG, "verbose mode enabled.");
		LOGGER.log(Level.CONFIG, "java properties: "+System.getProperties().toString());
		LOGGER.log(Level.CONFIG, "env variables: "+System.getenv().toString());
	}
	
	
	private final String name;
	private final Map<String, String> args;
	private Server server;
	private boolean isOk;
	
	
	public RMIServerControl(final String name, final Map<String, String> args) {
		this.name = name;
		this.args = args;
		if (args.containsKey("verbose")) {
			initVerbose();
		}
	}
	
	
	public void start() {
		LOGGER.log(Level.INFO,
				"[{0}] initialize server...", this.name);
		
		Server newServer = null;
		try {
			newServer = new RosudaJRILoader().loadServer(this.args, new ServerLocalPlugin() {
				
				public String getSymbolicName() {
					return "rmi";
				}
				
				public void rjIdle() throws Exception {
				}
				
				public void rjStop(final int state) throws Exception {
					if (state == 0) {
						try {
							Thread.sleep(1000);
						}
						catch (final InterruptedException e) {
							Thread.interrupted();
						}
					}
					checkCleanup();
				};
				
			});
		}
		catch (final Throwable e) {
			final LogRecord record = new LogRecord(Level.SEVERE,
					"[{0}] init JRI/Rengine failed.");
			record.setParameters(new Object[] { this.name });
			record.setThrown(e);
			LOGGER.log(record);
			
			checkCleanup();
			
			System.exit(EXIT_INIT_PROBLEM);
		}
		try {
			System.setSecurityManager(new SecurityManager());
			final Server stub = (Server) UnicastRemoteObject.exportObject(newServer, 0);
			this.server = newServer;
			Naming.bind(this.name, stub);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					checkCleanup();
				}
			});
			this.isOk = true;
			LOGGER.log(Level.INFO,
					"[{0}] server is added to registry - ready.", this.name);
			
			return;
		}
		catch (final Exception e) {
			final LogRecord record = new LogRecord(Level.SEVERE,
					"[{0}] init server failed.");
			record.setParameters(new Object[] { this.name });
			record.setThrown(e);
			LOGGER.log(record);
			
			checkCleanup();
			
			if (e instanceof AlreadyBoundException) {
				System.exit(EXIT_REGISTRY_PROBLEM);
			}
			System.exit(EXIT_INIT_PROBLEM);
		}
	}
	
	public void clean() {
		LOGGER.log(Level.INFO, "[{0}] removing Server from registry...", this.name);
		try {
			Naming.unbind(this.name);
		}
		catch (final NotBoundException e) {
			final LogRecord record = new LogRecord(Level.INFO,
					"[{0}] server is not registered.");
			record.setParameters(new Object[] { this.name });
			record.setThrown(e);
			LOGGER.log(record);
			
			System.exit(EXIT_REGISTRY_PROBLEM);
		}
		catch (final Exception e) {
			final LogRecord record = new LogRecord(Level.SEVERE,
					"[{0}] remove server from registry failed.");
			record.setParameters(new Object[] { this.name });
			record.setThrown(e);
			LOGGER.log(record);
			
			System.exit(EXIT_INIT_PROBLEM);
		}
	}
	
	protected void checkCleanup() {
		if (this.server == null) {
			return;
		}
		LOGGER.log(Level.INFO, "[{0}] cleaning up server resources...", this.name);
		try {
			Naming.unbind(this.name);
		}
		catch (final NotBoundException e) {
			// ok
		}
		catch (final Exception e) {
			final LogRecord record = new LogRecord(this.isOk ? Level.SEVERE : Level.INFO,
					"[{0}] clean up server resources failed.");
			record.setParameters(new Object[] { this.name });
			record.setThrown(e);
			LOGGER.log(record);
		}
		try {
			UnicastRemoteObject.unexportObject(this.server, true);
		}
		catch (final NoSuchObjectException e) {
			// ok
		}
		this.server = null;
	}
	
	void scheduleCleanup() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				checkCleanup();
			}
		}, 2000);
	}
	
}
