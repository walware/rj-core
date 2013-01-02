/*******************************************************************************
 * Copyright (c) 2011-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.collections.ConstList;
import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ts.IToolRunnable;
import de.walware.ecommons.ts.IToolService;

import de.walware.rj.eclient.graphics.IERGraphic;
import de.walware.rj.eclient.graphics.LocatorCallback;


/**
 * Basic implementation for a local locator for R graphics.
 * <p>
 * Requests the user to locate point and converts the graphic coordinates to user coordinates.
 * It uses the tool service API (de.walware.ecommons.ts) to schedule the R job.</p>
 */
public abstract class AbstractLocalLocator extends LocatorCallback implements IToolRunnable {
	
	protected static final Collection<String> OK_CANCEL_STOP_TYPES = new ConstList<String>(
			IERGraphic.LOCATOR_DONE, IERGraphic.LOCATOR_CANCEL);
	
	
	private final IERGraphic fGraphic;
	private final ITool fTool;
	
	private boolean fStarted;
	private boolean fGraphicLocatorStarted;
	private boolean fRunnableScheduled;
	
	private final List<double[]> fToConvert = new ArrayList<double[]>();
	
	private final List<double[]> fLocatedGraphic = new ArrayList<double[]>();
	private final List<double[]> fLocatedUser = new ArrayList<double[]>();
	
	private volatile int fCounter;
	
	
	public AbstractLocalLocator(final IERGraphic graphic) {
		fGraphic = graphic;
		fTool = graphic.getRHandle();
	}
	
	
	@Override
	public Collection<String> getStopTypes() {
		return OK_CANCEL_STOP_TYPES;
	}
	
	@Override
	public String getMessage() {
		return super.getMessage() + " (" + fCounter + " selected)";
	}
	
	@Override
	public int located(final double x, final double y) {
		synchronized (this) {
			fCounter++;
			fToConvert.add(new double[] { x, y });
			if (!internalScheduleConversion()) {
				internalStop(null);
				return STOP;
			}
		}
		return NEXT;
	}
	
	@Override
	public void stopped(final String type) {
		synchronized (this) {
			fGraphicLocatorStarted = false;
			if (type == IERGraphic.LOCATOR_DONE) {
				if (fToConvert.isEmpty()) {
					internalStop(IERGraphic.LOCATOR_DONE);
					return;
				}
				else if (internalScheduleConversion()) {
					return;
				}
				// scheduling failed
			}
			internalStop(null);
			return;
		}
	}
	
	public String getTypeId() {
		return "r/rjgd/locallocator"; //$NON-NLS-1$
	}
	
	public String getLabel() {
		return "Resolve Graphic Points";
	}
	
	public boolean isRunnableIn(final ITool tool) {
		return (tool == fTool);
	}
	
	public boolean changed(final int event, final ITool tool) {
		switch (event) {
		case REMOVING_FROM:
			return false;
		case BEING_ABANDONED:
		case FINISHING_CANCEL:
		case FINISHING_ERROR:
			synchronized (this) {
				fRunnableScheduled = false;
				if (!fGraphicLocatorStarted) {
					internalStop(null);
					break;
				}
			}
			fGraphic.stopLocator(null);
			break;
		case FINISHING_OK:
			synchronized (this) {
				if (!fGraphicLocatorStarted && fToConvert.isEmpty()) {
					internalStop(IERGraphic.LOCATOR_DONE);
					break;
				}
			}
		}
		return true;
	}
	
	public void run(final IToolService service,
			final IProgressMonitor monitor) throws CoreException {
		double[] graphic = null;
		double[] user = null;
		while (true) {
			synchronized (this) {
				if (graphic != null) {
					if (user != null) {
						fLocatedGraphic.add(graphic);
						fLocatedUser.add(user);
					}
					else {
						// invalid point?
					}
				}
				if (fToConvert.isEmpty()) {
					fCounter = fLocatedGraphic.size();
					fRunnableScheduled = false;
					return;
				}
				graphic = fToConvert.remove(0);
				user = null;
			}
			user = fGraphic.convertGraphic2User(graphic, monitor);
		}
	}
	
	/**
	 * synchronized
	 */
	protected boolean internalScheduleConversion() {
		if (!fRunnableScheduled) {
			if (fTool.getQueue().addHot(this).isOK()) {
				fRunnableScheduled = true;
				return true;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * synchronized
	 * 
	 * @param type the stop type
	 */
	protected void internalStop(final String type) {
		assert (!fGraphicLocatorStarted);
		if (fRunnableScheduled) {
			fTool.getQueue().removeHot(this);
			fRunnableScheduled = false;
		}
		fStarted = false;
		if (type == IERGraphic.LOCATOR_DONE) {
			finished(new ArrayList<double[]>(fLocatedGraphic), new ArrayList<double[]>(fLocatedUser));
		}
		else {
			canceled();
		}
	}
	
	
	public boolean start() {
		synchronized (this) {
			if (fStarted) {
				return false;
			}
			fToConvert.clear();
			fLocatedGraphic.clear();
			fLocatedUser.clear();
			fStarted = fGraphicLocatorStarted = true;
			fCounter = 0;
			if (fGraphic.startLocalLocator(this).isOK()) {
				return true;
			}
			else {
				fStarted = fGraphicLocatorStarted = false;
				return false;
			}
		}
	}
	
	/**
	 * Is called if the locator and conversion is finished.
	 * 
	 * @param graphic the graphic coordinates
	 * @param user the user coordinates
	 */
	protected abstract void finished(final List<double[]> graphic, final List<double[]> user);
	
	/**
	 * Is called if the locator was canceled
	 */
	protected abstract void canceled();
	
}
