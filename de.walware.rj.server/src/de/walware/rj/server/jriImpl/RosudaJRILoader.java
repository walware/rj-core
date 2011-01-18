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

package de.walware.rj.server.jriImpl;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import org.rosuda.rj.JRIClassLoader;

import de.walware.rj.server.Server;
import de.walware.rj.server.srvImpl.InternalEngine;
import de.walware.rj.server.srvext.ExtServer;
import de.walware.rj.server.srvext.ServerRuntimePlugin;
import de.walware.rj.server.srvext.ServerUtil;


public final class RosudaJRILoader {
	
	
	public RosudaJRILoader() {
	}
	
	
	public InternalEngine loadServer(final String name, final Map<String, String> args,
			final Server publicServer, final ServerRuntimePlugin plugin) throws Exception {
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			final boolean verbose = args.containsKey("verbose");
			if (verbose) {
				JRIClassLoader.setDebug(1000);
			}
			final JRIClassLoader loader = JRIClassLoader.getRJavaClassLoader();
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
			
			final InternalEngine engine;
			try {
				Thread.currentThread().setContextClassLoader(loader);
				loader.loadRJavaClass("org.rosuda.JRI.REXP");
				loader.loadRJavaClass("org.rosuda.JRI.RMainLoopCallbacks");
				final Class<?> rEngineClazz = loader.loadRJavaClass("org.rosuda.JRI.Rengine");
				final Method versionMethod = rEngineClazz.getMethod("getVersion");
				final long version = ((Long) versionMethod.invoke(null)).longValue();
				String serverClazzName = "de.walware.rj.server.jriImpl.RosudaJRIServer";
				final Class<? extends InternalEngine> serverClazz = (Class<? extends InternalEngine>) loader.loadRJavaClass(serverClazzName);
				loader.loadRJavaClass(serverClazzName + "$InitCallbacks");
				
				engine = serverClazz.newInstance();
			}
			catch (final ClassNotFoundException e) {
				Logger.getLogger("de.walware.rj.server.jri").log(Level.INFO, "Perhaps autodetection of RJ classpath entry failed: " + 
						((myClasspath != null) ? myClasspath : "-"));
				throw e;
			}
			
			final ExtServer localServer = (ExtServer) engine;
			
			localServer.init(name, publicServer);
			
			// plugins
			final List<String> plugins = ServerUtil.getArgValueList(args.get("plugins"));
			if (plugins.contains("awt")) {
				UIManager.put("ClassLoader", loader);
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch (final Throwable e) {
				}
			}
			if (plugins.contains("swt")) {
				localServer.addPlugin((ServerRuntimePlugin) Class.forName("de.walware.rj.server.srvstdext.SWTPlugin").newInstance());
			}
			if (plugin != null) {
				localServer.addPlugin(plugin);
			}
			return engine;
		}
		finally {
			try {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
			catch (final Throwable e) { }
		}
	}
	
}
