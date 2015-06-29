/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.client;

import java.util.Map;

import de.walware.rj.server.client.RClientGraphic.InitConfig;


/**
 * Factory interface used to create (and close) {@link RClientGraphic} objects.
 * 
 * The methods are called by the client protocol handler
 * ({@link AbstractRJComClient}) and must not be called at
 * another place.
 */
public interface RClientGraphicFactory {
	
	
	/**
	 * Indicates explicitly that the graphic will be added to a global graphic
	 * collection.
	 * 
	 * Typically that means, the graphic is shown in the default graphic view/window.
	 */
	int MANAGED_ON = 1 << 1;
	
	/**
	 * Indicates that the graphic will not be added to a global graphic collection.
	 * 
	 * Typically that means, the graphic isn't shown in the default graphic view/window.
	 */
	int MANAGED_OFF = 1 << 2;
	
	/**
	 * Indicates that the graphic will be disposed if closed in R.
	 */
	int R_CLOSE_ON = 1 << 3;
	
	/**
	 * Indicates that the graphic will not be disposed if closed in R.
	 */
	int R_CLOSE_OFF = 1 << 4;
	
	/**
	 * Returns a map with properties configuring the server
	 * The values must be serializable.
	 * 
	 * @return a map with keys and values of the properties
	 */
	Map<String, ? extends Object> getInitServerProperties();
	
	/**
	 * Called if a new graphic is created in R (<code>dev.new()</code>).
	 * 
	 * @param devId
	 * @param w
	 * @param h
	 * @param config initialization configuration
	 * @param active
	 * @param actions
	 * @return a new graphic object
	 */
	RClientGraphic newGraphic(int devId, double w, double h, InitConfig config,
			boolean active, RClientGraphicActions actions, int options);
	
	/**
	 * Called if the graphic is closed by the R (<code>dev.off()</code>
	 * or R closed).
	 * 
	 * @param graphic the closed graphic
	 */
	void closeGraphic(RClientGraphic graphic);
	
}
