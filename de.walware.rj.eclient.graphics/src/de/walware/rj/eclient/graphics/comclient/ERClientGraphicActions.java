/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics.comclient;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.walware.rj.server.client.AbstractRJComClient;
import de.walware.rj.server.client.AbstractRJComClientGraphicActions;

import de.walware.ecommons.collections.IntArrayMap;
import de.walware.ecommons.ts.ISystemRunnable;
import de.walware.ecommons.ts.ITool;

import de.walware.rj.eclient.AbstractRToolRunnable;
import de.walware.rj.eclient.IRToolService;


public class ERClientGraphicActions extends AbstractRJComClientGraphicActions
		implements IERClientGraphicActions {
	
	
	private final ITool fTool;
	
	private final IntArrayMap<Boolean> fResizeTasks = new IntArrayMap<Boolean>();
	private final IntArrayMap<Boolean> fCloseTasks = new IntArrayMap<Boolean>();
	
	
	public ERClientGraphicActions(final AbstractRJComClient rjs, final ITool tool) {
		super(rjs);
		fTool = tool;
	}
	
	
	private class ResizeRunnable extends AbstractRToolRunnable implements ISystemRunnable {
		
		private final int fDevId;
		private final Runnable fBeforeResize;
		
		public ResizeRunnable(final int devId, final Runnable beforeResize) {
			super("r/rj/gd/resize", "Resize R Graphic"); //$NON-NLS-1$
			fDevId = devId;
			fBeforeResize = beforeResize;
		}
		
		@Override
		public boolean changed(final int event, final ITool process) {
			switch (event) {
			case MOVING_FROM:
			case REMOVING_FROM:
				return false;
			case BEING_ABANDONED:
				synchronized (fCloseTasks) {
					fResizeTasks.put(fDevId, Boolean.FALSE);
				}
				return true;
			default:
				return true;
			}
		}
		
		@Override
		public void run(final IRToolService r,
				final IProgressMonitor monitor) throws CoreException {
			synchronized (fCloseTasks) {
				fResizeTasks.put(fDevId, Boolean.FALSE);
			}
			fBeforeResize.run();
			doResizeGraphic(fDevId, monitor);
		}
		
	}
	
	private class CloseRunnable extends AbstractRToolRunnable implements ISystemRunnable {
		
		
		private final int fDevId;
		
		public CloseRunnable(final int devId) {
			super("r/rj/gd/close", "Close R Graphic ("+(devId+1)+")"); //$NON-NLS-1$
			fDevId = devId;
		}
		
		@Override
		public boolean changed(final int event, final ITool tool) {
			switch (event) {
			case MOVING_FROM:
				return false;
			case REMOVING_FROM:
			case BEING_ABANDONED:
			case FINISHING_OK:
			case FINISHING_ERROR:
			case FINISHING_CANCEL:
				synchronized (fCloseTasks) {
					fCloseTasks.put(fDevId, Boolean.FALSE);
				}
				return true;
			default:
				return true;
			}
		}
		
		@Override
		public void run(final IRToolService r,
				final IProgressMonitor monitor) throws CoreException {
			doCloseGraphic(fDevId, monitor);
		}
		
	}
	
	
	public ITool getRHandle() {
		return fTool;
	}
	
	public String getRLabel() {
		return fTool.getLabel(ITool.DEFAULT_LABEL);
	}
	
	public IStatus resizeGraphic(final int devId, final Runnable beforeResize) {
		synchronized (fResizeTasks) {
			if (fResizeTasks.put(devId, Boolean.TRUE) == Boolean.TRUE) {
				return Status.OK_STATUS;
			}
		}
		return fTool.getQueue().add(new ResizeRunnable(devId, beforeResize));
	}
	
	public IStatus closeGraphic(final int devId) {
		synchronized (fCloseTasks) {
			if (fCloseTasks.put(devId, Boolean.TRUE) == Boolean.TRUE) {
				return Status.OK_STATUS;
			}
		}
		return fTool.getQueue().add(new CloseRunnable(devId));
	}
	
}
