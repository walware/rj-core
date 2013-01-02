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

package de.walware.rj.server.client;

import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.services.RService;


/**
 * A minimal {@link RClientGraphic} implementation.
 */
public class RClientGraphicDummy implements RClientGraphic {
	
	
	private final int devId;
	
	private boolean isActive;
	
	private double[] size;
	
	
	public RClientGraphicDummy(final int devId, final double w, final double h) {
		this.devId = devId;
		this.size = new double[] { w, h };
	}
	
	
	public int getDevId() {
		return this.devId;
	}
	
	public void reset(final double w, final double h, final InitConfig config) {
		this.size = new double[] { w, h };
	}
	
	public void setMode(final int mode) {
	}
	
	public void setActive(final boolean active) {
		this.isActive = active;
	}
	
	public boolean isActive() {
		return this.isActive;
	}
	
	public double[] computeSize() {
		return this.size;
	}
	
	public double[] computeFontMetric(final int ch) {
		return null;
	}
	
	public double[] computeStringWidth(final String txt) {
		return null;
	}
	
	public void addSetClip(final double x0, final double y0, final double x1, final double y1) {
	}
	
	public void addSetColor(final int color) {
	}
	
	public void addSetFill(final int color) {
	}
	
	public void addSetLine(final int type, final double width) {
	}
	
	public void addSetFont(final String family, final int face, final double pointSize, final double lineHeight) {
	}
	
	public void addDrawLine(final double x0, final double y0, final double x1, final double y1) {
	}
	
	public void addDrawRect(final double x0, final double y0, final double x1, final double y1) {
	}
	
	public void addDrawPolyline(final double[] x, final double[] y) {
	}
	
	public void addDrawPolygon(final double[] x, final double[] y) {
	}
	
	public void addDrawPath(final int[] n, final double[] x, final double[] y, final int winding) {
	}
	
	public void addDrawCircle(final double x, final double y, final double r) {
	}
	
	public void addDrawText(final String text,
			final double x, final double y, final double rDeg, final double hAdj) {
	}
	
	public void addDrawRaster(final byte[] imgData, final boolean imgAlpha, final int imgWidth, final int imgHeight,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate) {
	}
	
	public byte[] capture(final int width, final int height) {
		return null;
	}
	
	
	public double[] runRLocator(final RService r, final IProgressMonitor monitor) {
		return null;
	}
	
}
