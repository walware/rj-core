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
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.ui.SharedUIResources;


/**
 * Single graphic page for {@link PageBookRGraphicView}.
 */
public class RGraphicPage extends Page implements IStatusChangeListener {
	
	
	private final IERGraphic fGraphic;
	
	private RGraphicComposite fControl;
	
	private RGraphicCompositeActionSet fActions;
	
	private boolean fActivated;
	
	
	public RGraphicPage(final IERGraphic graphic) {
		fGraphic = graphic;
	}
	
	
	protected IERGraphic getGraphic() {
		return fGraphic;
	}
	
	@Override
	public void createControl(final Composite parent) {
		fControl = new RGraphicComposite(parent, fGraphic) {
			@Override
			public void setVisible(final boolean visible) {
				super.setVisible(visible);
				updateState();
			}
		};
		
		initActions(getSite(), getSite().getActionBars());
		
		fGraphic.addMessageListener(this);
		statusChanged(fGraphic.getMessage());
	}
	
	protected void initActions(final IServiceLocator serviceLocator, final IActionBars actionBars) {
		final IHandlerService handlerService = (IHandlerService) serviceLocator.getService(IHandlerService.class);
		
		final IHandler2 refreshHandler = new AbstractHandler() {
			public Object execute(final ExecutionEvent event) throws ExecutionException {
				fControl.redrawGraphic();
				return null;
			}
		};
		handlerService.activateHandler(IWorkbenchCommandConstants.FILE_REFRESH, refreshHandler);
		
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		toolBar.insertBefore(SharedUIResources.ADDITIONS_MENU_ID, new Separator(RGraphicCompositeActionSet.CONTEXT_MENU_GROUP_ID));
		toolBar.insertBefore(SharedUIResources.ADDITIONS_MENU_ID, new Separator(RGraphicCompositeActionSet.SIZE_MENU_GROUP_ID));
		
		fActions = createActionSet();
		fActions.setGraphic(fGraphic);
		fActions.initActions(serviceLocator);
		fActions.contributeToActionsBars(serviceLocator, actionBars);
		
		updateState();
	}
	
	protected RGraphicCompositeActionSet createActionSet() {
		return new RGraphicCompositeActionSet(fControl);
	}
	
	private void updateState() {
		if (fControl != null && !fControl.isDisposed() && fControl.isVisible()) {
			if (!fActivated) {
				fActivated = true;
			}
		}
		else {
			if (fActivated) {
				fActivated = false;
			}
		}
	}
	
	public void statusChanged(final IStatus status) {
		final String message;
		if (status.getSeverity() > 0) {
			message = status.getMessage();
		}
		else {
			message = null;
		}
		getSite().getActionBars().getStatusLineManager().setMessage(message);
	}
	
	@Override
	public Control getControl() {
		return fControl;
	}
	
	protected RGraphicComposite getGraphicComposite() {
		return fControl;
	}
	
	@Override
	public void setFocus() {
		fControl.setFocus();
	}
	
	@Override
	public void dispose() {
		if (fActions != null) {
			fActions.dispose();
			fActions = null;
		}
		if (fGraphic != null) {
			fGraphic.removeMessageListener(this);
		}
		super.dispose();
	}
	
}
