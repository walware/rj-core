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
 * Text at position <code>(x, y)</code>, horizontal adjusted by <code>hAdj</code>
 * [0, 1] and rotated by <code>rDeg</code>.
 */
public class RText extends RGraphicElement {
	
	/**
	 * The text to draw.
	 */
	public final String text;
	/**
	 * The position where to draw the text.
	 */
	public final double x, y;
	/**
	 * The factor for the horizontal adjustment of the text.
	 */
	public final double horizontalAdjust;
	/**
	 * The degrees for rotation of the text.
	 */
	public final double rotateDegree;
	
	
	/**
	 * Creates a new text element.
	 * 
	 * @param x {@link #x}
	 * @param y {@link #y}
	 * @param hAdj {@link #horizontalAdjust}
	 * @param rDeg {@link #rotateDegree}
	 * @param text {@link #text}
	 */
	public RText(final String text,
			final double x, final double y, final double rDeg, final double hAdj) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.horizontalAdjust = hAdj;
		this.rotateDegree = rDeg;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return DRAW_TEXT;
	}
	
	
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
