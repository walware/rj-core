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

package de.walware.rj.eclient.graphics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.rj.eclient.graphics.RGraphicCompositeActionSet.LocationListener;


/**
 * Single graphic page for {@link PageBookRGraphicView}.
 */
public class RGraphicPage extends Page implements IStatusChangeListener {
	
	
	private final IERGraphic fGraphic;
	
	private RGraphicComposite fControl;
	
	private RGraphicCompositeActionSet fActions;
	
	private StatusLineContributionItem fLocationStatusItem;
	private String fLocationStatusText;
	private LocationListener fMouseLocationListener;
	
	
	public RGraphicPage(final IERGraphic graphic) {
		fGraphic = graphic;
	}
	
	
	protected IERGraphic getGraphic() {
		return fGraphic;
	}
	
	protected void init(final StatusLineContributionItem locationStatusItem) {
		fLocationStatusItem = locationStatusItem;
	}
	
	@Override
	public void createControl(final Composite parent) {
		fControl = new RGraphicComposite(parent, fGraphic);
		
		initActions(getSite(), getSite().getActionBars());
		
		fGraphic.addMessageListener(this);
		statusChanged(fGraphic.getMessage());
	}
	
	protected void initActions(final IServiceLocator serviceLocator, final IActionBars actionBars) {
		final IHandlerService handlerService = (IHandlerService) serviceLocator.getService(IHandlerService.class);
		
		final IHandler2 refreshHandler = new AbstractHandler() {
			@Override
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
		
		// Can find wrong item from other view
//		fLocationStatusItem = (StatusLineContributionItem) actionBars.getStatusLineManager().find(RGraphicCompositeActionSet.POSITION_STATUSLINE_ITEM_ID);
		if (fLocationStatusItem != null) {
			fMouseLocationListener = new LocationListener() {
				
				final DecimalFormat format = new DecimalFormat("0.0####", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
				
				@Override
				public void loading() {
					if (fLocationStatusItem != null) {
						fLocationStatusItem.setText("..."); //$NON-NLS-1$
					}
				}
				
				@Override
				public void located(final double x, final double y) {
					if (fLocationStatusItem != null && UIAccess.isOkToUse(fControl)) {
						if (Double.isNaN(x) || Double.isInfinite(x)
								|| Double.isNaN(y) || Double.isInfinite(y) ) {
							fLocationStatusText = "NA"; //$NON-NLS-1$
						}
						else {
							final StringBuilder sb = new StringBuilder(32);
							sb.append('(');
							sb.append(format.format(x));
							sb.append(", "); //$NON-NLS-1$
							sb.append(format.format(y));
							sb.append(')');
							fLocationStatusText = sb.toString();
						}
						fLocationStatusItem.setText(fLocationStatusText);
					}
				}
			};
			fActions.addMouseClickLocationListener(fMouseLocationListener);
		}
	}
	
	protected RGraphicCompositeActionSet createActionSet() {
		return new RGraphicCompositeActionSet(fControl);
	}
	
	@Override
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
