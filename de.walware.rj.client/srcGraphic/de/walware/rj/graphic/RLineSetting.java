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
 * Configures line width and type.
 */
public class RLineSetting extends RPaintSetting {
	
	
	/**
	 * Line type, ~R: <code>par(lty)</code>
	 * <p>
	 * Values (not the predefined R constants, see line style encoding):
	 *     -1 = blank,
	 *     0 = solid (default),
	 *     encoded style
	 */
	public final int type;
	
	public final double width;
	
	
	/**
	 * Creates a new line setting.
	 * 
	 * @param type line type
	 * @param width line width
	 */
	public RLineSetting(final int type, final double width) {
		this.type = type;
		this.width = width;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return SET_LINE;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(30);
		sb.append("RLineSetting[");
		sb.append(this.type);
		sb.append(", ");
		sb.append(this.width);
		sb.append("]");
		return sb.toString();
	}
	
}
