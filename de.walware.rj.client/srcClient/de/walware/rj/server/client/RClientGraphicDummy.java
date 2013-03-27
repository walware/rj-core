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
	
	
	@Override
	public int getDevId() {
		return this.devId;
	}
	
	@Override
	public void reset(final double w, final double h, final InitConfig config) {
		this.size = new double[] { w, h };
	}
	
	@Override
	public void setMode(final int mode) {
	}
	
	@Override
	public void setActive(final boolean active) {
		this.isActive = active;
	}
	
	@Override
	public boolean isActive() {
		return this.isActive;
	}
	
	@Override
	public double[] computeSize() {
		return this.size;
	}
	
	@Override
	public double[] computeFontMetric(final int ch) {
		return null;
	}
	
	@Override
	public double[] computeStringWidth(final String txt) {
		return null;
	}
	
	@Override
	public void addSetClip(final double x0, final double y0, final double x1, final double y1) {
	}
	
	@Override
	public void addSetColor(final int color) {
	}
	
	@Override
	public void addSetFill(final int color) {
	}
	
	@Override
	public void addSetLine(final int type, final double width) {
	}
	
	@Override
	public void addSetFont(final String family, final int face, final double pointSize, final double lineHeight) {
	}
	
	@Override
	public void addDrawLine(final double x0, final double y0, final double x1, final double y1) {
	}
	
	@Override
	public void addDrawRect(final double x0, final double y0, final double x1, final double y1) {
	}
	
	@Override
	public void addDrawPolyline(final double[] x, final double[] y) {
	}
	
	@Override
	public void addDrawPolygon(final double[] x, final double[] y) {
	}
	
	@Override
	public void addDrawPath(final int[] n, final double[] x, final double[] y, final int winding) {
	}
	
	@Override
	public void addDrawCircle(final double x, final double y, final double r) {
	}
	
	@Override
	public void addDrawText(final String text,
			final double x, final double y, final double rDeg, final double hAdj) {
	}
	
	@Override
	public void addDrawRaster(final byte[] imgData, final boolean imgAlpha, final int imgWidth, final int imgHeight,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate) {
	}
	
	@Override
	public byte[] capture(final int width, final int height) {
		return null;
	}
	
	
	@Override
	public double[] runRLocator(final RService r, final IProgressMonitor monitor) {
		return null;
	}
	
}
