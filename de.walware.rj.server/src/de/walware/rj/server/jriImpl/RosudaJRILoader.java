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

import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import org.rosuda.rj.JRClassLoader;

import de.walware.rj.server.Server;
import de.walware.rj.server.ServerLocalExtension;
import de.walware.rj.server.ServerLocalPlugin;


public class RosudaJRILoader {
	
	
	public RosudaJRILoader() {
	}
	
	
	public Server loadServer(final Map<String, String> args, final ServerLocalPlugin plugin) throws Exception {
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			boolean verbose = args.containsKey("verbose");
			if (verbose) {
				JRClassLoader.setDebug(1000);
			}
			final JRClassLoader loader = JRClassLoader.getRJavaClassLoader();
			if (verbose) {
				loader.setDefaultAssertionStatus(true);
			}
			
			// Add JR classpath entry to rJava ClassLoader
			// If this does not work, you can add in your command line to the rjava.class.path property
			final String myClassResource = getClass().getName().replace('.', '/')+".class";
			final URL url = getClass().getClassLoader().getResource(myClassResource);
			String myClasspath = null;
			if (url != null) {
				String s = url.toExternalForm();
				s = URLDecoder.decode(s, System.getProperty("file.encoding"));
				if (s.endsWith(myClassResource)) {
					s = s.substring(0, s.length()-myClassResource.length());
					if (s.startsWith("jar:") && s.endsWith("!/")) {
						s = s.substring(4, s.length()-2);
					}
					if (s.startsWith("file:")) {
						s = s.substring(5);
					}
					myClasspath = s;
					loader.addClassPath(s);
				}
			}
			
			final Server server;
			try {
				Thread.currentThread().setContextClassLoader(loader);
				final Class<Server> serverClazz = (Class<Server>) loader.loadRJavaClass("de.walware.rj.server.jriImpl.RosudaJRIServer");
				loader.loadRJavaClass("org.rosuda.JRI.REXP");
				loader.loadRJavaClass("org.rosuda.JRI.Rengine");
				
				server = serverClazz.newInstance();
			}
			catch (ClassNotFoundException e) {
				Logger.getLogger("de.walware.rj.server.jri").log(Level.INFO, "Perhaps autodetection of RJ classpath entry failed: " + 
						((myClasspath != null) ? myClasspath : "-"));
				throw e;
			}
			
			
			final String string = args.get("plugins");
			if (string != null && string.length() > 0) {
				final List<String> plugins = Arrays.asList(string.split(","));
				
				if (plugins.contains("awt")) {
					UIManager.put("ClassLoader", loader);
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					}
					catch (final Throwable e) {
					}
				}
				if (plugins.contains("swt")) {
					if (server instanceof ServerLocalExtension) {
						((ServerLocalExtension) server).addPlugin(new SWTPlugin());
					}
				}
			}
			if (plugin != null) {
				if (server instanceof ServerLocalExtension) {
					((ServerLocalExtension) server).addPlugin(plugin);
				}
			}
			return server;
		}
		finally {
			try {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
			catch (final Throwable e) { }
		}
	}
	
}
