//
//  GDInterface.java
//  Java Graphics Device
//
//  Created by Simon Urbanek on Thu Aug 05 2004.
//  Copyright (c) 2004-2009 Simon Urbanek. All rights reserved.
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation;
//  version 2.1 of the License.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//

package org.rosuda.javaGD;


/** <code>CGInterface</code> defines an interface (and provides a simple implementation) between the JavaGD R device and the Java code. Any back-end that desires to display R graphics in Java can subclass this class are provide its name to JavaGD package via JAVAGD_CLASS_NAME environment variable. The default implementation handles most callbacks, but subclasses should override at least {@link #gdOpen} to create an instance of {@link GDContainer} {@link #c} which will be used for all subsequent drawing.
 <p>
 <b>external API: those methods are called via JNI from the GD C code</b>
 <p>
 <pre>
 public void     gdOpen(int devNr, double w, double h);
 public void     gdActivate();
 public void     gdCircle(double x, double y, double r);
 public void     gdClip(double x0, double x1, double y0, double y1);
 public void     gdClose();
 public void     gdDeactivate();
 public double[] gdLocator();
 public void     gdLine(double x1, double y1, double x2, double y2);
 public double[] gdMetricInfo(int ch);
 public void     gdMode(int mode);
 public void     gdNewPage();
 public void     gdPolygon(int n, double[] x, double[] y);
 public void     gdPolyline(int n, double[] x, double[] y);
 public void     gdRect(double x0, double y0, double x1, double y1);
 public double[] gdSize();
 public double   gdStrWidth(String str);
 public void     gdText(double x, double y, String str, double rot, double hadj);
 </pre>
 <p>
 <b>GDC - manipulation of the current graphics state</b>
 <p>
 <pre>
 public void gdcSetColor(int cc);
 public void gdcSetFill(int cc);
 public void gdcSetLine(double lwd, int lty);
 public void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily);
 </pre>
*/
public abstract class GDInterface {
	
	/**
	 * Flag indicating whether this device is active (current) in R
	 **/
	private boolean active = false;
	
	/**
	 * Flag indicating whether this device has currently an open instance
	 **/
	private boolean open = false;
	
	/**
	 * The device number as supplied by R in {@link #gdOpen(int, double, double)}
	 * (-1 if undefined)
	 **/
	private int devNr = -1;
	
	
	/**
	 * Returns the device number
	 * 
	 * @return the device number or -1 is unknown
	 **/
	public final int getDeviceNumber() {
		return this.devNr;
	}
	
	public final boolean isActive() {
		return this.active;
	}
	
	public final boolean isOpen() {
		return this.open;
	}
	
	
	/** 
	 * Opens the new device with the specified size
	 * @param devNr device number
	 * @param w width of the device
	 * @param h height of the device */
	public void     gdOpen(int devNr, double w, double h) {
		if (isOpen()) {
			gdClose();
		}
		this.devNr = devNr;
		this.open = true;
	}
	
    /** the device became active (current) */ 
    public void     gdActivate() {
        this.active = true;
    }

    /** draw a circle
     *  @param x x coordinate of the center
     *  @param y y coordinate of the center
     *  @param r radius */
    public abstract void gdCircle(double x, double y, double r);

    /** clip drawing to the specified region
     *  @param x0 left coordinate
     *  @param x1 right coordinate
     *  @param y0 top coordinate
     *  @param y1 bottom coordinate */
    public abstract void gdClip(double x0, double x1, double y0, double y1);

    /** close the display */
    public void     gdClose() {
        this.open = false;
    }

    /** the device became inactive (i.e. another device is now current) */
    public void     gdDeactivate() {
        this.active = false;
    }

    /** invoke the locator
     *  @return array of indices or <code>null</code> is cancelled */
    public abstract double[] gdLocator();

    /** draw a line
     *  @param x1 x coordinate of the origin
     *  @param y1 y coordinate of the origin
     *  @param x2 x coordinate of the end
     *  @param y2 y coordinate of the end */
    public abstract void gdLine(double x1, double y1, double x2, double y2);

    /** retrieve font metrics info for the given unicode character
     *  @param ch character (encoding may depend on the font type)
     *  @return an array consisting for three doubles: ascent, descent and width */
    public abstract double[] gdMetricInfo(int ch);

    /** R signalled a mode change
     *  @param mode mode as signalled by R (currently 0=R stopped drawing, 1=R started drawing, 2=graphical input exists) */
    public abstract void gdMode(int mode);

    /** create a new, blank page 
     *  @param devNr device number assigned to this device by R */
    public void gdNewPage() {
    }

    public abstract void gdPolygon(int n, double[] x, double[] y);

    public abstract void gdPolyline(int n, double[] x, double[] y);

    public abstract void gdRect(double x0, double y0, double x1, double y1);

    /** retrieve the current size of the device
     *  @return an array of four doubles: 0, width, height, 0 */
    public abstract double[] gdSize();

    /** retrieve width of the given text when drawn in the current font
     *  @param str text
     *  @return width of the text */
    public abstract double gdStrWidth(String str);

    /** draw text
     *  @param x x coordinate of the origin
     *  @param y y coordinate of the origin
     *  @param str text to draw
     *  @param rot rotation (in degrees)
     *  @param hadj horizontal adjustment with respect to the text size (0=left-aligned wrt origin, 0.5=centered, 1=right-aligned wrt origin) */
    public abstract void gdText(double x, double y, String str, double rot, double hadj);

    /*-- GDC - manipulation of the current graphics state */
    /** set drawing color
     *  @param cc color */
    public abstract void gdcSetColor(int cc);

    /** set fill color
     *  @param cc color */
    public abstract void gdcSetFill(int cc);

    /** set line width and type
     *  @param lwd line width (see <code>lwd</code> parameter in R)
     *  @param lty line type (see <code>lty</code> parameter in R) */
    public abstract void gdcSetLine(double lwd, int lty);

    /** set current font
     *  @param cex character expansion (see <code>cex</code> parameter in R)
     *  @param ps point size (see <code>ps</code> parameter in R - for all practical purposes the requested font size in points is <code>cex * ps</code>)
     *  @param lineheight line height
     *  @param fontface font face (see <code>font</code> parameter in R: 1=plain, 2=bold, 3=italic, 4=bold-italic, 5=symbol)
     *  @param fontfamily font family (see <code>family</code> parameter in R) */
    public abstract void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily);
	
}
