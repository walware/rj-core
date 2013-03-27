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
 * Circle with center <code>(x, y)</code> and radius <code>r</code>.
 */
public class RCircle extends RGraphicElement {
	
	
	public final double x;
	public final double y;
	public final double r;
	
	
	/**
	 * Creates a new circle.
	 * 
	 * @param x x coordinate of the center
	 * @param y y coordinate of the center
	 * @param r radius
	 */
	public RCircle(final double x, final double y, final double r) {
		this.x = x;
		this.y = y;
		this.r = r;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return DRAW_CIRCLE;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(40);
		sb.append("Circle[(");
		sb.append(this.x);
		sb.append(",");
		sb.append(this.y);
		sb.append("), ");
		sb.append(this.r);
		sb.append("]");
		return sb.toString();
	}
	
}
