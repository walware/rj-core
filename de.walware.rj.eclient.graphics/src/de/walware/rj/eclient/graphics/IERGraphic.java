/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.ts.ITool;

import de.walware.rj.graphic.RGraphic;

import de.walware.rj.eclient.graphics.utils.CopyToDevRunnable;


/**
 * R graphic for Eclipse based clients.
 * 
 * <h4>Coordinate systems:</h4>
 * <p>
 * The <b>graphic coordinate</b> system is used to draw the graphic, the point of origin is the upper
 * left corner, values increasing to the right and downward respectively.</p>
 * <p>
 * The <b>user coordinate</b> system is defined by the main x-axis and y-axis of the plot using
 * the original units of the plotted values, if possible.<p>
 * 
 */
public interface IERGraphic extends RGraphic {
	
	/**
	 * @see IERGraphic#addListener(Listener)
	 * @see IERGraphic#removeListener(Listener)
	 * @since 1.0
	 */
	interface ListenerLocatorExtension extends Listener {
		
		/**
		 * Is called when a locator is started or restarted (next point).
		 * <p>
		 * The method is called in the display thread.</p>
		 */
		void locatorStarted();
		
		/**
		 * Is called when a locator is stopped.
		 * <p>
		 * The method is called in the display thread.</p>
		 */
		void locatorStopped();
		
	}
	
	
	/**
	 * The default locator stop type indicating that the user does not want to select more points
	 * (OK, in R also called stop, default action for right mouse click).
	 * 
	 * @since 1.0
	 */
	String LOCATOR_DONE = "done"; //$NON-NLS-1$
	
	/**
	 * Locator stop type indicating that the user wants to cancel the locator.
	 * 
	 * @since 1.0
	 */
	String LOCATOR_CANCEL = "cancel"; //$NON-NLS-1$
	
	/**
	 * @since 1.0
	 */
	interface ListenerInstructionsExtension extends Listener {
		
		void instructionsChanged(boolean reset, List<IERGraphicInstruction> added);
		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	int getDevId();
	
	/**
	 * Returns the current label for this graphic
	 * <p>
	 * The label can change if the graphic (device) is activated or a locator is started or stopped.
	 * </p>
	 * 
	 * @return the current label
	 */
	String getLabel();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	boolean isActive();
	
	ITool getRHandle();
	
	@Override
	List<? extends IERGraphicInstruction> getInstructions();
	
	IStatus resize(double w, double h);
	
	IStatus close();
	
	/**
	 * Returns the current message for the graphic.
	 * <p>
	 * The standard OK message means there is no message.  If the message changes messages listeners
	 * registered for this graphic ({@link #addMessageListener(IStatusChangeListener)}, 
	 * {@link #removeMessageListener(IStatusChangeListener)}) are notified.</p>
	 * <p>
	 * The message I can be shown for example in the status line of the application.</p>
	 * 
	 * @return the current message to show
	 * @since 1.0
	 */
	IStatus getMessage();
	
	/**
	 * Registers a new message listener for this graphic.
	 * 
	 * @param listener the listener to add
	 * @since 1.0
	 */
	void addMessageListener(IStatusChangeListener listener);
	
	/**
	 * Removes a message listener registered for this graphic.
	 * 
	 * @param listener the listener to remove
	 * @since 1.0
	 */
	void removeMessageListener(IStatusChangeListener listener);
	
	
	/**
	 * Starts a local locator requesting the user to select a point in the graphic.
	 * <p>
	 * The method starts the locator and returns directly; it does not wait for an answer.
	 * If a locator is already installed for this graphic (local or from R), the method does 
	 * nothing and returns an error status.</p>
	 * <p>
	 * The locator callback configures the request and receives the answer(s).  The answer can be
	 * a coordinate located by the user ({@link LocatorCallback#located(double, double)}) or 
	 * a stop command ({@link LocatorCallback#stopped(String)}.</p>
	 * <p>
	 * Graphic listeners implementing {@link ListenerLocatorExtension} and registered for this
	 * graphic ({@link #addListener(Listener)}, {@link #removeListener(Listener)}) are notified 
	 * if the locator is started and if it is finally stopped.</p>
	 * 
	 * @param callback the callback called to handover the answer(s)
	 * @return An OK status if the locator is started for the given callback, otherwise 
	 *     an error status
	 * @see LocatorCallback
	 * @since 1.0
	 */
	IStatus startLocalLocator(LocatorCallback callback);
	
	/**
	 * Returns true if any locator (local or from R) is started for this graphic.
	 * 
	 * @return <code>true</code> if a locator is started, otherwise <code>false</code>
	 * @since 1.0
	 */
	boolean isLocatorStarted();
	
	/**
	 * Returns a collection of currently supported stop types.
	 * <p>
	 * If no locator is started, the method returns an empty collection.</p>
	 * 
	 * @return a list of supported stop types
	 * @see LocatorCallback#getStopTypes()
	 * @since 1.0
	 */
	Collection<String> getLocatorStopTypes();
	
	/**
	 * Answers a locator request with the specified coordinates.
	 * <p>
	 * If no locator is started, the method does nothing.</p>
	 * 
	 * @param x the x value of the graphic coordinate
	 * @param y the y value of the graphic coordinate
	 * @since 1.0
	 */
	void returnLocator(double x, double y);
	
	/**
	 * Answers a locator request with the specified stop command.
	 * <p>
	 * If no locator is started or does not support the specified type, the method does nothing.</p>
	 * 
	 * @param type the stop type or <code>null</code>
	 * @see LocatorCallback#getStopTypes()
	 * @since 1.0
	 */
	void stopLocator(String type);
	
	
	/**
	 * @deprecated {@link #copy(String, String, String, IProgressMonitor)} or
	 *     {@link CopyToDevRunnable}
	 */
	@Deprecated
	IStatus copy(String toDev, String toDevFile, String toDevArgs) throws CoreException;
	
	/**
	 * Copies the graphic to another R graphic device.
	 * <p>
	 * This is an R based function.  The caller must have exclusive access to the R service.  The 
	 * graphic must be available in R.</p>
	 * 
	 * @param toDev the name of the target device
	 * @param toDevFile the name of the file for file based devices
	 * @param toDevArgs other R arguments for the target arguments
	 * @param monitor
	 * @throws CoreException
	 * @since 1.0
	 */
	void copy(String toDev, String toDevFile, String toDevArgs,
			IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Converts a coordinate (x, y) from graphic to user coordinate system.
	 * <p>
	 * This is an R based function.  The caller must have exclusive access to the R service.  The 
	 * graphic must be available in R.</p>
	 * 
	 * @param xy the graphic coordinate to convert
	 * @param monitor
	 * @return the converted user coordinate
	 * @throws CoreException
	 * @see IERGraphic coordinate systems
	 * @since 1.0
	 */
	double[] convertGraphic2User(double[] xy,
			IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Converts a coordinate (x, y) from user to graphic coordinate system.
	 * <p>
	 * This is an R based function.  The caller must have exclusive access to the R service.  The 
	 * graphic must be available in R.</p>
	 * 
	 * @param xy the graphic coordinate to convert
	 * @param monitor
	 * @return the converted user coordinate
	 * @throws CoreException
	 * @see IERGraphic coordinate systems
	 * @since 1.0
	 */
	double[] convertUser2Graphic(double[] xy,
			IProgressMonitor monitor) throws CoreException;
	
}
