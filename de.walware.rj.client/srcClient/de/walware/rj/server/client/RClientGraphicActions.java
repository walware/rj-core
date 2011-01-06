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

import org.eclipse.core.runtime.IStatus;


/**
 * Interface for (RJ) graphics offering actions which can be used
 * in graphic views/windows.
 */
public interface RClientGraphicActions {
	
	
	Object getRHandle();
	
	String getRLabel();
	
	IStatus copyGraphic(int devId, String toDev, String toDevFile, String toDevArgs);
	
	IStatus resizeGraphic(int devId, Runnable beforeResize);
	
	IStatus closeGraphic(int devId);
	
}
