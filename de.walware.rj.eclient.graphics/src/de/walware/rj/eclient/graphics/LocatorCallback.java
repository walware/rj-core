/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.graphics;

import java.util.Collection;

import de.walware.ecommons.collections.ConstArrayList;


/**
 * Callback for a local locator.
 * <p>
 * When using the locator function of {@link IERGraphic}, the locator callback configures the 
 * request and receives the answer.  When starting a local locator using {@link IERGraphic#startLocalLocator(LocatorCallback)}
 * the callback must be specified as argument.  The callback configures the locator request
 * and receives the answer(s).</p>
 * <p>
 * The methods {@link #getMessage()} and {@link #getStopTypes()} allows to configure the behavior
 * of the locator.  The properties can be used for example in the GUI.  The values can change
 * for each point to locate; the properties are checked at beginning and again after 
 * {@link #located(double, double)} was called and request one more point.</p>
 * <p>
 * The callback is notified by points located by the user are by calling ({@link LocatorCallback#located(double, double)}).
 * Note that the callback receives graphic coordinates.  The method can return {@link #NEXT}
 * to request a next point or {@link #STOP} to stop the locator.  The locator is also stopped
 * if the method {@link LocatorCallback#stopped(String)} was called.</p>
 * <p>
 * Stop types allow to specify a special user action when stopping the locator.  Stop types known 
 * by the default implementation are {@link IERGraphic#LOCATOR_DONE} and {@link IERGraphic#LOCATOR_CANCEL}
 * representing the usual OK and Cancel action.  The {@link RGraphicCompositeActionSet} provides
 * buttons for both actions.</p>
 * 
 * @since 1.0
 */
public abstract class LocatorCallback {
	
	
	protected static final Collection<String> DEFAULT_STOP_TYPES = new ConstArrayList<String>(IERGraphic.LOCATOR_DONE);
	
	/**
	 * Return code for {@link #located(double, double)} indicating to stop directly the locator
	 * ({@link #stopped(String)} is not called).
	 */
	public static final int STOP = 0;
	
	/**
	 * Return code for {@link #located(double, double)} indicating to restart/continue the locator
	 * requesting the next point.
	 */
	public static final int NEXT = 1;
	
	
	public LocatorCallback() {
	}
	
	
	/**
	 * Returns the current message for the locator.
	 * 
	 * @return the current message
	 */
	public String getMessage() {
		return "→ Locate a point by mouse click";
	}
	
	/**
	 * Returns the currently supported stop types for the locator.
	 * 
	 * @return a collection of stop types, an empty collection if no regular stop is supported
	 * @see #stopped(String)
	 */
	public Collection<String> getStopTypes() {
		return DEFAULT_STOP_TYPES;
	}
	
	/**
	 * Is called if the user selected a value.
	 * <p>
	 * The method is called by the graphic. Other class must use {@link IERGraphic#returnLocator(double, double)}
	 * to specify a located point.
	 * It is not guaranteed that the method is called in a special thread.</p>
	 * 
	 * @param x value of the graphic coordinate
	 * @param y value of the graphic coordinate
	 * @return a known return code
	 * @see #STOP
	 * @see #NEXT
	 */
	public abstract int located(double x, double y);
	
	/**
	 * Is called if the locator was stopped by the user or graphic manager.
	 * <p>
	 * The stop type is one of the {@link #getStopTypes() supported stop types} of the locator or 
	 * also <code>null</code>.  <code>null</code> as stop type is always allowed and is used for 
	 * example if the graphic was closed.</p>
	 * <p>
	 * The method is called by the graphic. Other class must use {@link IERGraphic#stopLocator(String)}
	 * to stop the locator.
	 * It is not guaranteed that the method is called in a special thread.</p>
	 * 
	 * @param type the stop type
	 */
	public abstract void stopped(String type);
	
}
