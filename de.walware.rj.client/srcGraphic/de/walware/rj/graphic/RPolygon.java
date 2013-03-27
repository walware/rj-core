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
 * Polygon with vertices <code>(x[1], y[1])</code>, ..., <code>(x[n-1], y[n-1])</code>.
 */
public class RPolygon extends RGraphicElement {
	
	
	/**
	 * Coordinates of the vertices.
	 */
	public final double[] x, y;
	
	
	/**
	 * Creates a new polygon
	 * 
	 * @param x {@link #x}
	 * @param y {@link #y}
	 */
	public RPolygon(final double[] x, final double[] y) {
		this.x = x;
		this.y = y;
	}
	
	
	@Override
	public final byte getInstructionType() {
		return DRAW_POLYGON;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final int n = this.x.length;
		if (n == 0) {
			return "RPolygon[]";
		}
		final StringBuilder sb = new StringBuilder(14 + this.x.length*20);
		sb.append("RPolygon[(");
		sb.append(this.x[0]);
		sb.append(",");
		sb.append(this.y[0]);
		for (int i = 1; i < n; i++) {
			sb.append("), (");
			sb.append(this.x[i]);
			sb.append(",");
			sb.append(this.y[i]);
		}
		sb.append(")]");
		return sb.toString();
	}
	
}
