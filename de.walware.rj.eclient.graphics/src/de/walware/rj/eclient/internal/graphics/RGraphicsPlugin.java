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

package de.walware.rj.eclient.internal.graphics;

import static de.walware.ecommons.ui.util.ImageRegistryUtil.T_LOCTOOL;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;
import de.walware.ecommons.ui.util.ImageRegistryUtil;

import de.walware.rj.eclient.graphics.RGraphics;


/**
 * Plug-in <code>de.walware.rj.eclient.graphics</code>.
 */
public class RGraphicsPlugin extends AbstractUIPlugin {
	
	
	public static final String IMG_LOCTOOL_RESIZE_FIT_R = RGraphics.PLUGIN_ID + "/image/loctool/resize-fit-r"; //$NON-NLS-1$
	
	public static final String IMG_LOCTOOL_LOCATOR_DONE = RGraphics.PLUGIN_ID + "/image/loctool/locator-done"; //$NON-NLS-1$
	public static final String IMG_LOCTOOL_LOCATOR_CANCEL = RGraphics.PLUGIN_ID + "/image/loctool/locator-cancel"; //$NON-NLS-1$
	
	
	/** The shared instance */
	private static RGraphicsPlugin gPlugin;
	
	/**
	 * Returns the shared plug-in instance
	 *
	 * @return the shared instance
	 */
	public static RGraphicsPlugin getDefault() {
		return gPlugin;
	}
	
	
	private boolean fStarted;
	
	private final List<IDisposable> fDisposables = new ArrayList<IDisposable>();
	
	
	public RGraphicsPlugin() {
	}
	
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		gPlugin = this;
		fStarted = true;
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			synchronized (this) {
				fStarted = false;
			}
			
			for (final IDisposable listener : fDisposables) {
				try {
					listener.dispose();
				}
				catch (final Throwable e) {
					getLog().log(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN, "Error occured when dispose module", e)); 
				}
			}
			fDisposables.clear();
		}
		finally {
			gPlugin = null;
			super.stop(context);
		}
	}
	
	
	@Override
	protected void initializeImageRegistry(final ImageRegistry reg) {
		if (!fStarted) {
			throw new IllegalStateException("Plug-in is not started.");
		}
		final ImageRegistryUtil util = new ImageRegistryUtil(this);
		
		util.register(IMG_LOCTOOL_RESIZE_FIT_R, T_LOCTOOL, "resize-fit-r.png");
		
		util.register(IMG_LOCTOOL_LOCATOR_DONE, T_LOCTOOL, "locator-done.png");
		util.register(IMG_LOCTOOL_LOCATOR_CANCEL, T_LOCTOOL, "locator-cancel.png");
	}
	
	public void registerPluginDisposable(final IDisposable listener) {
		if (listener == null) {
			throw new NullPointerException();
		}
		synchronized (this) {
			if (!fStarted) {
				throw new IllegalStateException("Plug-in is not started.");
			}
			fDisposables.add(listener);
		}
	}
	
}
