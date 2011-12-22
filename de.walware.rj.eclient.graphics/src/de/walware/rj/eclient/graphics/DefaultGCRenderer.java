/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Transform;

import de.walware.rj.graphic.RGraphicInstruction;

import de.walware.rj.eclient.internal.graphics.CircleElement;
import de.walware.rj.eclient.internal.graphics.ClipSetting;
import de.walware.rj.eclient.internal.graphics.ColorSetting;
import de.walware.rj.eclient.internal.graphics.FillSetting;
import de.walware.rj.eclient.internal.graphics.FontSetting;
import de.walware.rj.eclient.internal.graphics.GraphicInitialization;
import de.walware.rj.eclient.internal.graphics.LineElement;
import de.walware.rj.eclient.internal.graphics.LineSetting;
import de.walware.rj.eclient.internal.graphics.PolygonElement;
import de.walware.rj.eclient.internal.graphics.PolylineElement;
import de.walware.rj.eclient.internal.graphics.RectElement;
import de.walware.rj.eclient.internal.graphics.TextElement;


public class DefaultGCRenderer {
	
	
	private float fScale = 1.0f;
	
	private final LineAttributes fLineAttributes = new LineAttributes(1.0f);
	private Color fLineColor;
	private int fLineAlpha;
	private Color fFillColor;
	private int fFillAlpha;
	private final double[] fFontProperties = new double[1];
	private int fXMax;
	private int fYMax;
	
	
	public void clear(final float scale) {
		fScale = scale;
		fLineColor = null;
		fLineAlpha = 0xff;
		fFillColor = null;
		fFillAlpha = 0xff;
		fLineAttributes.style = SWT.LINE_SOLID;
		fLineAttributes.width = scale;
		fXMax = 0;
		fYMax = 0;
	}
	
	public void paint(final GC gc, final List<? extends IERGraphicInstruction> instructions) {
		final Transform defaultTransform = null;
		final Transform tempTransform = new Transform(gc.getDevice());
		final float scale = fScale;
		int currentAlpha = -1;
		Color lineColor = fLineColor;
		int lineAlpha = fLineAlpha;
		Color fillColor = fFillColor;
		int fillAlpha = fFillAlpha;
		
		try {
			gc.setAdvanced(true);
			gc.setAntialias(SWT.ON);
			gc.setTextAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.setLineAttributes(fLineAttributes);
			gc.setTransform(defaultTransform);
			gc.setAlpha(currentAlpha);
			if (fLineColor != null) {
				gc.setForeground(fLineColor);
			}
			if (fFillColor != null) {
				gc.setBackground(fFillColor);
			}
			int ixmax = fXMax;
			int iymax = fYMax;
			
			for (final IERGraphicInstruction instr : instructions) {
				switch (instr.getInstructionType()) {
				case RGraphicInstruction.INIT:
					final GraphicInitialization init = (GraphicInitialization) instr;
					ixmax = (int) (init.width * scale + 0.5);
					iymax = (int) (init.height * scale + 0.5);
					gc.setBackground(fillColor = gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
					gc.setForeground(lineColor = gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
					gc.setAlpha(0xff);
					gc.fillRectangle(0, 0, ixmax, iymax);
					gc.setClipping(0, 0, ixmax, iymax);
					continue;
				case RGraphicInstruction.SET_CLIP: {
					final ClipSetting setting = (ClipSetting) instr;
					final int ix0 = (int) (setting.x0 * scale + 1.5);
					final int iy0 = (int) (setting.y0 * scale + 1.5);
					gc.setClipping(ix0, iy0,
							(int) Math.min((setting.x1 * scale + 0.5), ixmax) - ix0,
							(int) Math.min((setting.y1 * scale + 0.5), iymax) - iy0 );
					continue; }
				case RGraphicInstruction.SET_COLOR: {
					final ColorSetting setting = (ColorSetting) instr;
					lineAlpha = setting.getAlpha();
					gc.setForeground(lineColor = setting.swtColor);
					continue; }
				case RGraphicInstruction.SET_FILL: {
					final FillSetting setting = (FillSetting) instr;
					fillAlpha = setting.getAlpha();
					gc.setBackground(fillColor = setting.swtColor);
					continue; }
				case RGraphicInstruction.SET_LINE: {
					final LineSetting setting = (LineSetting) instr;
					switch (setting.type) {
					case 0:
						fLineAttributes.style = SWT.LINE_SOLID;
						fLineAttributes.width = (float) (setting.width * scale);
						gc.setLineAttributes(fLineAttributes);
						continue;
					case -1:
						fLineAttributes.style = SWT.LINE_SOLID;
						fLineAttributes.width = 0.0f;
						gc.setLineAttributes(fLineAttributes);
						continue;
	//				case 0x44:
	//					fTempLineAttributes.style = SWT.LINE_DASH;
	//					fTempLineAttributes.width = (float) (setting.width * scale);
	//					gc.setLineAttributes(fTempLineAttributes);
	//					continue;
	//				case 0x13:
	//					fTempLineAttributes.style = SWT.LINE_DOT;
	//					fTempLineAttributes.width = (float) (setting.width * scale);
	//					gc.setLineAttributes(fTempLineAttributes);
	//					continue;
	//				case 0x1343:
	//					fTempLineAttributes.style = SWT.LINE_DASHDOT;
	//					fTempLineAttributes.width = (float) (setting.width * scale);
	//					gc.setLineAttributes(fTempLineAttributes);
	//					continue;
					}
					
					int rPattern = setting.type;
					int length = 0;
					while (rPattern != 0) {
						length++;
						rPattern >>>= 4;
					}
					final int[] dashes = new int[length];
					rPattern = setting.type;
					for (int i = 0; i < length; i++) {
						dashes[i] = (rPattern & 0xf);
						rPattern >>>= 4;
					}
					gc.setLineDash(dashes);
					gc.setLineWidth((int) (setting.width * scale + 0.5));
					continue; }
				case RGraphicInstruction.SET_FONT: {
					final FontSetting setting = (FontSetting) instr;
					gc.setFont(setting.swtFont);
					fFontProperties[0] = setting.swtProperties[0];
					continue; }
				case RGraphicInstruction.DRAW_LINE: {
					final LineElement element = (LineElement) instr;
					if (lineAlpha != currentAlpha) {
						gc.setAlpha(currentAlpha = lineAlpha);
					}
					gc.drawLine(
							(int) (element.x0 * scale + 0.5),
							(int) (element.y0 * scale + 0.5),
							(int) (element.x1 * scale + 0.5),
							(int) (element.y1 * scale + 0.5) );
					continue; }
				case RGraphicInstruction.DRAW_RECTANGLE: {
					final RectElement element = (RectElement) instr;
					final int ix0 = (int) (element.x0 * scale + 0.5);
					final int iy0 = (int) (element.y0 * scale + 0.5);
					final int iw = (int) (element.x1 * scale + 0.5) - ix0;
					final int ih = (int) (element.y1 * scale + 0.5) - iy0;
					if (iw == 0 || ih == 0) {
						if (lineAlpha != 0) {
							if (lineAlpha != currentAlpha) {
								gc.setAlpha(currentAlpha = lineAlpha);
							}
							gc.drawLine(ix0, iy0, ix0+iw, iy0+ih);
							continue;
						}
						continue;
					}
					if (fillAlpha != 0) {
						if (fillAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = fillAlpha);
						}
						gc.fillRectangle(ix0+1, iy0+1, iw-1, ih-1);
					}
					if (lineAlpha != 0) {
						if (lineAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = lineAlpha);
						}
						gc.drawRectangle(ix0, iy0, iw, ih);
					}
					continue; }
				case RGraphicInstruction.DRAW_POLYLINE: {
					final PolylineElement element = (PolylineElement) instr;
					final int n = element.x.length;
					final int[] icoord = new int[n * 2];
					for (int i = 0, j = 0; j < n; j++) {
						icoord[i++] = (int) (element.x[j] * scale + 0.5);
						icoord[i++] = (int) (element.y[j] * scale + 0.5);
					}
					gc.drawPolyline(icoord);
					continue; }
				case RGraphicInstruction.DRAW_POLYGON: {
					final PolygonElement element = (PolygonElement) instr;
					final int n = element.x.length;
					final int[] icoord = new int[n * 2];
					for (int i = 0, j = 0; j < n; j++) {
						icoord[i++] = (int) (element.x[j] * scale + 0.5);
						icoord[i++] = (int) (element.y[j] * scale + 0.5);
					}
					if (fillAlpha != 0) {
						if (fillAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = fillAlpha);
						}
						gc.fillPolygon(icoord);
					}
					if (lineAlpha != 0) {
						if (lineAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = lineAlpha);
						}
						gc.drawPolygon(icoord);
					}
					continue; }
				case RGraphicInstruction.DRAW_CIRCLE: {
					final CircleElement element = (CircleElement) instr;
					final int id = (int) (element.r * 2.0 + 0.5);
					tempTransform.setElements(1, 0, 0, 1,
							(float) (element.x - id / 2.0),
							(float) (element.y - id / 2.0) );
					gc.setTransform(tempTransform);
					if (fillAlpha != 0) {
						if (fillAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = fillAlpha);
						}
						gc.fillOval(1, 1, id-1, id-1);
					}
					if (lineAlpha != 0) {
						if (lineAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = lineAlpha);
						}
						gc.drawOval(0, 0, id, id);
					}
					gc.setTransform(defaultTransform);
					continue; }
				case RGraphicInstruction.DRAW_TEXT: {
					final TextElement element = (TextElement) instr;
					final double hShift;
					if (element.horizontalAdjust != 0.0) {
						hShift = element.horizontalAdjust * element.swtStrWidth;
					}
					else {
						hShift = 0.0;
					}
					if (element.rotateDegree != 0.0) {
						tempTransform.setElements(1, 0, 0, 1,
								(float) element.x * scale,
								(float) element.y * scale );
						tempTransform.rotate((float) -element.rotateDegree);
						tempTransform.translate(
								(float) Math.floor(1.1111 - hShift),
								(float) Math.floor(0.0511 - fFontProperties[0]) );
						gc.setTransform(tempTransform);
						if (lineAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = lineAlpha);
						}
						gc.drawString(element.text, 0, 0, true);
						gc.setTransform(defaultTransform);
						
						continue;
					}
					else {
						if (lineAlpha != currentAlpha) {
							gc.setAlpha(currentAlpha = lineAlpha);
						}
						gc.drawString(element.text,
								(int) Math.floor(1.1111 + element.x - hShift),
								(int) Math.floor(0.6111 + element.y - fFontProperties[0]),
								true );
						
						continue;
					} }
				}
			}
			
			fLineColor = lineColor;
			fLineAlpha = lineAlpha;
			fFillColor = fillColor;
			fFillAlpha = fillAlpha;
			fXMax = ixmax;
			fYMax = iymax;
		}
		finally {
			tempTransform.dispose();
		}
	}
	
}
