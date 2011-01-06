/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.client;


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
	 * Typically that means, the graphic isn't shown in the default graphic
	 * view/window.
	 */
	public static final int MANAGED_ON = 1 << 1;
	
	/**
	 * Indicates that the graphic will not be added to a global graphic collection.
	 * 
	 * Typically that means, the graphic isn't shown in the default graphic
	 * view/window.
	 */
	public static final int MANAGED_OFF = 1 << 2;
	
	/**
	 * Indicates that the graphic will be disposed if closed in R.
	 */
	public static final int R_CLOSE_ON = 1 << 3;
	
	/**
	 * Indicates that the graphic will not be disposed if closed in R.
	 */
	public static final int R_CLOSE_OFF = 1 << 4;
	
	
	/**
	 * Called if a new graphic is created in R (<code>dev.new()</code>).
	 * 
	 * @param devId
	 * @param w
	 * @param h
	 * @param active
	 * @param actions
	 * @return a new graphic object
	 */
	RClientGraphic newGraphic(final int devId, final double w, final double h,
			final boolean active, RClientGraphicActions actions, int options);
	
	/**
	 * Called if the graphic is closed by the R (<code>dev.off()</code>
	 * or R closed).
	 * 
	 * @param graphic the closed graphic
	 */
	void closeGraphic(RClientGraphic graphic);
	
}
