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

package de.walware.rj.services;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.graphic.RGraphic;


/**
 * Controls the creation of {@link RGraphic}.
 * 
 * The creator can be access by {@link RService#createRGraphicCreator(int)}.
 * A creator can be used multiple times for different graphics. Properties
 * are reused and not reseted.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 0.5.0
 * @provisional
 */
public interface RGraphicCreator {
	
	/**
	 * Sets the size in pixel of the graphic to create.
	 * 
	 * @param width width in pixel
	 * @param height height in pixel
	 */
	void setSize(double width, double height);
	
	RGraphic create(String expression, IProgressMonitor monitor) throws CoreException;
	
	RGraphic create(FunctionCall fcall, IProgressMonitor monitor) throws CoreException;
	
}
