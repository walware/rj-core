/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.security.AccessControlException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.walware.rj.RjException;
import de.walware.rj.data.RObjectFactory;


public class RjsComConfig {
	
	
	public static final String RJ_COM_S2C_ID_PROPERTY_ID = "rj.com.s2c.id";
	
	public static final String RJ_DATA_STRUCTS_LISTS_MAX_LENGTH_PROPERTY_ID = "rj.data.structs.lists.max_length";
	public static final String RJ_DATA_STRUCTS_ENVS_MAX_LENGTH_PROPERTY_ID = "rj.data.structs.envs.max_length";
	
	
	private static final Map<String, Object> PROPERTIES = new ConcurrentHashMap<String, Object>();
	
	
	public static interface PathResolver {
		File resolve(Remote client, String path) throws RjException;
	}
	
	
	public static void setServerPathResolver(final RjsComConfig.PathResolver resolver) {
		BinExchange.gSPathResolver = resolver;
	}
	
	
	public static int registerClientComHandler(final ComHandler handler) {
		if (handler == null) {
			throw new NullPointerException();
		}
		final int id = MainCmdS2CList.gComHandlers.put(handler);
		if (id < 0xffff) {
			return id;
		}
		MainCmdS2CList.gComHandlers.remove(id);
		throw new UnsupportedOperationException("Too much open clients");
	}
	
	public static void unregisterClientComHandler(final int id) {
		MainCmdS2CList.gComHandlers.remove(id);
	}
	
	
	/**
	 * Registers an additional RObject factory
	 * 
	 * Factory registration is valid for the current VM.
	 */
	public static final void registerRObjectFactory(final String id, final RObjectFactory factory) {
		if (id == null || factory == null) {
			throw new NullPointerException();
		}
		if (id.equals(DataCmdItem.DEFAULT_FACTORY_ID)) {
			throw new IllegalArgumentException();
		}
		DataCmdItem.gFactories.put(id, factory);
	}
	
	/**
	 * Sets the default RObject factory
	 * 
	 * Factory registration is valid for the current VM.
	 */
	public static final void setDefaultRObjectFactory(final RObjectFactory factory) {
		if (factory == null) {
			throw new NullPointerException();
		}
		DataCmdItem.gDefaultFactory = factory;
		DataCmdItem.gFactories.put(DataCmdItem.DEFAULT_FACTORY_ID, factory);
	}
	
	public static final void setProperty(final String key, final Object value) {
		PROPERTIES.put(key, value);
	}
	
	public static final Object getProperty(final String key) {
		return PROPERTIES.get(key);
	}
	
	
	private static final ThreadLocal<RMIClientSocketFactory> gRMIClientSocketFactoriesInit = new ThreadLocal<RMIClientSocketFactory>();
	private static final ConcurrentHashMap<String, RMIClientSocketFactory> gRMIClientSocketFactories = new ConcurrentHashMap<String, RMIClientSocketFactory>();
	
	
	private static RMIClientSocketFactory getSystemRMIClientSocketFactory() {
		RMIClientSocketFactory factory = RMISocketFactory.getSocketFactory();
		if (factory == null) {
			factory = RMISocketFactory.getDefaultSocketFactory();
		}
		return factory;
	}
	
	private static final class RjRMIClientSocketFactory implements RMIClientSocketFactory, Externalizable {
		
		
		private static final long serialVersionUID = -2470426070934072117L;
		
		private static String getLocalHostName() {
			try {
				return InetAddress.getLocalHost().getCanonicalHostName();
			}
			catch (final UnknownHostException e) {}
			catch (final ArrayIndexOutOfBoundsException e) { /* JVM bug */ }
			return "unknown";
		}
		
		
		private String id;
		private RMIClientSocketFactory resolvedFactory;
		
		
		@SuppressWarnings("unused")
		public RjRMIClientSocketFactory() {
		}
		
		public RjRMIClientSocketFactory(final String init) {
			final StringBuilder sb = new StringBuilder(init);
			sb.append(getLocalHostName());
			sb.append('/').append(System.nanoTime()).append('/').append(Math.random());
			this.id = sb.toString();
		}
		
		
		@Override
		public void writeExternal(final ObjectOutput out) throws IOException {
			out.writeUTF(this.id);
		}
		@Override
		public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
			this.id = in.readUTF();
		}
		
		
		@Override
		public Socket createSocket(final String host, final int port) throws IOException {
			RMIClientSocketFactory factory = null;
			factory = gRMIClientSocketFactoriesInit.get();
			if (factory != null) {
				this.resolvedFactory = factory;
				gRMIClientSocketFactories.put(this.id, factory);
			}
			else {
				factory = this.resolvedFactory;
				if (factory == null) {
					factory = gRMIClientSocketFactories.get(this.id);
					if (factory != null) {
						this.resolvedFactory = factory;
					}
					else {
						factory = getSystemRMIClientSocketFactory();
					}
				}
			}
			return factory.createSocket(host, port);
		}
		
		
		@Override
		public int hashCode() {
			return this.id.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			return (this == obj
					|| (obj instanceof RjRMIClientSocketFactory
							&& this.id.equals(((RjRMIClientSocketFactory) obj).id) ));
		}
		
	}
	
	private final static boolean RMISERVER_CLIENTSOCKET_FACTORY_ENABLED;
	
	private static RMIClientSocketFactory RMISERVER_CLIENTSOCKET_FACTORY;
	
	static {
		{	boolean enabled = true;
			try {
				if ("true".equalsIgnoreCase(System.getProperty("de.walware.rj.rmi.disableSocketFactory"))) {
					enabled = false;
				}
			}
			catch (final AccessControlException e) { // in RMI registry
			}
			RMISERVER_CLIENTSOCKET_FACTORY_ENABLED = enabled;
		}
	}
	
	public static synchronized final RMIClientSocketFactory getRMIServerClientSocketFactory() {
		if (RMISERVER_CLIENTSOCKET_FACTORY_ENABLED && RMISERVER_CLIENTSOCKET_FACTORY == null) {
			RMISERVER_CLIENTSOCKET_FACTORY = new RjRMIClientSocketFactory("S/");
		}
		return RMISERVER_CLIENTSOCKET_FACTORY;
	}
	
	public static final void setRMIClientSocketFactory(RMIClientSocketFactory factory) {
		if (factory == null) {
			factory = getSystemRMIClientSocketFactory();
		}
		gRMIClientSocketFactoriesInit.set(factory);
	}
	
	public static final void clearRMIClientSocketFactory() {
		gRMIClientSocketFactoriesInit.set(null);
	}
	
}
