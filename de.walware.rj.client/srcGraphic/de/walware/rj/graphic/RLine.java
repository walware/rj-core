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
 * Line with end points <code>(x0, y0)</code> and <code>(x1, y1)</code>.
 */
public class RLine extends RGraphicElement {
	
	
	public final double x0;
	public final double y0;
	public final double x1;
	public final double y1;
	
	
	/**
	 * Creates a new line
	 * 
	 * @param x0 x coordinate of point 1
	 * @param y0 y coordinate of point 1
	 * @param x1 x coordinate of point 2
	 * @param y1 y coordinate of point 2
	 */
	public RLine(final double x0, final double y0, final double x1, final double y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return DRAW_LINE;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(50);
		sb.append("RLine[(");
		sb.append(this.x0);
		sb.append(",");
		sb.append(this.y0);
		sb.append("), (");
		sb.append(this.x1);
		sb.append(",");
		sb.append(this.y1);
		sb.append(")]");
		return sb.toString();
	}
	
}
