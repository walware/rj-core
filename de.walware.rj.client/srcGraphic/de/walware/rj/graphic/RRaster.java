/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.graphic;


/**
 * Raster image.
 */
public class RRaster extends RGraphicElement {
	
	/**
	 * The image data to draw (RGBA).
	 */
	public final byte[] imgData;
	/**
	 * The dimension of the {@link #imgData image}.
	 */
	public final int imgWidth, imgHeight;
	/**
	 * The position for the bottom-left corner where to draw the image.
	 */
	public final double x, y;
	/**
	 * The dimension where to draw the image.
	 */
	public final double width, height;
	/**
	 * The degree to rotate anti-clockwise the image.
	 */
	public final double rotateDegree;
	
	public final boolean interpolate;
	
	
	/**
	 * Creates a new raster image element.
	 * 
	 * @param imgData {@link #imgData}
	 * @param imgWidth {@link #imgWidth}
	 * @param imgHeight {@link #imgHeight}
	 * @param x {@link #x}
	 * @param y {@link #y}
	 * @param w {@link #width}
	 * @param h {@link #height}
	 * @param rDeg {@link #rotateDegree}
	 * @param interpolate {@link #interpolate}
	 */
	public RRaster(final byte[] imgData, final int imgWidth, final int imgHeight,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate) {
		this.imgData = imgData;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		this.rotateDegree = rDeg;
		this.interpolate = interpolate;
	}
	
	
	@Override
	public byte getInstructionType() {
		return DRAW_RASTER;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(50);
		sb.append("RRaster[(");
		sb.append(this.x);
		sb.append(",");
		sb.append(this.y);
		sb.append("), (");
		sb.append(this.width);
		sb.append(" x ");
		sb.append(this.height);
		sb.append(", ");
		sb.append(this.rotateDegree);
		sb.append(", \"");
		sb.append(this.interpolate);
		sb.append("\"]");
		return sb.toString();
	}
	

}
