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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.walware.rj.graphic.RGraphic;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.ts.ITool;

import de.walware.rj.eclient.graphics.utils.CopyToDevRunnable;


/**
 * R graphic for Eclipse based clients.
 * 
 */
public interface IERGraphic extends RGraphic {
	
	
	interface ListenerInstructionsExtension extends Listener {
		
		void instructionsChanged(boolean reset, List<IERGraphicInstruction> added);
		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	int getDevId();
	
	/**
	 * Returns the current label for this graphic
	 * <p>
	 * The label can change if the graphic (device) is activated.
	 * </p>
	 * 
	 * @return the current label
	 */
	String getLabel();
	
	/**
	 * {@inheritDoc}
	 */
	boolean isActive();
	
	ITool getRHandle();
	
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
	 */
	IStatus getMessage();
	
	/**
	 * Registers a new message listener for this graphic.
	 * 
	 * @param listener the listener to add
	 */
	void addMessageListener(IStatusChangeListener listener);
	
	/**
	 * Removes a message listener registered for this graphic.
	 * 
	 * @param listener the listener to remove
	 */
	void removeMessageListener(IStatusChangeListener listener);
	
	
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
	 */
	void copy(String toDev, String toDevFile, String toDevArgs,
			IProgressMonitor monitor) throws CoreException;
	
}
