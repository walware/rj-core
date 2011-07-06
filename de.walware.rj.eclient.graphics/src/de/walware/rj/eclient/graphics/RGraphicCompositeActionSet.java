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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerContributionItem;

import de.walware.rj.eclient.internal.graphics.RGraphicsPlugin;


/**
 * Actions for R graphics in an {@link RGraphicComposite}.
 */
public class RGraphicCompositeActionSet implements IERGraphic.Listener {
	
	
	public static final String CONTEXT_MENU_GROUP_ID = "context"; //$NON-NLS-1$
	public static final String SIZE_MENU_GROUP_ID = "size"; //$NON-NLS-1$
	
	
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
	
	
	private final List<IActionBars> fActionBars = new ArrayList<IActionBars>(4);
	
	private IERGraphic fGraphic;
	private final RGraphicComposite fGraphicComposite;
	private final Display fDisplay;
	
	private final IHandler2 fResizeFitRHandler;
	
	
	public RGraphicCompositeActionSet(final RGraphicComposite composite) {
		fGraphicComposite = composite;
		fDisplay = fGraphicComposite.getDisplay();
		
		fResizeFitRHandler = new ResizeFitRHandler();
	}
	
	
	public void setGraphic(final IERGraphic graphic) {
		if (fGraphic != null) {
			fGraphic.removeListener(this);
		}
		fGraphic = graphic;
		if (fGraphic != null) {
			fGraphic.addListener(this);
		}
		
		update();
	}
	
	public void initActions(final IServiceLocator serviceLocator) {
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
	}
	
	protected void addSizeActions(final IServiceLocator serviceLocator, final IActionBars actionBars) {
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		
		final ImageRegistry rGraphicsImageRegistry = RGraphicsPlugin.getDefault().getImageRegistry();
		
		toolBar.appendToGroup(SIZE_MENU_GROUP_ID, new HandlerContributionItem(new CommandContributionItemParameter(
				serviceLocator, null, HandlerContributionItem.NO_COMMAND_ID, null,
				rGraphicsImageRegistry.getDescriptor(RGraphicsPlugin.IMG_LOCTOOL_RESIZE_FIT_R), null, null,
				"Resize Fit in R", null, null, HandlerContributionItem.STYLE_PUSH, null, false ),
				fResizeFitRHandler ));
		
		update();
	}
	
	protected void update() {
		if (fActionBars.isEmpty()) {
			return;
		}
		
		for (final IActionBars actionBars : fActionBars) {
			actionBars.getToolBarManager().update(true);
		}
	}
	
	
	public void activated() {
	}
	
	public void deactivated() {
	}
	
	public void drawingStarted() {
	}
	
	public void drawingStopped() {
	}
	
	
	public void dispose(final IActionBars actionBars) {
		fActionBars.remove(actionBars);
	}
	
	public void dispose() {
		setGraphic(null);
	}
	
}
