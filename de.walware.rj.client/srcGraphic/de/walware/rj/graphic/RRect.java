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
 * Rectangle with the vertexes <code>(x0, y0)</code>, <code>(x0, y1)</code>,
 * <code>(x1, y1)</code>, <code>(x1, y1)</code>.
 */
public class RRect extends RGraphicElement {
	
	
	public final double x0;
	public final double y0;
	public final double x1;
	public final double y1;
	
	
	/**
	 * Creates a new rectangle.
	 * 
	 * @param x0 lower x coordinate
	 * @param y0 lower y coordinate
	 * @param x1 higher x coordinate
	 * @param y1 higher x coordinate
	 */
	public RRect(final double x0, final double y0, final double x1, final double y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return DRAW_RECTANGLE;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(50);
		sb.append("RRect[(");
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
