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

package de.walware.rj.eclient.graphics;

import java.util.List;


/**
 * Manager for a collection of R graphics.
 */
public interface IERGraphicsManager {
	
	
	interface Listener {
		
		void graphicAdded(IERGraphic graphic);
		
		void graphicRemoved(IERGraphic graphic);
		
	}
	
	interface ListenerShowExtension extends Listener {
		
		/**
		 * If the listener can show the graphic
		 * 
		 * <code>-1</code> not supported
		 * <code>&gt;= 0</code> priority (higher more appropriate)
		 * @param graphic the graphic to show
		 * @return
		 */
		int canShowGraphic(IERGraphic graphic);
		
		/**
		 * If the listener was selected (highest priority) to show the graphic
		 * 
		 * @param graphic the graphic to show
		 */
		void showGraphic(IERGraphic graphic);
		
	}
	
	
	void addListener(Listener listener);
	
	void removeListener(Listener listener);
	
	List<? extends IERGraphic> getAllGraphics();
	
}
