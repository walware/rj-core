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

package de.walware.rj.server;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.walware.rj.server.srvImpl.AbstractServerControl;
import de.walware.rj.server.srvImpl.DefaultServerImpl;
import de.walware.rj.server.srvext.ServerAuthMethod;


/**
 * Provides java and command line interface to control the RMI server.
 */
public class RMIServerControl extends AbstractServerControl {
	
	
	public static void main(final String[] args) {
		final String command = cliCommandName(args);
		if ("start".equals(command)) {
			final String name = cliGetName(args);
			final RMIServerControl server = new RMIServerControl(name, cliGetArgs(args, 2));
			server.start();
			return;
		}
		if ("clean".equals(command)) {
			final String name = cliGetName(args);
			final RMIServerControl server = new RMIServerControl(name, cliGetArgs(args, 2));
			server.clean();
			return;
		}
		cliInvalidArgs("Invalid server command '"+command+"'");
	}
	
	private static String cliCommandName(final String[] args) {
		if (args.length <= 0 || args[0].startsWith("-")) {
			cliInvalidArgs("Missing server command");
		}
		return args[0];
	}
	
	private static String cliGetName(final String[] args) {
		if (args.length <= 1) {
			cliInvalidArgs("Missing server name");
		}
		return args[1];
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
		System.out.println("  -verbose        verbose logging");
		System.out.println("  -plugins=<..>   list of plugins");
		System.out.println("  -auth=<..>      authetification method");
	}
	
	
	
	public RMIServerControl(final String name, final Map<String, String> args) {
		super(name, args);
	}
	
	
	public void start() {
		LOGGER.log(Level.INFO, "{0} initialize server...", this.logPrefix);
		
		ServerAuthMethod authMethod;
		try {
			authMethod = createServerAuth(this.args.remove("auth"));
		}
		catch (final Exception e) {
			System.exit(AbstractServerControl.EXIT_INIT_AUTHMETHOD_ERROR);
			throw new IllegalStateException();
		}
		final DefaultServerImpl server = new DefaultServerImpl(this.name, this, authMethod);
		if (!initREngine(server)) {
			System.exit(EXIT_INIT_RENGINE_ERROR | 1);
		}
		
		publishServer(server);
	}
	
	public void clean() {
		if (unbindDead()) {
			System.exit(0);
			return;
		}
		if (!this.args.containsKey("force")) {
			System.exit(EXIT_REGITRY_SERVER_STILL_ACTIVE);
		}
		try {
			Naming.unbind(this.name);
			LOGGER.log(Level.INFO,
					"{0} server removed from registry.",
					this.logPrefix);
		}
		catch (final NotBoundException e) {
			final LogRecord record = new LogRecord(Level.INFO,
					"{0} server is not registered.");
			record.setParameters(new Object[] { this.logPrefix });
			record.setThrown(e);
			LOGGER.log(record);
			
			System.exit(0);
		}
		catch (final Exception e) {
			final LogRecord record = new LogRecord(Level.SEVERE,
					"{0} removing server from registry failed.");
			record.setParameters(new Object[] { this.logPrefix });
			record.setThrown(e);
			LOGGER.log(record);
			
			System.exit(EXIT_INIT_PROBLEM);
		}
	}
	
	void scheduleCleanup() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				checkCleanup();
			}
		}, 2000);
	}
	
	@Override
	protected boolean unbindDead() {
		Remote remote;
		try {
			remote = Naming.lookup(this.name);
		}
		catch (final Exception lookupException) {
			return true;
		}
		if (!(remote instanceof Server)) {
			return false;
		}
		try {
			((Server) remote).getInfo();
			return false;
		}
		catch (final RemoteException deadException) {
			try {
				Naming.unbind(this.name);
				LOGGER.log(Level.INFO,
						"{0} dead server removed from registry.",
						this.logPrefix);
				return true;
			}
			catch (final Exception unbindException) {
				return false;
			}
		}
	}
	
}
