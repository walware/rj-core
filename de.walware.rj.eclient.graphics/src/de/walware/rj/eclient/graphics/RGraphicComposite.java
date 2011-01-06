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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;


/**
 * Composite to display an R graphic.
 */
public class RGraphicComposite extends Composite implements IERGraphic.Listener {
	
	
	/**
	 * This class provides the layout for RGraphicComposite
	 */
	private static class ScrolledCompositeLayout extends Layout {
		
		static final int DEFAULT_WIDTH	= 64;
		static final int DEFAULT_HEIGHT	= 64;
		
		
		private boolean fInLayout = false;
		
		
		public ScrolledCompositeLayout() {
		}
		
		
		@Override
		protected boolean flushCache(final Control control) {
			return true;
		}
		
		@Override
		protected Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flushCache) {
			final RGraphicComposite sc = (RGraphicComposite) composite;
			final Point size = new Point(wHint, hHint);
			if (wHint == SWT.DEFAULT) {
				size.x = Math.max(DEFAULT_WIDTH, sc.fWidth);
			}
			if (hHint == SWT.DEFAULT) {
				size.y = Math.max(DEFAULT_HEIGHT, sc.fHeight);
			}
			return size;
		}
		
		@Override
		protected void layout(final Composite composite, final boolean flushCache) {
			if (fInLayout || composite == null) {
				return;
			}
			final RGraphicComposite sc = (RGraphicComposite) composite;
			final ScrollBar hBar = sc.getHorizontalBar();
			final ScrollBar vBar = sc.getVerticalBar();
			
			if (hBar.getSize().y >= sc.getSize().y) {
				return;
			}
			if (vBar.getSize().x >= sc.getSize().x) {
				return;
			}
			
			fInLayout = true;
			
			final Rectangle previousBounds = sc.fCanvas.getBounds();
			boolean hVisible = sc.needHBar(previousBounds, false);
			final boolean vVisible = sc.needVBar(previousBounds, hVisible);
			if (!hVisible && vVisible) {
				hVisible = sc.needHBar(previousBounds, vVisible);
			}
			hBar.setVisible(hVisible);
			vBar.setVisible(vVisible);
			
			final Rectangle clientArea = sc.getClientArea();
			final int x;
			final int y;
			final int width = sc.fWidth;
			final int height = sc.fHeight;
			hBar.setThumb(clientArea.width);
			hBar.setPageIncrement(clientArea.width);
			if (hVisible) {
				x = (sc.fChanged || previousBounds.x >= 0) ? 0 :
						Math.max(previousBounds.x, clientArea.width - width);
				hBar.setSelection(-x);
			}
			else {
				x = (clientArea.width - width) / 2;
				hBar.setSelection(0);
			}
			vBar.setThumb(clientArea.height);
			vBar.setPageIncrement(clientArea.height);
			if (vVisible) {
				y = (sc.fChanged || previousBounds.y >= 0) ? 0 :
						Math.max(previousBounds.y, clientArea.height - height);
				vBar.setSelection(-y);
			}
			else {
				y = (clientArea.height - height) / 2;
				vBar.setSelection(0);
			}
			
			sc.fChanged = false;
			sc.fCanvas.setBounds(x, y, width, height);
			fInLayout = false;
		}
		
	}
	
	
	private static int checkStyle (final int style) {
		final int mask = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}
	
	
	private IERGraphic fGraphic;
	
	private RGraphicCanvas fCanvas;
	
	private int fWidth = 0;
	private int fHeight = 0;
	
	private boolean fChanged;
	
	
	public RGraphicComposite(final Composite parent, final IERGraphic graphic) {
		super(parent, checkStyle(SWT.H_SCROLL | SWT.V_SCROLL));
		super.setLayout(new ScrolledCompositeLayout());
		
		final ScrollBar hBar = getHorizontalBar();
		hBar.setVisible(false);
		hBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				scrollH();
			}
		});
		final ScrollBar vBar = getVerticalBar();
		vBar.setVisible(false);
		vBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				scrollV();
			}
		});
		addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {
//				checkContentSize();
			}
		});
		
		createCanvas();
		
		setGraphic(graphic);
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				if (fGraphic != null) {
					fGraphic.removeListener(RGraphicComposite.this);
				}
			}
		});
		isDisposed();
	}
	
	private void createCanvas() {
		fCanvas = new RGraphicCanvas(this);
	}
	
	boolean needHBar(final Rectangle contentRect, final boolean vVisible) {
		final Rectangle hostRect = getBounds();
		hostRect.width -= 2 * getBorderWidth();
		if (vVisible) {
			hostRect.width -= getVerticalBar().getSize().x;
		}
		return (fWidth > hostRect.width);
	}
	
	boolean needVBar(final Rectangle contentRect, final boolean hVisible) {
		final Rectangle hostRect = getBounds();
		hostRect.height -= 2 * getBorderWidth();
		if (hVisible) {
			hostRect.height -= getHorizontalBar().getSize().y;
		}
		return (fHeight > hostRect.height);
	}
	
	private void scrollH() {
		final Point location = fCanvas.getLocation();
		final ScrollBar hBar = getHorizontalBar();
		final int hSelection = hBar.getSelection();
		fCanvas.setLocation(-hSelection, location.y);
	}
	
	private void scrollV() {
		final Point location = fCanvas.getLocation();
		final ScrollBar vBar = getVerticalBar();
		final int vSelection = vBar.getSelection();
		fCanvas.setLocation(location.x, -vSelection);
	}
	
	public void setGraphic(final IERGraphic graphic) {
		if (fGraphic != null) {
			fGraphic.removeListener(this);
		}
		fGraphic = graphic;
		if (fGraphic != null) {
			fGraphic.addListener(this);
		}
		
		drawingStopped();
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Note: No Layout can be set on this Control because it already
	 * manages the size and position of its children.</p>
	 */
	@Override
	public void setLayout (final Layout layout) {
		checkWidget();
		return;
	}
	
	public void activated() {
	}
	
	public void deactivated() {
	}
	
	public void drawingStarted() {
	}
	
	public void drawingStopped() {
		final List<? extends IERGraphicInstruction> instructions = (fGraphic != null) ?
				fGraphic.getInstructions() : RGraphicCanvas.NO_GRAPHIC;
		fCanvas.setInstructions(instructions);
		checkContentSize();
		fCanvas.redraw();
	}
	
	private void checkContentSize() {
		final Point size = fCanvas.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (fWidth == size.x && fHeight == size.y) {
			return;
		}
		fChanged = true;
		fWidth = size.x;
		fHeight = size.y;
		fCanvas.setSize(fWidth, fHeight);
		
		final ScrollBar hBar = getHorizontalBar();
		hBar.setMaximum(fWidth);
		
		final ScrollBar vBar = getVerticalBar();
		vBar.setMaximum(fHeight);
		layout(false);
	}
	
	
	public void redrawGraphic() {
		fCanvas.redraw();
	}
	
	public double[] getGraphicFitSize() {
		final Rectangle bounds = getBounds();
		return new double[] { bounds.width, bounds.height };
	}
	
}
