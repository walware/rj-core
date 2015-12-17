/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ts.ISystemReadRunnable;
import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerCollection;
import de.walware.ecommons.ui.actions.HandlerContributionItem;

import de.walware.rj.eclient.AbstractRToolRunnable;
import de.walware.rj.eclient.IRToolService;
import de.walware.rj.eclient.graphics.utils.AbstractLocalLocator;
import de.walware.rj.eclient.internal.graphics.RGraphicsPlugin;


/**
 * Actions for R graphics in an {@link RGraphicComposite}.
 */
public class RGraphicCompositeActionSet implements IERGraphic.ListenerLocatorExtension {
	
	
	public static final String POSITION_STATUSLINE_ITEM_ID = "position"; //$NON-NLS-1$
	
	public static final String CONTEXT_MENU_GROUP_ID = "context"; //$NON-NLS-1$
	public static final String SIZE_MENU_GROUP_ID = "size"; //$NON-NLS-1$
	
	private static final String LOCATOR_DONE_COMMAND_ID = ".locator.done"; //$NON-NLS-1$
	private static final String LOCATOR_CANCEL_COMMAND_ID = ".locator.cancel"; //$NON-NLS-1$
	private static final String RESIZE_FIT_COMMAND_ID = ".resize.fit"; //$NON-NLS-1$
	
	
	public static interface LocationListener {
		
		void loading();
		
		void located(double x, double y);
		
	}
	
	
	private class ResizeFitRHandler extends AbstractHandler {
		
		public ResizeFitRHandler() {
		}
		
		@Override
		public void setEnabled(final Object evaluationContext) {
			setBaseEnabled(fGraphic != null);
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			if (fGraphic == null) {
				return null;
			}
			final double[] size = fGraphicComposite.getGraphicFitSize();
			final IStatus status = fGraphic.resize(size[0], size[1]);
			if (status == null || !status.isOK()) {
				// TODO: Status message
				Display.getCurrent().beep();
			}
			return null;
		}
		
	}
	
	protected class StopLocatorHandler extends AbstractHandler {
		
		private final String fType;
		
		public StopLocatorHandler(final String type) {
			fType = type;
		}
		
		@Override
		public void setEnabled(final Object evaluationContext) {
			setBaseEnabled(fGraphic != null && fGraphic.isLocatorStarted()
					&& fGraphic.getLocatorStopTypes().contains(fType) );
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			if (fGraphic == null) {
				return null;
			}
			fGraphic.stopLocator(fType);
			return null;
		}
		
	}
	
	private static abstract class ConversionRunnable extends AbstractRToolRunnable implements ISystemReadRunnable {
		
		private final IERGraphic fGraphic;
		
		private boolean fScheduled;
		
		private double[] fTodoSource;
		
		private double[] fConvertedSource;
		private double[] fConvertedTarget;
		
		
		public ConversionRunnable(final IERGraphic graphic) {
			super("r/rjgd/position", "Converting graphic coordinates"); //$NON-NLS-1$
			fGraphic = graphic;
		}
		
		
		public boolean schedule(final double[] source) {
			synchronized (this) {
				fTodoSource = source;
				if (fScheduled) {
					return true;
				}
				final IStatus status = fGraphic.getRHandle().getQueue().addHot(this);
				if (status.isOK()) {
					fScheduled = true;
					return true;
				}
				return false;
			}
		}
		
		public void cancel() {
			synchronized (this) {
				if (fScheduled) {
					fScheduled = false;
					fTodoSource = null;
					fGraphic.getRHandle().getQueue().removeHot(this);
				}
			}
		}
		
		
		@Override
		public boolean isRunnableIn(final ITool tool) {
			return (tool == fGraphic.getRHandle() && super.isRunnableIn(tool));
		}
		
		@Override
		public boolean changed(final int event, final ITool tool) {
			switch (event) {
			case MOVING_FROM:
				return false;
			case REMOVING_FROM:
			case BEING_ABANDONED:
			case FINISHING_ERROR:
			case FINISHING_CANCEL:
				synchronized (this) {
					fScheduled = false;
					break;
				}
			case FINISHING_OK:
				converted(fGraphic, fConvertedSource, fConvertedTarget);
				break;
			}
			return true;
		}
		
		@Override
		protected void run(final IRToolService service,
				final IProgressMonitor monitor) throws CoreException {
			double[] source = null;
			double[] target = null;
			while (true) {
				synchronized (this) {
					fConvertedSource = source;
					fConvertedTarget = target;
					
					source = fTodoSource;
					if (source == null) {
						fScheduled = false;
						return;
					}
					fTodoSource = null;
				}
				target = fGraphic.convertGraphic2User(source, monitor);
			}
		}
		
		protected abstract void converted(IERGraphic graphic, double[] source, double[] target);
		
	}
	
	private class MouseLocationListener implements Listener {
		
		private double[] fCurrentGraphic;
		private double[] fCurrentTarget;
		
		
		@Override
		public void handleEvent(final Event event) {
			switch (event.type) {
			case SWT.MouseDown:
				if (event.button == 1) {
					final double[] request = fCurrentGraphic = new double[] {
							fGraphicComposite.convertWidget2GraphicX(event.x),
							fGraphicComposite.convertWidget2GraphicY(event.y) };
					fCurrentTarget = null;
					event.display.timerExec(1000, new Runnable() {
						@Override
						public void run() {
							if (fCurrentTarget == null && fCurrentGraphic == request
									&& !fGraphicComposite.isDisposed()) {
								notifyMouseLocationListeners(null);
							}
						}
					});
					if (fMouseLocationRunnable == null) {
						fMouseLocationRunnable = new ConversionRunnable(fGraphic) {
							@Override
							protected void converted(final IERGraphic graphic,
									final double[] source, final double[] target) {
								if (fGraphic == graphic) {
									fDisplay.asyncExec(new Runnable() {
										@Override
										public void run() {
											if (fGraphic == graphic
													&& source != null && target != null && fCurrentGraphic != null
													&& source[0] == fCurrentGraphic[0] && source[1] == fCurrentGraphic[1]) {
												fCurrentTarget = target;
												notifyMouseLocationListeners(target);
											}
										}
									});
								}
							}
						};
					}
					fMouseLocationRunnable.schedule(fCurrentGraphic);
				}
				break;
			}
		}
		
	}
	
	
	private final List<IActionBars> fActionBars = new ArrayList<IActionBars>(4);
	
	private IERGraphic fGraphic;
	private final RGraphicComposite fGraphicComposite;
	private final Display fDisplay;
	
	private HandlerCollection fHandlerCollection;
	
	private final FastList<LocationListener> fMouseLocationListeners = new FastList<LocationListener>(LocationListener.class);
	private MouseLocationListener fMouseListenerListener;
	private ConversionRunnable fMouseLocationRunnable;
	
	
	public RGraphicCompositeActionSet(final RGraphicComposite composite) {
		fGraphicComposite = composite;
		fDisplay = fGraphicComposite.getDisplay();
	}
	
	
	public void setGraphic(final IERGraphic graphic) {
		if (fGraphic != null) {
			fGraphic.removeListener(this);
			if (fMouseLocationRunnable != null) {
				fMouseLocationRunnable.cancel();
				fMouseLocationRunnable = null;
			}
		}
		fGraphic = graphic;
		if (fGraphic != null) {
			fGraphic.addListener(this);
		}
		
		update();
	}
	
	public void initActions(final IServiceLocator serviceLocator) {
		fHandlerCollection = new HandlerCollection();
		fHandlerCollection.add(LOCATOR_DONE_COMMAND_ID,
				new StopLocatorHandler(IERGraphic.LOCATOR_DONE) );
		fHandlerCollection.add(LOCATOR_CANCEL_COMMAND_ID,
				new StopLocatorHandler(IERGraphic.LOCATOR_CANCEL) );
		fHandlerCollection.add(RESIZE_FIT_COMMAND_ID,
				new ResizeFitRHandler() );
	}
	
	public void contributeToActionsBars(final IServiceLocator serviceLocator,
			final IActionBars actionBars) {
		fActionBars.add(actionBars);
		
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		if (toolBar.find(CONTEXT_MENU_GROUP_ID) == null) {
			toolBar.insertBefore(SharedUIResources.ADDITIONS_MENU_ID, new Separator(CONTEXT_MENU_GROUP_ID));
		}
		if (toolBar.find(SIZE_MENU_GROUP_ID) == null) {
			toolBar.insertBefore(SharedUIResources.ADDITIONS_MENU_ID, new Separator(SIZE_MENU_GROUP_ID));
		}
		
		final ImageRegistry rGraphicsImageRegistry = RGraphicsPlugin.getDefault().getImageRegistry();
		
		toolBar.appendToGroup(CONTEXT_MENU_GROUP_ID, new HandlerContributionItem(new CommandContributionItemParameter(
				serviceLocator, null, HandlerContributionItem.NO_COMMAND_ID, null,
				rGraphicsImageRegistry.getDescriptor(RGraphicsPlugin.IMG_LOCTOOL_LOCATOR_DONE), null, null,
				"Stop Locator", null, null, HandlerContributionItem.STYLE_PUSH, null, true),
				fHandlerCollection.get(LOCATOR_DONE_COMMAND_ID) ));
		toolBar.appendToGroup(CONTEXT_MENU_GROUP_ID, new HandlerContributionItem(new CommandContributionItemParameter(
				serviceLocator, null, HandlerContributionItem.NO_COMMAND_ID, null,
				rGraphicsImageRegistry.getDescriptor(RGraphicsPlugin.IMG_LOCTOOL_LOCATOR_CANCEL), null, null,
				"Cancel Locator", null, null, HandlerContributionItem.STYLE_PUSH, null, true),
				fHandlerCollection.get(LOCATOR_CANCEL_COMMAND_ID) ));
	}
	
	protected void addTestLocator(final IServiceLocator serviceLocator, final IActionBars actionBars) {
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		
		final IHandler2 handler = new AbstractHandler() {
			@Override
			public void setEnabled(final Object evaluationContext) {
				setBaseEnabled(fGraphic != null && !fGraphic.isLocatorStarted());
			}
			@Override
			public Object execute(final ExecutionEvent event) throws ExecutionException {
				if (fGraphic == null || fGraphic.isLocatorStarted()) {
					return null;
				}
				final AbstractLocalLocator locator = new AbstractLocalLocator(fGraphic) {
					@Override
					protected void finished(final List<double[]> graphic, final List<double[]> user) {
						final StringBuilder sb = new StringBuilder();
						for (int i = 0; i < user.size(); i++) {
							sb.append(Arrays.toString(user.get(i))).append("\n");
						}
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog.openInformation(null, "Locator Result",
										sb.toString());
							}
						});
					};
					@Override
					protected void canceled() {
					};
				};
				locator.start();
				return null;
			}
		};
		fHandlerCollection.add(".locator.startTest", handler);
		toolBar.appendToGroup(CONTEXT_MENU_GROUP_ID, new HandlerContributionItem(new CommandContributionItemParameter(
				serviceLocator, null, HandlerContributionItem.NO_COMMAND_ID, null,
				SharedUIResources.getImages().getDescriptor(SharedUIResources.LOCTOOL_SORT_SCORE_IMAGE_ID), null, null,
				"Test Locator", null, null, HandlerContributionItem.STYLE_PUSH, null, false),
				handler ));
	}
	
	protected void addSizeActions(final IServiceLocator serviceLocator, final IActionBars actionBars) {
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		
		final ImageRegistry rGraphicsImageRegistry = RGraphicsPlugin.getDefault().getImageRegistry();
		
		toolBar.appendToGroup(SIZE_MENU_GROUP_ID, new HandlerContributionItem(new CommandContributionItemParameter(
				serviceLocator, null, HandlerContributionItem.NO_COMMAND_ID, null,
				rGraphicsImageRegistry.getDescriptor(RGraphicsPlugin.IMG_LOCTOOL_RESIZE_FIT_R), null, null,
				"Resize Fit in R", null, null, HandlerContributionItem.STYLE_PUSH, null, false ),
				fHandlerCollection.get(RESIZE_FIT_COMMAND_ID) ));
		
		update();
	}
	
	protected void update() {
		if (fActionBars.isEmpty()) {
			return;
		}
		
		fHandlerCollection.update(null);
		
		for (final IActionBars actionBars : fActionBars) {
			actionBars.getToolBarManager().update(true);
		}
	}
	
	
	@Override
	public void activated() {
	}
	
	@Override
	public void deactivated() {
	}
	
	@Override
	public void drawingStarted() {
	}
	
	@Override
	public void drawingStopped() {
	}
	
	@Override
	public void locatorStarted() {
		update();
	}
	
	@Override
	public void locatorStopped() {
		update();
	}
	
	
	public void addMouseClickLocationListener(final LocationListener listener) {
		fMouseLocationListeners.add(listener);
		if (fMouseListenerListener == null) {
			fMouseListenerListener = new MouseLocationListener();
			final Control widget = fGraphicComposite.getGraphicWidget();
			widget.addListener(SWT.MouseDown, fMouseListenerListener);
		}
	}
	
	public void removeMouseLocationListener(final LocationListener listener) {
		fMouseLocationListeners.remove(listener);
	}
	
	private void notifyMouseLocationListeners(final double[] xy) {
		final LocationListener[] listeners = fMouseLocationListeners.toArray();
		if (xy != null) {
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].located(xy[0], xy[1]);
			}
		}
		else {
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].loading();
			}
		}
	}
	
	
	public void dispose(final IActionBars actionBars) {
		fActionBars.remove(actionBars);
	}
	
	public void dispose() {
		setGraphic(null);
	}
	
}
