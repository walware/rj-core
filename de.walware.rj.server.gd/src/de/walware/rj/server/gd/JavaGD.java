/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.RJ;
import de.walware.rj.server.RjsGraphic;


public class JavaGD extends GDInterface implements RjsGraphic {
	
	
	private final RJ rj;
	private byte rjSlot;
	
	private double w;
	private double h;
	
	
	public JavaGD() {
		this.rj = RJ.get();
	}
	
	
	public void setSlot(final byte slot) {
		this.rjSlot = slot;
	}
	
	public byte getSlot() {
		return this.rjSlot;
	}
	
	public int getDevId() {
		return getDeviceNumber();
	}
	
	@Override
	public void gdOpen(final int devNr, final double w, final double h) {
		super.gdOpen(devNr, w, h);
		this.w = w;
		this.h = h;
		
		this.rj.registerGraphic(this);
		this.rj.execGDCommand(new GDCmdItem.CInit(
				getDeviceNumber(), this.w, this.h, isActive(), this.rjSlot ));
	}
	
	public void deferredInit(final int devId, final String target) {
	}
	
	@Override
	public void gdNewPage() {
		this.rj.execGDCommand(new GDCmdItem.CInit(
				getDeviceNumber(), this.w, this.h, isActive(), this.rjSlot ));
	}
	
	@Override
	public void gdClose() {
		this.rj.unregisterGraphic(this);
		super.gdClose();
//		System.out.println(hashCode());
		this.rj.execGDCommand(new GDCmdItem.CCloseDevice(
				getDeviceNumber(), this.rjSlot ));
	}
	
	@Override
	public void gdActivate() {
		super.gdActivate();
		if (isOpen()) {
			this.rj.execGDCommand(new GDCmdItem.CSetActiveOn(
					getDeviceNumber(), this.rjSlot ));
		}
	}
	
	@Override
	public void gdDeactivate() {
		super.gdDeactivate();
		if (isOpen()) {
			this.rj.execGDCommand(new GDCmdItem.CSetActiveOff(
					getDeviceNumber(), this.rjSlot ));
		}
	}
	
	@Override
	public void gdMode(final int mode) {
		if (isOpen()) {
			this.rj.execGDCommand(new GDCmdItem.CSetMode(
					getDeviceNumber(), mode, this.rjSlot ));
		}
	}
	
	@Override
	public double[] gdSize() {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetSize(
				getDeviceNumber(), this.rjSlot ));
		if (result != null && result.length == 2) {
			this.w = result[0];
			this.h = result[1];
			return new double[] { 0.0, result[0], result[1], 0.0 };
		}
		else {
			return new double[] { 0.0, 0.0, 0.0, 0.0 };
		}
	}
	
	
	@Override
	public void gdClip(final double x0, final double x1, final double y0, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.SetClip(
				getDeviceNumber(), x0, y0, x1, y1, this.rjSlot ));
	}
	
	@Override
	public void gdcSetColor(final int cc) {
		this.rj.execGDCommand(new GDCmdItem.SetColor(
				getDeviceNumber(), cc, this.rjSlot ));
	}
	
	@Override
	public void gdcSetFill(final int cc) {
		this.rj.execGDCommand(new GDCmdItem.SetFill(
				getDeviceNumber(), cc, this.rjSlot ));
	}
	
	@Override
	public void gdcSetLine(final double lwd, final int lty) {
		this.rj.execGDCommand(new GDCmdItem.SetLine(
				getDeviceNumber(), lty, lwd, this.rjSlot ));
	}
	
	@Override
	public double[] gdMetricInfo(final int ch) {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetFontMetric(
				getDeviceNumber(), ch, this.rjSlot ));
		if (result != null && result.length == 3) {
			return result;
		}
		else {
			return new double[] { 0.0, 0.0, 8.0 };
		}
	}
	
	@Override
	public double gdStrWidth(final String str) {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetStrWidth(
				getDeviceNumber(), str, this.rjSlot ));
		if (result != null && result.length == 1) {
			return result[0];
		}
		else {
			return (8 * str.length()); // rough estimate
		}
	}
	
	@Override
	public void gdcSetFont(final double cex, final double ps, final double lineheight, final int fontface, final String fontfamily) {
		this.rj.execGDCommand(new GDCmdItem.SetFont(
				getDeviceNumber(), fontfamily, fontface, ps, cex, lineheight, this.rjSlot ));
	}
	
	
	@Override
	public void gdLine(final double x0, final double y0, final double x1, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.DrawLine(
				getDeviceNumber(), x0, y0, x1, y1, this.rjSlot ));
	}
	
	@Override
	public void gdPolyline(final int n, final double[] x, final double[] y) {
		this.rj.execGDCommand(new GDCmdItem.DrawPolyline(
				getDeviceNumber(), x, y, this.rjSlot ));
	}
	
	@Override
	public void gdRect(final double x0, final double y0, final double x1, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.DrawRect(
				getDeviceNumber(), x0, y0, x1, y1, this.rjSlot ));
	}
	
	@Override
	public void gdCircle(final double x, final double y, final double r) {
		this.rj.execGDCommand(new GDCmdItem.DrawCircle(
				getDeviceNumber(), x, y, r, this.rjSlot ));
	}
	
	@Override
	public void gdPolygon(final int n, final double[] x, final double[] y) {
		this.rj.execGDCommand(new GDCmdItem.DrawPolygon(
				getDeviceNumber(), x, y, this.rjSlot ));
	}
	
	@Override
	public void gdText(final double x, final double y, final String str, final double rot, final double hadj) {
		this.rj.execGDCommand(new GDCmdItem.DrawText(
				getDeviceNumber(), x, y, hadj, rot, str, this.rjSlot ));
	}
	
	
	@Override
	public double[] gdLocator() {
		return null; // TODO
	}
	
}
