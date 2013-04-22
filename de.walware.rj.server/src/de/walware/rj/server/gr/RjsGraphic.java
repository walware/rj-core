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

package de.walware.rj.server.gr;

import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.MainCmdItem;
import de.walware.rj.server.RJ;


public /*final*/ class RjsGraphic {
	
	
	public static final int STATE_CLOSED = -1;
	public static final int STATE_OPENED = 1;
	public static final int STATE_PAGED = 2;
	
	
	private final RJ rj;
	private final RjsGraphicManager manager;
	
	private final byte slot;
	
	private int devId;
	int state;
	
	private int cachedStrWidthChar;
	private double[] cachedStrWidthCharResult;
	private String cachedStrWidthStr;
	private double[] cachedStrWidthStrResult;
	
	
	public RjsGraphic() {
		this.rj = RJ.get();
		this.manager = this.rj.getGraphicManager();
		this.slot = this.rj.getCurrentSlot();
	}
	
	
	public byte getSlot() {
		return this.slot;
	}
	
	public int getDevId() {
		return this.devId;
	}
	
	public int getState() {
		return this.state;
	}
	
	
	public void initPage(final int devId, final int state, final double width, final double height,
			final int canvasColor, final boolean isActive) {
		if (this.devId != devId || this.state < STATE_OPENED) {
			this.devId = devId;
			this.manager.registerGraphic(this);
		}
		this.state = state;
		
		this.cachedStrWidthChar = -1;
		this.cachedStrWidthStr = null;
		
		this.rj.sendMainCmd(new GDCmdItem.CInit(
				this.devId, width, height, canvasColor, isActive, this.slot ));
	}
	
	public void close() {
		this.state = STATE_CLOSED;
		this.manager.unregisterGraphic(this);
		this.rj.sendMainCmd(new GDCmdItem.CCloseDevice(
				this.devId, this.slot ));
	}
	
	public void setMode(final int mode) {
		if (this.state > 0) {
			this.rj.sendMainCmd(new GDCmdItem.CSetMode(
					this.devId, mode, this.slot ));
		}
	}
	
	public void activate() {
		if (this.state > 0) {
			this.manager.activate(this);
		}
	}
	
	public void deactivate() {
		if (this.state > 0) {
			this.manager.deactivate(this);
		}
	}
	
	public boolean newPageConfirm() {
		return false;
	}
	
	public double[] newPagePPI() {
		return (double[]) this.rj.getClientProperty(this.slot, "display.ppi");
	}
	
	public double[] newPageSize() {
		final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.CGetSize(
				this.devId, this.slot ));
		return (answer instanceof GDCmdItem && answer.isOK() ) ?
				((GDCmdItem) answer).getDoubleData() : null;
	}
	
	
	public void setColor(final int color) {
		this.rj.sendMainCmd(new GDCmdItem.SetColor(
				this.devId, color, this.slot ));
	}
	
	public void setFill(final int color) {
		this.rj.sendMainCmd(new GDCmdItem.SetFill(
				this.devId, color, this.slot ));
	}
	
	public void setLine(final int lty, final double lwd) {
		this.rj.sendMainCmd(new GDCmdItem.SetLine(
				this.devId, lty, lwd, this.slot ));
	}
	
	public void setClip(final double x0, final double x1, final double y0, final double y1) {
		this.rj.sendMainCmd(new GDCmdItem.SetClip(
				this.devId, x0, y0, x1, y1, this.slot ));
	}
	
	public void drawLine(final double x0, final double y0, final double x1, final double y1) {
		this.rj.sendMainCmd(new GDCmdItem.DrawLine(
				this.devId, x0, y0, x1, y1, this.slot ));
	}
	
	public void drawRect(final double x0, final double y0, final double x1, final double y1) {
		this.rj.sendMainCmd(new GDCmdItem.DrawRect(
				this.devId, x0, y0, x1, y1, this.slot ));
	}
	
	public void drawPolyline(final double[] x, final double[] y) {
		this.rj.sendMainCmd(new GDCmdItem.DrawPolyline(
				this.devId, x, y, this.slot ));
	}
	
	public void drawPolygon(final double[] x, final double[] y) {
		this.rj.sendMainCmd(new GDCmdItem.DrawPolygon(
				this.devId, x, y, this.slot ));
	}
	
	public void drawPath(final int[] n, final double[] x, final double[] y, final int mode) {
		this.rj.sendMainCmd(new GDCmdItem.DrawPath(
				this.devId, n, x, y, mode, this.slot ));
	}
	
	public void drawCircle(final double x, final double y, final double r) {
		this.rj.sendMainCmd(new GDCmdItem.DrawCircle(
				this.devId, x, y, r, this.slot ));
	}
	
	
	public void setFont(final double pointSize, final double lineheight,
			final int face, final String family) {
		this.rj.sendMainCmd(new GDCmdItem.SetFont(
				this.devId, family, face, pointSize, lineheight, this.slot ));
		
		this.cachedStrWidthChar = -1;
		this.cachedStrWidthStr = null;
	}
	
	public double[] getStrWidth(final String str) {
		if (str.length() == 1) {
			final int ch = str.charAt(0);
			if (ch == this.cachedStrWidthChar) {
				return this.cachedStrWidthCharResult;
			}
			final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.CGetStrWidth(
					this.devId, str, this.slot ));
			this.cachedStrWidthCharResult = (answer instanceof GDCmdItem && answer.isOK() ) ?
					((GDCmdItem) answer).getDoubleData() : null;
			this.cachedStrWidthChar = ch;
			return this.cachedStrWidthCharResult;
		}
		else {
			if (str.equals(this.cachedStrWidthStr)) {
				return this.cachedStrWidthStrResult;
			}
			final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.CGetStrWidth(
					this.devId, str, this.slot ));
			this.cachedStrWidthStrResult = (answer instanceof GDCmdItem && answer.isOK() ) ?
					((GDCmdItem) answer).getDoubleData() : null;
			this.cachedStrWidthStr = str;
			return this.cachedStrWidthStrResult;
		}
	}
	
	public double[] getMetricInfo(final int ch) {
		final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.CGetFontMetric(
				this.devId, ch, this.slot ));
		return (answer instanceof GDCmdItem && answer.isOK() ) ?
				((GDCmdItem) answer).getDoubleData() : null;
	}
	
	public void drawText(final String str, final double x, final double y, final double rDeg, final double hAdj) {
		this.rj.sendMainCmd(new GDCmdItem.DrawText(
				this.devId, str, x, y, rDeg, hAdj, this.slot ));
	}
	
	
	public void drawRaster(final byte[] imgData, final boolean imgAlpha, final int imgW, final int imgH,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate) {
		this.rj.sendMainCmd(new GDCmdItem.DrawRaster(this.devId, imgData, imgAlpha, imgW, imgH, x, y, w, h,
				rDeg, interpolate, this.slot ));
	}
	
	public byte[] capture(final int[] dim) {
		final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.Capture(this.devId,
				dim[0], dim[1], this.slot ));
		return (answer instanceof GDCmdItem && answer.isOK() ) ?
				((GDCmdItem) answer).getByteData() : null;
	}
	
	
	public double[] execLocator() {
		final MainCmdItem answer = this.rj.sendMainCmd(new GDCmdItem.Locator(
				this.devId, this.slot ));
		return (answer instanceof GDCmdItem && answer.isOK() ) ?
				((GDCmdItem) answer).getDoubleData() : null;
	}
	
}
