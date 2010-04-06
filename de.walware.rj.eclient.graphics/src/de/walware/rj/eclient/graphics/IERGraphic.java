/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import de.walware.rj.graphic.RGraphic;


/**
 * R graphic.
 */
public interface IERGraphic extends RGraphic {
	
	
	int getDevId();
	
	String getLabel();
	
	boolean isActive();
	
	Object getRHandle();
	
	IStatus copy(String toDev, String toDevFile, String toDevArgs);
	
	IStatus resize(double w, double h);
	
	IStatus close();
	
	List<? extends IERGraphicInstruction> getInstructions();
	
}
