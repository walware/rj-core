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

package de.walware.rj.server.client;

import de.walware.rj.graphic.RCircle;
import de.walware.rj.graphic.RClipSetting;
import de.walware.rj.graphic.RColorSetting;
import de.walware.rj.graphic.RFillSetting;
import de.walware.rj.graphic.RFontSetting;
import de.walware.rj.graphic.RLine;
import de.walware.rj.graphic.RLineSetting;
import de.walware.rj.graphic.RPolygon;
import de.walware.rj.graphic.RPolyline;
import de.walware.rj.graphic.RRect;
import de.walware.rj.graphic.RText;


/**
 * Interface for the R side of (RJ) graphics at client layer.
 * 
 * The methods are called by the client protocol handler
 * (e.g. {@link AbstractRJComClient}) and must not be called at
 * another place.
 */
public interface RClientGraphic {
	
	/**
	 * Returns the device id of R
	 * 
	 * @return the id
	 */
	int getDevId();
	
	/**
	 * 
	 * @param w the width of the graphic
	 * @param h the height of the graphic
	 */
	void reset(double w, double h);
	
	/**
	 * Sets if the graphic is the active device in R.
	 * 
	 * @param active <code>true</code> if active, otherwise <code>false</code>
	 */
	void setActive(boolean active);
	
	/**
	 * Returns if the graphic is the active device in R.
	 * 
	 * @return <code>true</code> if active, otherwise <code>false</code>
	 */
	boolean isActive();
	
	/**
	 * Sets the current mode signaled by R.
	 * <p>
	 * Values:
	 *     0 = R stopped drawing
	 *     1 = R started drawing
	 *     2 = graphical input exists
	 * 
	 * @param mode the value constant
	 */
	void setMode(int mode);
	
	double[] computeSize();
	double[] computeFontMetric(int ch);
	double[] computeStringWidth(String txt);
	
	/**
	 * @see RClipSetting#RClipSetting(double, double, double, double)
	 */
	void addSetClip(double x0, double y0, double x1, double y1);
	/**
	 * @see RColorSetting#RColorSetting(int)
	 */
	void addSetColor(int color);
	/**
	 * @see RFillSetting#RFillSetting(int)
	 */
	void addSetFill(int color);
	/**
	 * @see RLineSetting#RLineSetting(int, double)
	 */
	void addSetLine(int type, double width);
	/**
	 * @see RFontSetting#RFontSetting(String, int, double, double, double)
	 */
	void addSetFont(String family, int face, double pointSize, double cex, double lineHeight);
	/**
	 * @see RLine#RLine(double, double, double, double)
	 */
	void addDrawLine(final double x0, final double y0, final double x1, final double y1);
	/**
	 * @see RRect#RRect(double, double, double, double)
	 */
	void addDrawRect(final double x0, final double y0, final double x1, final double y1);
	/**
	 * @see RPolyline#RPolyline(double[], double[])
	 */
	void addDrawPolyline(final double[] x, final double[] y);
	/**
	 * @see RPolygon#RPolygon(double[], double[])
	 */
	void addDrawPolygon(final double[] x, final double[] y);
	/**
	 * @see RCircle#RCircle(double, double, double)
	 */
	void addDrawCircle(final double x, final double y, final double r);
	/**
	 * @see RText#RText(double, double, double, double, String)
	 */
	void addDrawText(final double x, final double y, final double hAdj, final double rDeg, final String text);
	
}
