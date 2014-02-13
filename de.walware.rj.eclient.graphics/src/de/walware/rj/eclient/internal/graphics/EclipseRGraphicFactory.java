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

package de.walware.rj.eclient.internal.graphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import de.walware.ecommons.FastList;
import de.walware.ecommons.collections.ConstArrayList;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.rj.server.client.RClientGraphic;
import de.walware.rj.server.client.RClientGraphicActions;
import de.walware.rj.server.client.RClientGraphicFactory;

import de.walware.rj.eclient.graphics.IERGraphic;
import de.walware.rj.eclient.graphics.IERGraphicsManager;
import de.walware.rj.eclient.graphics.RGraphics;
import de.walware.rj.eclient.graphics.RGraphicsPreferencePage;
import de.walware.rj.eclient.graphics.comclient.IERClientGraphicActions;


/**
 * Factory and manager implementation for R graphics under Eclipse.
 * <p>
 * Public class is {@link de.walware.rj.eclient.graphics.comclient.ERGraphicFactory}.</p>
 */
public class EclipseRGraphicFactory implements RClientGraphicFactory, IERGraphicsManager {
	
	
	private static class AddedSafeRunnable implements ISafeRunnable {
		
		IERGraphic fGraphic;
		IERGraphicsManager.Listener fListener;
		
		@Override
		public void run() {
			fListener.graphicAdded(fGraphic);
		}
		
		@Override
		public void handleException(final Throwable exception) {
//			RGraphicsPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, RGraphicsPlugin.PLUGIN_ID,
//					ICommonStatusConstants.INTERNAL_PLUGGED_IN, "An error occurred when notifying.", exception));  //$NON-NLS-1$
		}
		
	}
	
	private static class RemovedSafeRunnable implements ISafeRunnable {
		
		IERGraphic fGraphic;
		IERGraphicsManager.Listener fListener;
		
		@Override
		public void run() {
			fListener.graphicRemoved(fGraphic);
		}
		
		@Override
		public void handleException(final Throwable exception) {
		}
		
	}
	
	private static class ShowSafeRunnable implements ISafeRunnable {
		
		IERGraphic fGraphic;
		IERGraphicsManager.ListenerShowExtension fListener;
		
		int fBestPriority;
		IERGraphicsManager.ListenerShowExtension fBestListener;
		
		@Override
		public void run() {
			if (fListener != null) {
				final int priority = fListener.canShowGraphic(fGraphic);
				if (priority > fBestPriority) {
					fBestPriority = priority;
					fBestListener = fListener;
				}
			}
			else {
				fBestListener.showGraphic(fGraphic);
			}
		}
		
		@Override
		public void handleException(final Throwable exception) {
//			RGraphicsPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, RGraphicsPlugin.PLUGIN_ID,
//					ICommonStatusConstants.INTERNAL_PLUGGED_IN, "An error occurred when notifying.", exception));  //$NON-NLS-1$
		}
		
	}
	
	
	private final FastList<EclipseRGraphic> fGraphics = new FastList<EclipseRGraphic>(EclipseRGraphic.class, FastList.IDENTITY);
	
	private final FastList<IERGraphicsManager.Listener> fListeners = new FastList<IERGraphicsManager.Listener>(IERGraphicsManager.Listener.class, FastList.IDENTITY);
	private final AddedSafeRunnable fAddedRunnable = new AddedSafeRunnable();
	private final RemovedSafeRunnable fRemovedRunnable = new RemovedSafeRunnable();
	private final ShowSafeRunnable fShowRunnable = new ShowSafeRunnable();
	
	private final Display fDefaultDisplay;
	private final FontManager fFontManager;
	private final ColorManager fColorManager;
	
	
	public EclipseRGraphicFactory() {
		fDefaultDisplay = UIAccess.getDisplay();
		fFontManager = new FontManager(fDefaultDisplay);
		fColorManager = new ColorManager(fDefaultDisplay);
		fDefaultDisplay.asyncExec(new Runnable() {
			@Override
			public void run() {
				fDefaultDisplay.disposeExec(new Runnable() {
					@Override
					public void run() {
						fFontManager.dispose();
						fColorManager.dispose();
					}
				});
			}
		});
	}
	
	
	@Override
	public Map<String, ? extends Object> getInitServerProperties() {
		final Map<String, Object> map = new HashMap<String, Object>();
		final IPreferencesService preferences = Platform.getPreferencesService();
		final AtomicReference<double[]> dpi = new AtomicReference<double[]>();
		dpi.set(RGraphicsPreferencePage.parseDPI(preferences.getString(
				RGraphics.PREF_DISPLAY_QUALIFIER, RGraphics.PREF_DISPLAY_CUSTOM_DPI_KEY, null, null )));
		if (dpi.get() == null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					final Point point = Display.getCurrent().getDPI();
					dpi.set(new double[] { point.x, point.y });
				}
			});
			if (dpi.get() == null) {
				dpi.set(new double[] { 96.0, 96.0 });
			}
		}
		map.put("display.ppi", dpi.get()); //$NON-NLS-1$
		return map;
	}
	
	@Override
	public RClientGraphic newGraphic(final int devId, final double w, final double h,
			final RClientGraphic.InitConfig config,
			final boolean active, final RClientGraphicActions actions, final int options) {
		final EclipseRGraphic egraphic = new EclipseRGraphic(devId, w, h, config, active,
				(actions instanceof IERClientGraphicActions) ? (IERClientGraphicActions) actions : null,
				options, this );
		if ((options & MANAGED_OFF) == 0) {
			fDefaultDisplay.syncExec(new Runnable() {
				@Override
				public void run() {
					fGraphics.add(egraphic);
					final Listener[] listeners = fListeners.toArray();
					
					fShowRunnable.fGraphic = egraphic;
					fShowRunnable.fBestPriority = Integer.MIN_VALUE;
					for (final Listener listener : listeners) {
						if (listener instanceof ListenerShowExtension) {
							fShowRunnable.fListener = (ListenerShowExtension) listener;
							SafeRunner.run(fShowRunnable);
						}
					}
					fShowRunnable.fListener = null;
					if (fShowRunnable.fBestPriority >= 0) {
						SafeRunner.run(fShowRunnable);
					}
					fShowRunnable.fBestListener = null;
					fShowRunnable.fGraphic = null;
					
					fAddedRunnable.fGraphic = egraphic;
					for (final Listener listener : listeners) {
						fAddedRunnable.fListener = listener;
						SafeRunner.run(fAddedRunnable);
					}
					fAddedRunnable.fListener = null;
					fAddedRunnable.fGraphic = null;
				}
			});
		}
		return egraphic;
	}
	
	@Override
	public void closeGraphic(final RClientGraphic graphic) {
		final EclipseRGraphic egraphic = (EclipseRGraphic) graphic;
		close(egraphic);
		egraphic.closeFromR();
	}
	
	void close(final EclipseRGraphic graphic) {
		if (!fDefaultDisplay.isDisposed()) {
			fDefaultDisplay.syncExec(new Runnable() {
				@Override
				public void run() {
					fGraphics.remove(graphic);
					final Listener[] listeners = fListeners.toArray();
					
					fRemovedRunnable.fGraphic = graphic;
					for (final Listener listener : listeners) {
						fRemovedRunnable.fListener = listener;
						SafeRunner.run(fRemovedRunnable);
					}
					fRemovedRunnable.fListener = null;
					fRemovedRunnable.fGraphic = null;
				}
			});
		}
	}
	
	
	public FontManager getFontManager(final Display display) {
		if (display == fDefaultDisplay) {
			return fFontManager;
		}
		return null;
	}
	
	public ColorManager getColorManager(final Display display) {
		if (display == fDefaultDisplay) {
			return fColorManager;
		}
		return null;
	}
	
	
	@Override
	public List<? extends IERGraphic> getAllGraphics() {
		return new ConstArrayList<EclipseRGraphic>(fGraphics.toArray());
	}
	
	@Override
	public void addListener(final Listener listener) {
		fListeners.add(listener);
	}
	
	@Override
	public void removeListener(final Listener listener) {
		fListeners.remove(listener);
	}
	
}
