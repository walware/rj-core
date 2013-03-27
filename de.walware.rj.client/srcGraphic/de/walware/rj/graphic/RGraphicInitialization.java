/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.graphic;


/**
 * First instruction of an R graphic.
 */
public class RGraphicInitialization implements RGraphicInstruction {
	
	
	/**
	 * The width the graphic is created for in R
	 */
	public final double width;
	
	/**
	 * The height the graphic is created for in R
	 */
	public final double height;
	
	/**
	 * The color of the device background
	 */
	public final int canvasColor;
	
	
	public RGraphicInitialization(final double width, final double height, final int canvasColor) {
		this.width = width;
		this.height = height;
		this.canvasColor = canvasColor;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return INIT;
	}
	
}
