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

package de.walware.rj.gd;

import java.util.Arrays;

import org.rosuda.javaGD.GDInterface;

import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.RJ;
import de.walware.rj.server.RjsGraphic;


public class JavaGD extends GDInterface implements RjsGraphic {
	
	
	private final RJ rj;
	private byte rjSlot;
	
	private int devId = -1;
	
	private double w;
	private double h;
	
	
	public JavaGD() {
		this.rj = RJ.get();
	}
	
	
	public void setSlot(final byte slot) {
		rjSlot = slot;
	}
	
	public byte getSlot() {
		return rjSlot;
	}
	
	public int getDevId() {
		return devId;
	}
	
	@Override
	public int getDeviceNumber() {
		return this.devId;
	}
	
	@Override
	public void gdOpen(final double w, final double h) {
		if (this.open) {
			gdClose();
		}
		super.gdOpen(w, h);
		this.w = w;
		this.h = h;
		
		this.rj.registerGraphic(this);
	}
	
	public void deferredInit(final int devId, final String target) {
		this.devId = devId;
		this.active = true;
		this.rj.execGDCommand(new GDCmdItem.CInit(this.devId, this.w, this.h, this.active, this.rjSlot));
	}
	
	
	@Override
	public void gdNewPage() {
	}
	
	@Override
	public void gdNewPage(final int devNr) {
		super.gdNewPage(devNr);
		this.devId = devNr;
		this.rj.execGDCommand(new GDCmdItem.CInit(this.devId, this.w, this.h, this.active, this.rjSlot));
	}
	
	@Override
	public void gdClose() {
		this.rj.unregisterGraphic(this);
		super.gdClose();
//		System.out.println(hashCode());
		this.rj.execGDCommand(new GDCmdItem.CCloseDevice(this.devId, this.rjSlot));
	}
	
	@Override
	public void gdActivate() {
		if (this.active) {
			return;
		}
		this.active = true;
		if (this.devId >= 0) {
			this.rj.execGDCommand(new GDCmdItem.CSetActiveOn(this.devId, this.rjSlot));
		}
	}
	
	@Override
	public void gdDeactivate() {
		if (!this.active) {
			return;
		}
		this.active = false;
		if (this.devId >= 0) {
			this.rj.execGDCommand(new GDCmdItem.CSetActiveOff(this.devId, this.rjSlot));
		}
	}
	
	@Override
	public void gdMode(final int mode) {
		this.rj.execGDCommand(new GDCmdItem.CSetMode(this.devId, mode, this.rjSlot));
	}
	
	@Override
	public double[] gdSize() {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetSize(this.devId, this.rjSlot));
		if (result != null && result.length == 2) {
			w = result[0];
			h = result[1];
			return new double[] { 0.0, result[0], result[1], 0.0 };
		}
		else {
			return new double[] { 0.0, 0.0, 0.0, 0.0 };
		}
	}
	
	
	@Override
	public void gdClip(final double x0, final double x1, final double y0, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.SetClip(this.devId, x0, y0, x1, y1, this.rjSlot));
	}
	
	@Override
	public void gdcSetColor(final int cc) {
		this.rj.execGDCommand(new GDCmdItem.SetColor(this.devId, cc, this.rjSlot));
	}
	
	@Override
	public void gdcSetFill(final int cc) {
		this.rj.execGDCommand(new GDCmdItem.SetFill(this.devId, cc, this.rjSlot));
	}
	
	@Override
	public void gdcSetLine(final double lwd, final int lty) {
		this.rj.execGDCommand(new GDCmdItem.SetLine(this.devId, lty, lwd, this.rjSlot));
	}
	
	@Override
	public double[] gdMetricInfo(final int ch) {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetFontMetric(this.devId, ch, this.rjSlot));
		if (result != null && result.length == 3) {
			System.out.println("metric: " + Arrays.toString(result));
			return result;
		}
		else {
			return new double[] { 0.0, 0.0, 8.0 };
		}
	}
	
	@Override
	public double gdStrWidth(final String str) {
		final double[] result = this.rj.execGDCommand(new GDCmdItem.CGetStrWidth(devId, str, this.rjSlot));
		if (result != null && result.length == 1) {
			return result[0];
		}
		else {
			return (8 * str.length()); // rough estimate
		}
	}
	
	@Override
	public void gdcSetFont(final double cex, final double ps, final double lineheight, final int fontface, final String fontfamily) {
		this.rj.execGDCommand(new GDCmdItem.SetFont(this.devId, fontfamily, fontface, ps, cex, lineheight, this.rjSlot));
	}
	
	
	@Override
	public void gdLine(final double x0, final double y0, final double x1, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.DrawLine(this.devId, x0, y0, x1, y1, this.rjSlot));
	}
	
	@Override
	public void gdPolyline(final int n, final double[] x, final double[] y) {
		this.rj.execGDCommand(new GDCmdItem.DrawPolyline(this.devId, x, y, this.rjSlot));
	}
	
	@Override
	public void gdRect(final double x0, final double y0, final double x1, final double y1) {
		this.rj.execGDCommand(new GDCmdItem.DrawRect(this.devId, x0, y0, x1, y1, this.rjSlot));
	}
	
	@Override
	public void gdCircle(final double x, final double y, final double r) {
		this.rj.execGDCommand(new GDCmdItem.DrawCircle(this.devId, x, y, r, this.rjSlot));
	}
	
	@Override
	public void gdPolygon(final int n, final double[] x, final double[] y) {
		this.rj.execGDCommand(new GDCmdItem.DrawPolygon(this.devId, x, y, this.rjSlot));
	}
	
	@Override
	public void gdText(final double x, final double y, final String str, final double rot, final double hadj) {
		this.rj.execGDCommand(new GDCmdItem.DrawText(this.devId, x, y, hadj, rot, str, this.rjSlot));
	}
	
	
	@Override
	public void gdHold() {
	}
	
	@Override
	public double[] gdLocator() {
		return null; // TODO
	}
	
	@Override
	public void executeDevOff() {
		// not supported at this layer
	}
	
}
