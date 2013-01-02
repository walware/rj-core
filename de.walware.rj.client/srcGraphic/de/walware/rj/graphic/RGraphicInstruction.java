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
 * Instruction to paint a R graphic.
 * 
 * An instruction has one of the instruction type defined in this interface.
 * 
 * @see RGraphic
 */
public interface RGraphicInstruction {
	
	
	byte INIT = 0;
	
	byte SET_CLIP = 1;
	byte SET_COLOR = 2;
	byte SET_FILL = 3;
	byte SET_LINE = 4;
	byte SET_FONT = 5;
	
	byte DRAW_LINE = 6;
	byte DRAW_RECTANGLE = 7;
	byte DRAW_POLYLINE = 8;
	byte DRAW_POLYGON = 9;
	byte DRAW_CIRCLE = 10;
	byte DRAW_TEXT = 11;
	byte DRAW_RASTER = 12;
	byte DRAW_PATH = 13;
	
	
	/**
	 * Gets the type of this instruction.
	 * 
	 * @return the instruction type
	 * @see RGraphicInstruction
	 */
	byte getInstructionType();
	
}
