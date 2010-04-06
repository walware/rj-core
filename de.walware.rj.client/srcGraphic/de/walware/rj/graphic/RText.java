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

package de.walware.rj.graphic;


/**
 * Text at position <code>(x, y)</code>, horizontal adjusted by <code>hAdj</code>
 * [0, 1] and rotated by <code>rDeg</code>.
 */
public class RText extends RGraphicElement {
	
	
	public final double x;
	public final double y;
	public final double horizontalAdjust;
	public final double rotateDegree;
	public final String text;
	
	
	/**
	 * Creates a new line.
	 * 
	 * @param x x coordinate of point 1
	 * @param y y coordinate of point 1
	 * @param hAdj horizontal adjust factor
	 * @param rDeg degrees to rotate
	 * @param text text
	 */
	public RText(final double x, final double y, final double hAdj, final double rDeg,
			final String text) {
		this.x = x;
		this.y = y;
		this.horizontalAdjust = hAdj;
		this.rotateDegree = rDeg;
		this.text = text;
	}
	
	
	public final byte getInstructionType() {
		return DRAW_TEXT;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(50 + this.text.length());
		sb.append("RText[(");
		sb.append(this.x);
		sb.append(",");
		sb.append(this.y);
		sb.append("), ");
		sb.append(this.horizontalAdjust);
		sb.append(", ");
		sb.append(this.rotateDegree);
		sb.append(", \"");
		sb.append(this.text);
		sb.append("\"]");
		return sb.toString();
	}
	
}
