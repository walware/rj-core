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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.IViewDescriptor;

import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerCollection;
import de.walware.ecommons.ui.actions.HandlerContributionItem;
import de.walware.ecommons.ui.actions.SimpleContributionItem;
import de.walware.ecommons.ui.mpbv.ISession;
import de.walware.ecommons.ui.mpbv.ManagedPageBookView;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.rj.eclient.AbstractRToolRunnable;
import de.walware.rj.eclient.IRToolService;


/**
 * Multi page view for R graphics.
 * <p>
 * No view is registered by this plug-in.</p>
 */
public abstract class PageBookRGraphicView extends ManagedPageBookView<PageBookRGraphicView.RGraphicSession> {
	
	
	public class RGraphicSession implements ISession {
		
		
		private final IERGraphic fGraphic;
		
		
		public RGraphicSession(final IERGraphic graphic) {
			fGraphic = graphic;
		}
		
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return ImageDescriptor.createFromImage(PageBookRGraphicView.this.getTitleImage());
		}
		
		@Override
		public String getLabel() {
			return fGraphic.getLabel();
		}
		
		public IERGraphic getGraphic() {
			return fGraphic;
		}
		
	}
	
	public static class ShowRequiredViewListener implements IERGraphicsManager.ListenerShowExtension {
		
		
		private final String fViewId;
		
		
		public ShowRequiredViewListener(final String viewId) {
			fViewId = viewId;
		}
		
		
		@Override
		public int canShowGraphic(final IERGraphic graphic) {
			return 0;
		}
		
		private static String getId(final IViewReference ref) {
			// E-Bug #405563
			final String id = ref.getId();
			final int idx = id.indexOf(':');
			return (idx >= 0) ? id.substring(0, idx) : id;
		}
		
		@Override
		public void showGraphic(final IERGraphic graphic) {
			try {
				final IWorkbenchPage page = getBestPage(graphic);
				String secondaryId = ""; //$NON-NLS-1$
				final IViewReference[] refs = page.getViewReferences();
				for (int i = 0; i < refs.length; i++) { // search views not yet instanced
					if (fViewId.equals(getId(refs[i])) && refs[i].getView(false) == null) {
						if (refs[i].getSecondaryId() == null) {
							secondaryId = null;
							break;
						}
						if (secondaryId == "") { //$NON-NLS-1$
							secondaryId = refs[i].getSecondaryId();
						}
					}
				}
				if (secondaryId == "") { //$NON-NLS-1$
					secondaryId = "t"+System.currentTimeMillis(); //$NON-NLS-1$
				}
				gNewViewGraphic = graphic;
				page.showView(fViewId, secondaryId, IWorkbenchPage.VIEW_VISIBLE );
			}
			catch (final PartInitException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID,
						"An error occurred when opening a new R Graphics view.", e ));
			}
			finally {
				gNewViewGraphic = null;
			}
		}
		
		protected IWorkbenchPage getBestPage(final IERGraphic graphic) {
			return UIAccess.getActiveWorkbenchPage(true);
		}
		
		@Override
		public void graphicAdded(final IERGraphic graphic) {
		}
		
		@Override
		public void graphicRemoved(final IERGraphic graphic) {
		}
		
	}
	
	
	protected static abstract class NewDevHandler extends AbstractHandler {
		
		
		public NewDevHandler() {
		}
		
		
		protected abstract ITool getTool() throws CoreException;
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			try {
				final ITool tool = getTool();
				if (tool != null) {
					tool.getQueue().add(new AbstractRToolRunnable(
						"r/rj/gd/new", "New R Graphic") { //$NON-NLS-1$
						
						@Override
						public void run(final IRToolService r,
								final IProgressMonitor monitor) throws CoreException {
							r.evalVoid("rj.gd::rj.GD()", monitor); //$NON-NLS-1$
						}
						
					});
				}
			}
			catch (final CoreException e) {
				if (e.getStatus().getSeverity() != IStatus.CANCEL) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
							"An error occurrend when creating a new graphic device.", e),
							StatusManager.LOG | StatusManager.SHOW);
				}
			}
			return null;
		}
		
	}
	
	
	private static class RGraphicComparator implements Comparator<RGraphicSession> {
		
		public RGraphicComparator() {
		}
		
		@Override
		public int compare(final RGraphicSession o1, final RGraphicSession o2) {
			final ITool handle1 = o1.fGraphic.getRHandle();
			final ITool handle2 = o2.fGraphic.getRHandle();
			if (handle1 == null) {
				if (handle2 == null) {
					return 0;
				}
				return Integer.MIN_VALUE;
			}
			else if (handle2 == null) {
				return Integer.MAX_VALUE;
			}
			if (handle1 != handle2) {
				final int diff = handle1.getLabel(ITool.LONG_LABEL).compareTo(handle2.getLabel(ITool.LONG_LABEL));
				if (diff != 0) {
					return diff;
				}
			}
			return o1.fGraphic.getDevId() - o2.fGraphic.getDevId();
		}
		
	}
	
	
	private static class OpenAdditionalViewHandler extends AbstractHandler {
		
		private final IViewSite fViewSite;
		
		public OpenAdditionalViewHandler(final IViewSite viewSite) {
			fViewSite = viewSite;
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			try {
				final String secondaryId = "t" + System.currentTimeMillis(); //$NON-NLS-1$
				fViewSite.getWorkbenchWindow().getActivePage().showView(fViewSite.getId(),
						secondaryId, IWorkbenchPage.VIEW_ACTIVATE );
			}
			catch (final PartInitException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
						"An error occurred when opening an additional R graphics view.", e));
			}
			return null;
		}
		
	}
	
	private class PinPageAction extends SimpleContributionItem {
		
		public PinPageAction() {
			super(SharedUIResources.getImages().getDescriptor(SharedUIResources.LOCTOOL_PIN_PAGE_IMAGE_ID),
					SharedUIResources.getImages().getDescriptor(SharedUIResources.LOCTOOLD_PIN_PAGE_IMAGE_ID),
					"Pin Graphic Page", "P", SimpleContributionItem.STYLE_CHECK);
			setChecked(fPinPage);
		}
		
		@Override
		protected void execute() throws ExecutionException {
			fPinPage = !fPinPage;
			setChecked(fPinPage);
		}
		
	}
	
	
	private static IERGraphic gNewViewGraphic;
	
	private IERGraphicsManager fManager;
	private final IERGraphicsManager.ListenerShowExtension fManagerListener = new IERGraphicsManager.ListenerShowExtension() {
		private IERGraphic toShow;
		@Override
		public int canShowGraphic(final IERGraphic graphic) {
			return PageBookRGraphicView.this.canShowGraphic(graphic);
		}
		@Override
		public void showGraphic(final IERGraphic graphic) {
			toShow = graphic;
			final IViewSite site = getViewSite();
			try {
				site.getPage().showView(site.getId(), site.getSecondaryId(), IWorkbenchPage.VIEW_VISIBLE);
			}
			catch (final PartInitException e) {}
		}
		@Override
		public void graphicAdded(final IERGraphic graphic) {
			add(graphic, graphic == toShow
					|| (graphic.isActive() && (!fPinPage || getCurrentSession() == null)) );
			toShow = null;
		}
		@Override
		public void graphicRemoved(final IERGraphic graphic) {
			final RGraphicSession session = getSession(graphic);
			if (session != null) {
				PageBookRGraphicView.super.closePage(session);
			}
		}
	};
	
	private boolean fPinPage;
	
	private final IERGraphic.Listener fGraphicListener = new IERGraphic.ListenerLocatorExtension() {
		@Override
		public void activated() {
			updateTitle();
		}
		@Override
		public void deactivated() {
			updateTitle();
		}
		@Override
		public void drawingStarted() {
		}
		@Override
		public void drawingStopped() {
		}
		@Override
		public void locatorStarted() {
			updateTitle();
		}
		@Override
		public void locatorStopped() {
			updateTitle();
		}
	};
	
	private StatusLineContributionItem fPositionStatusLineItem;
	
	
	public PageBookRGraphicView() {
	}
	
	
	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		setSessionComparator(new RGraphicComparator());
		super.init(site, memento);
		fManager = loadManager();
	}
	
	protected abstract IERGraphicsManager loadManager();
	
	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);
		if (fManager != null) {
			fManager.addListener(fManagerListener);
			IERGraphic active = gNewViewGraphic;
			if (active != null) {
				final List<? extends IERGraphic> graphics = fManager.getAllGraphics();
				for (final IERGraphic graphic : graphics) {
					add(graphic, false);
				}
			}
			else {
				final List<? extends IERGraphic> graphics = fManager.getAllGraphics();
				for (final IERGraphic graphic : graphics) {
					add(graphic, false);
					if (graphic.isActive()) {
						active = graphic;
					}
				}
				if (active == null && !graphics.isEmpty()) {
					active = graphics.get(graphics.size()-1);
				}
			}
			if (active != null) {
				final RGraphicSession session = getSession(active);
				if (session != null) {
					showPage(session);
				}
			}
		}
	}
	
	public RGraphicSession getSession(final IERGraphic graphic) {
		final List<RGraphicSession> sessions = getSessions();
		for (final RGraphicSession session : sessions) {
			if (session.getGraphic() == graphic) {
				return session;
			}
		}
		return null;
	}
	
	protected int canShowGraphic(final IERGraphic graphic) {
		final RGraphicSession session = getCurrentSession();
		int canShow;
		if (session != null && session.getGraphic() == graphic) {
			canShow = (fPinPage) ? 20 : 10;
		}
		else if (fPinPage && session != null) {
			return -1;
		}
		else {
			canShow = 1;
		}
		if (getViewSite().getPage().isPartVisible(this)) {
			canShow+=2;
		}
		return canShow;
	}
	
	protected void add(final IERGraphic graphic, final boolean show) {
		super.newPage(new RGraphicSession(graphic), show);
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
		
		final OpenAdditionalViewHandler openViewHandler = new OpenAdditionalViewHandler(getViewSite());
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
				if (preferencePages.length > 0 && (shell == null || !shell.isDisposed())) {
					org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, null).open();
				}
			}
		});
		
		final IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.insertAfter("page_control.change_page", new PinPageAction()); //$NON-NLS-1$
		
		final IStatusLineManager lineManager = actionBars.getStatusLineManager();
		fPositionStatusLineItem = new StatusLineContributionItem(RGraphicCompositeActionSet.POSITION_STATUSLINE_ITEM_ID, 20);
		lineManager.add(fPositionStatusLineItem);
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
	protected void initPage(final IPageBookViewPage page) {
		super.initPage(page);
		if (page instanceof RGraphicPage) {
			((RGraphicPage) page).init(fPositionStatusLineItem);
		}
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
		if (fPositionStatusLineItem != null) {
			fPositionStatusLineItem.setText(""); //$NON-NLS-1$
		}
		super.onPageHiding(page, session);
	}
	
	@Override
	public void dispose() {
		if (fManager != null) {
			fManager.removeListener(fManagerListener);
		}
		final RGraphicSession session = getCurrentSession();
		if (session != null) {
			session.getGraphic().removeListener(fGraphicListener);
		}
		super.dispose();
	}
	
}
