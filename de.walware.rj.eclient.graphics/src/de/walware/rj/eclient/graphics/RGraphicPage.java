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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.ui.actions.HandlerContributionItem;

import de.walware.rj.eclient.internal.graphics.RGraphicsPlugin;


/**
 * Single graphic page for {@link PageBookRGraphicView}.
 */
public class RGraphicPage extends Page {
	
	
	private class ResizeFitRHandler extends AbstractHandler {
		
		
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			final double[] size = fControl.getGraphicFitSize();
			final IStatus status = fGraphic.resize(size[0], size[1]);
			if (status == null || !status.isOK()) {
				// TODO: Status message
				Display.getCurrent().beep();
			}
			return null;
		}
		
	}
	
	
	private final IERGraphic fGraphic;
	
	private RGraphicComposite fControl;
	
	
	public RGraphicPage(final IERGraphic graphic) {
		fGraphic = graphic;
	}
	
	
	protected IERGraphic getGraphic() {
		return fGraphic;
	}
	
	@Override
	public void createControl(final Composite parent) {
		fControl = new RGraphicComposite(parent, fGraphic);
		
		initActions(getSite(), getSite().getActionBars());
	}
	
	protected void initActions(final IServiceLocator serviceLocator,final IActionBars actionBars) {
		final IHandlerService handlerService = (IHandlerService) serviceLocator.getService(IHandlerService.class);
		
		final IHandler2 refreshHandler = new AbstractHandler() {
			public Object execute(final ExecutionEvent event) throws ExecutionException {
				fControl.redrawGraphic();
				return null;
			}
		};
		handlerService.activateHandler(IWorkbenchCommandConstants.FILE_REFRESH, refreshHandler);
		
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		final IMenuManager menu = actionBars.getMenuManager();
		
		toolBar.add(new Separator());
		toolBar.add(new HandlerContributionItem(new CommandContributionItemParameter(
				getSite(), null, HandlerContributionItem.NO_COMMAND_ID, null,
				RGraphicsPlugin.getDefault().getImageRegistry().getDescriptor(RGraphicsPlugin.IMG_LOCTOOL_RESIZE_FIT_R), null, null,
			"Resize Fit in R", null, null, HandlerContributionItem.STYLE_PUSH, null, false),
			new ResizeFitRHandler()));
	}
	
	@Override
	public Control getControl() {
		return fControl;
	}
	
	@Override
	public void setFocus() {
		fControl.setFocus();
	}
	
}
