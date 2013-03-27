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
 * Configures the text font.
 */
public class RFontSetting extends RPaintSetting {
	
	/**
	 * Font family name, ~R: <code>par(familiy)</code>.
	 **/
	public final String family;
	
	/**
	 * Font face constant, ~R: <code>par(font)</code>.
	 * <p>
	 * Values:
	 *     1 = default,
	 *     2 = bold,
	 *     3 = italic,
	 *     4 = bold and italic,
	 *     5 = symbol.</p>
	 **/
	public final int face;
	
	/**
	 * Font size in points, ~R: ps * cex.
	 */
	public final double pointSize;
	
	/**
	 * The line height factor, ~R: <code>par(lheight)</code>
	 */
	public final double lineHeight;
	
	
	/**
	 * Creates a new font setting.
	 * 
	 * @param family font family name
	 * @param type face font face constant
	 * @param pointSize font size in points
	 * @param lineHeight line height factor
	 */
	public RFontSetting(final String family, final int face, final double pointSize,
			final double lineHeight) {
		this.family = family;
		this.face = face;
		this.pointSize = pointSize;
		this.lineHeight = lineHeight;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return SET_FONT;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(40 + this.family.length());
		sb.append("RFontParameter[");
		sb.append(this.family);
		sb.append(", ");
		sb.append(this.face);
		sb.append(", ");
		sb.append(this.pointSize);
		sb.append(", ");
		sb.append(this.lineHeight);
		sb.append("]");
		return sb.toString();
	}
	
}
