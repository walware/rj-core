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

package de.walware.rj.server;

import java.io.File;
import java.rmi.Remote;

import de.walware.rj.RjException;
import de.walware.rj.data.RObjectFactory;


public class RjsComConfig {
	
	
	public static final String RJ_COM_S2C_ID_PROPERTY_ID = "rj.com.s2c.id";
	
	public static final String RJ_DATA_STRUCTS_LISTS_MAX_LENGTH_PROPERTY_ID = "rj.data.structs.lists.max_length";
	public static final String RJ_DATA_STRUCTS_ENVS_MAX_LENGTH_PROPERTY_ID = "rj.data.structs.envs.max_length";
	
	
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

}
