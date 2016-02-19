/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.graphic;

import java.util.List;


/**
 * An RGraphic represents the client side of a graphic (device)
 * in R.
 * <p>
 * The attributes devId ({@link #getDevId()}) and active ({@link #isActive()},
 * {@link Listener#activated()}, {@link Listener#deactivated()}) are equivalent
 * to the device id and active device in R.</p>
 * <p>
 * A graphic has a sequence of graphic instructions describing how to paint the
 * graphic ({@link #getInstructions()}). If not empty, the first instruction is
 * of the type {@link RGraphicInstruction#INIT INIT}, followed by setting and
 * drawing instructions. Settings are valid for all following drawing instructions
 * until it is changed by a new setting instruction of the same type.</p>
 */
public interface RGraphic {
	
	
	interface Listener {
		
		void activated();
		void deactivated();
		void drawingStarted();
		void drawingStopped();
		
	}
	
	/**
	 * Returns the device id of the graphic in R. Note that the device number presented in public
	 * R functions is this device id + 1.
	 * 
	 * @return the device id
	 */
	int getDevId();
	
	/**
	 * Returns if graphic device of this graphic is the active device in R.
	 * 
	 * @return <code>true</code> if it is active, otherwise <code>false</code>
	 */
	boolean isActive();
	
	List<? extends RGraphicInstruction> getInstructions();
	
	void addListener(Listener listener);
	
	void removeListener(Listener listener);
	
}
