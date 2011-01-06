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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.IViewDescriptor;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerCollection;
import de.walware.ecommons.ui.actions.HandlerContributionItem;
import de.walware.ecommons.ui.actions.SimpleContributionItem;
import de.walware.ecommons.ui.mpbv.ISession;
import de.walware.ecommons.ui.mpbv.ManagedPageBookView;


/**
 * Multi page view for R graphics.
 * <p>
 * No view is registered by this plug-in.</p>
 */
public abstract class PageBookRGraphicView extends ManagedPageBookView<PageBookRGraphicView.RGraphicSession>
		implements IERGraphicsManager.Listener {
	
	
	public class RGraphicSession implements ISession {
		
		
		private final IERGraphic fGraphic;
		
		
		public RGraphicSession(final IERGraphic graphic) {
			fGraphic = graphic;
		}
		
		
		public ImageDescriptor getImageDescriptor() {
			return null;
		}
		
		public String getLabel() {
			return fGraphic.getLabel();
		}
		
		public IERGraphic getGraphic() {
			return fGraphic;
		}
		
	}
	
	
	private static class OpenAdditionalView extends AbstractHandler {
		
		private static int gViewCounter;
		
		private IViewSite fViewSite;
		
		public OpenAdditionalView(final IViewSite viewSite) {
			fViewSite = viewSite;
		}
		
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			try {
				fViewSite.getWorkbenchWindow().getActivePage().showView(fViewSite.getId(), "#" + gViewCounter++, IWorkbenchPage.VIEW_ACTIVATE);
			}
			catch (final PartInitException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
						"An error occurred when opening an additional R graphics view.", e));
			}
			return null;
		}
		
	}
	
	
	private IERGraphicsManager fManager;
	
	private final IERGraphic.Listener fGraphicListener = new IERGraphic.Listener() {
		public void activated() {
			updateTitle();
		}
		public void deactivated() {
			updateTitle();
		}
		public void drawingStarted() {
		}
		public void drawingStopped() {
		}
	};
	
	
	public PageBookRGraphicView() {
	}
	
	
	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);
		fManager = loadManager();
	}
	
	protected abstract IERGraphicsManager loadManager();
	
	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);
		if (fManager != null) {
			fManager.addListener(this);
			final List<? extends IERGraphic> graphics = fManager.getAllGraphics();
			for (final IERGraphic graphic : graphics) {
				graphicAdded(graphic);
			}
		}
	}
	
	public void graphicAdded(final IERGraphic graphic) {
		super.newPage(new RGraphicSession(graphic), graphic.isActive());
	}
	
	public void graphicRemoved(final IERGraphic graphic) {
		final List<RGraphicSession> sessions = getSessions();
		for (final RGraphicSession session : sessions) {
			if (session.getGraphic() == graphic) {
				super.closePage(session);
				return;
			}
		}
	}
	
	@Override
	protected String getNoPageTitle() {
		return "No graphics at this time.";
	}
	
	@Override
	protected IHandler2 createNewPageHandler() {
		return null;
	}
	
	
	@Override
	protected void initActions(final IServiceLocator serviceLocator, final HandlerCollection handlers) {
		super.initActions(serviceLocator, handlers);
		
		final OpenAdditionalView openViewHandler = new OpenAdditionalView(getViewSite());
		handlers.add(".OpenView", openViewHandler);
	}
	
	@Override
	protected void contributeToActionBars(final IServiceLocator serviceLocator, final IActionBars actionBars, final HandlerCollection handlers) {
		super.contributeToActionBars(serviceLocator, actionBars, handlers);
		
		final IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(new Separator("view"));
		final IViewDescriptor viewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(getViewSite().getId());
		menuManager.add(new HandlerContributionItem(new CommandContributionItemParameter(serviceLocator,
				null, HandlerContributionItem.NO_COMMAND_ID, null,
				viewDescriptor.getImageDescriptor(), null, null,
				NLS.bind("Open Additional {0} View", viewDescriptor.getLabel()), "O", null,
				HandlerContributionItem.STYLE_PUSH, null, false), handlers.get(".OpenView")));
		menuManager.add(new Separator("save"));
		menuManager.add(new Separator(SharedUIResources.ADDITIONS_MENU_ID));
		
		menuManager.add(new Separator("settings")); //$NON-NLS-1$
		menuManager.add(new SimpleContributionItem("Preferences...", "P") {
			@Override
			protected void execute() throws ExecutionException {
				final Shell shell = getViewSite().getShell();
				final String[] preferencePages = collectContextMenuPreferencePages();
				if (preferencePages.length > 0 && (shell == null || !shell.isDisposed()))
						org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, null).open();
			}
		});
	}
	
	private String[] collectContextMenuPreferencePages() {
		final List<String> pageIds = new ArrayList<String>();
		collectContextMenuPreferencePages(pageIds);
		return pageIds.toArray(new String[pageIds.size()]);
	}
	
	protected void collectContextMenuPreferencePages(final List<String> pageIds) {
	}
	
	
	@Override
	protected RGraphicPage doCreatePage(final RGraphicSession session) {
		return new RGraphicPage(session.getGraphic());
	}
	
	@Override
	public void closePage(final RGraphicSession session) {
		final IStatus status = session.getGraphic().close();
		if (status != null && status.getSeverity() < IStatus.ERROR) {
			return;
		}
		super.closePage(session);
	}
	
	@Override
	protected void onPageShowing(final IPageBookViewPage page, final RGraphicSession session) {
		if (session != null) {
			session.getGraphic().addListener(fGraphicListener);
		}
		super.onPageShowing(page, session);
	}
	
	@Override
	protected void onPageHiding(final IPageBookViewPage page, final RGraphicSession session) {
		if (session != null) {
			session.getGraphic().removeListener(fGraphicListener);
		}
		super.onPageHiding(page, session);
	}
	
	@Override
	public void dispose() {
		if (fManager != null) {
			fManager.removeListener(this);
		}
		final RGraphicSession session = getCurrentSession();
		if (session != null) {
			session.getGraphic().removeListener(fGraphicListener);
		}
		super.dispose();
	}
	
}
