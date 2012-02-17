/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.gd;

import org.rosuda.javaGD.GDInterface;

import de.walware.rj.server.srvext.RjsGraphic;


public final class JavaGD extends GDInterface {
	
	
	private final RjsGraphic rjsGraphic;
	
	
	public JavaGD() {
		this.rjsGraphic = new RjsGraphic();
	}
	
	
	@Override
	public double[] gdInit(final double width, final double height, final int unit,
			double xpi, double ypi, final int canvasColor) {
		if (xpi <= 0.0 || ypi <= 0.0) {
			try {
				final double[] ppi = this.rjsGraphic.newPagePPI();
				if (ppi != null && ppi.length == 2) {
					xpi = ppi[0];
					ypi = ppi[1];
				}
			}
			catch (final Exception e) {
				xpi = 0.0;
				ypi = 0.0;
			}
		}
		return super.gdInit(width, height, unit, xpi, ypi, canvasColor);
	}
	
	@Override
	public void gdOpen(final int devNr) {
		super.gdOpen(devNr);
		this.rjsGraphic.initPage(devNr, RjsGraphic.STATE_OPENED,
				getWidth(), getHeight(), getCanvasColor(), isActive() );
		this.rjsGraphic.setMode(0);
	}
	
	@Override
	public void gdNewPage() {
		this.rjsGraphic.initPage(getDeviceNumber(), RjsGraphic.STATE_PAGED,
				getWidth(), getHeight(), getCanvasColor(), isActive() );
	}
	
	@Override
	public boolean gdNewPageConfirm() {
		return this.rjsGraphic.newPageConfirm();
	}
	
	@Override
	public void gdClose() {
		super.gdClose();
		this.rjsGraphic.close();
	}
	
	@Override
	public void gdActivate() {
		super.gdActivate();
		this.rjsGraphic.activate();
	}
	
	@Override
	public void gdDeactivate() {
		super.gdDeactivate();
		this.rjsGraphic.deactivate();
	}
	
	@Override
	public void gdMode(final int mode) {
		this.rjsGraphic.setMode(mode);
	}
	
	@Override
	public double[] gdSize() {
		final double[] result = this.rjsGraphic.newPageSize();
		if (result != null && result.length == 2) {
			setSize(result[0], result[1], PX);
		}
		return new double[] { 0.0, getWidth() - 1, getHeight() - 1, 0.0 };
	}
	
	@Override
	public void setSize(final double width, final double height, final int unit) {
		super.setSize(width, height, unit);
		if (this.rjsGraphic.getState() == RjsGraphic.STATE_OPENED) {
			gdMode(1);
			this.rjsGraphic.initPage(getDeviceNumber(), RjsGraphic.STATE_OPENED,
					getWidth(), getHeight(), getCanvasColor(), isActive() );
			gdMode(0);
		}
	}
	
	
	@Override
	public void gdClip(final double x0, final double x1, final double y0, final double y1) {
		this.rjsGraphic.setClip(x0, x1, y0, y1);
	}
	
	@Override
	public void gdcSetColor(final int cc) {
		this.rjsGraphic.setColor(cc);
	}
	
	@Override
	public void gdcSetFill(final int cc) {
		this.rjsGraphic.setFill(cc);
	}
	
	@Override
	public void gdcSetLine(final double lwd, final int lty) {
		this.rjsGraphic.setLine(lty, lwd);
	}
	
	@Override
	public double[] gdMetricInfo(final int ch) {
		final double[] result = this.rjsGraphic.getMetricInfo(ch);
//		System.out.println("MetricInfo: " + Arrays.toString(result) + " " + (char) ch + " (" + ch + ")");
		return (result != null && result.length == 3) ? result :
				new double[] { 0.0, 0.0, 8.0 };
	}
	
	@Override
	public double gdStrWidth(final String str) {
		final double[] result = this.rjsGraphic.getStrWidth(str);
//		if (str.length() == 1) {
//			System.out.println("StrWidth: " + Arrays.toString(result) + " \"" + str + "\" (" + str.codePointAt(0) + ")");
//		}
//		else {
//			System.out.println("StrWidth: " + Arrays.toString(result) + " \"" + str + "\"");
//		}
		return (result != null && result.length == 1) ? result[0] :
				(8 * str.length()); // rough estimate
	}
	
	@Override
	public void gdcSetFont(final double cex, final double ps, final double lineheight, final int fontface, final String fontfamily) {
//		System.out.println("Font: " + fontface + " \"" + fontfamily + "\"");
		this.rjsGraphic.setFont(cex * ps, lineheight, fontface, fontfamily);
	}
	
	
	@Override
	public void gdLine(final double x0, final double y0, final double x1, final double y1) {
//		System.out.println("Line: (" + x0 + ", " + y0 + "), (" + x1 + ", " + y1 + ")");
		this.rjsGraphic.drawLine(x0, y0, x1, y1);
	}
	
	@Override
	public void gdRect(final double x0, final double y0, final double x1, final double y1) {
//		System.out.println("Rect: (" + x0 + ", " + y0 + "), (" + x1 + ", " + y1 + ")");
		this.rjsGraphic.drawRect(x0, y0, x1, y1);
	}
	
	@Override
	public void gdPolyline(final int n, final double[] x, final double[] y) {
//		System.out.println("Polyline: " + n);
		this.rjsGraphic.drawPolyline(x, y);
	}
	
	@Override
	public void gdPolygon(final int n, final double[] x, final double[] y) {
//		System.out.println("Polygon: " + n);
		this.rjsGraphic.drawPolygon(x, y);
	}
	
	@Override
	public void gdPath(final int nPoly, final int[] nPer, final double[] x, final double[] y, final int mode) {
//		System.out.println("Path: " + Arrays.toString(nPer));
		this.rjsGraphic.drawPath(nPer, x, y, mode);
	}
	
	@Override
	public void gdCircle(final double x, final double y, final double r) {
//		System.out.println("Circle: (" + x + ", " + y + "), " +r);
		this.rjsGraphic.drawCircle(x, y, r);
	}
	
	@Override
	public void gdText(final double x, final double y, final String str, final double rot, final double hadj) {
//		System.out.println("Text: " + str);
		this.rjsGraphic.drawText(str, x, y, rot, hadj);
	}
	
	@Override
	public void gdRaster(final byte[] img, final boolean imgAlpha, final int img_w, final int img_h,
			final double x, final double y, final double w, final double h,
			final double rot, final boolean interpolate) {
		this.rjsGraphic.drawRaster(img, imgAlpha, img_w, img_h, x, y, w, h, rot, interpolate);
	}
	
	@Override
	public byte[] gdCap(final int[] dim) {
		{	final int iWidth = (int) (getWidth() + 0.5);
			if (dim[0] < 0 || dim[0] == iWidth) {
				dim[0] = iWidth;
				dim[1] = (int) (getHeight() + 0.5);
			}
			else {
				dim[1] = (int) ((dim[0]/getWidth()) * getHeight() + 0.5);
			}
		}
		{	final byte[] array = this.rjsGraphic.capture(dim);
			return (array != null && array.length == dim[0] * dim[1] * 4) ? array : null;
		}
	}
	
	
	@Override
	public double[] gdLocator() {
		final double[] xy = this.rjsGraphic.execLocator();
		return (xy != null && xy.length == 2) ? xy : null;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("JavaGD RJ-GD");
		if (getDeviceNumber() > 0) {
			sb.append(" Nr ").append(getDeviceNumber());
		}
		else {
			sb.append(" (not yet open)");
		}
		return sb.toString();
	}
	
}
