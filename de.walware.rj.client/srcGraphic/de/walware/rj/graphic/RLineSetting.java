/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.graphic;


/**
 * Configures line width and type.
 */
public class RLineSetting extends RPaintSetting {
	
	
	public static final int TYPE_BLANK= -1;
	public static final int TYPE_SOLID= 0;
	
	public static final byte CAP_ROUND= 1;
	public static final byte CAP_BUTT= 2;
	public static final byte CAP_SQUARE= 3;
	
	public static final byte JOIN_ROUND= 1;
	public static final byte JOIN_MITER= 2;
	public static final byte JOIN_BEVEL= 3;
	
	
	/**
	 * Line type, ~R: <code>par(lty)</code>
	 * <p>
	 * Values (not the predefined R constants, see line style encoding):
	 *     -1 = blank,
	 *     0 = solid (default),
	 *     encoded style
	 */
	public final int type;
	
	public final float width;
	
	public final byte cap;
	
	public final byte join;
	public final float joinMiterLimit;
	
	
	/**
	 * Creates a new line setting.
	 * 
	 * @param type line type
	 * @param width line width
	 */
	public RLineSetting(final int type, final float width,
			final byte cap, final byte join, final float joinMiterLimit) {
		this.type= type;
		this.width= width;
		this.cap= cap;
		this.join= join;
		this.joinMiterLimit= joinMiterLimit;
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
		final StringBuilder sb= new StringBuilder(30);
		sb.append("RLineSetting[");
		sb.append(this.type);
		sb.append(", ");
		sb.append(this.width);
		sb.append(", ");
		sb.append(this.cap);
		sb.append(", ");
		sb.append(this.join);
		sb.append(", ");
		sb.append(this.joinMiterLimit);
		sb.append("]");
		return sb.toString();
	}
	
}
