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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;


/**
 * Composite to display an R graphic.
 */
public class RGraphicComposite extends Composite
		implements IERGraphic.ListenerInstructionsExtension, IERGraphic.ListenerLocatorExtension {
	
	
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
				x = (sc.fChangedLayout || previousBounds.x >= 0) ? 0 :
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
				y = (sc.fChangedLayout || previousBounds.y >= 0) ? 0 :
						Math.max(previousBounds.y, clientArea.height - height);
				vBar.setSelection(-y);
			}
			else {
				y = (clientArea.height - height) / 2;
				vBar.setSelection(0);
			}
			
			sc.fChangedLayout = false;
			sc.fCanvas.setBounds(x, y, width, height);
			fInLayout = false;
		}
		
	}
	
	private class PanListener implements Listener {
		
		private boolean started;
		private Point startMouse;
		
		public void handleEvent(final Event event) {
			switch (event.type) {
			case SWT.MouseDown:
				if (event.button == 2) {
					started = true;
					updateCursor();
					
					startMouse = checkedPoint(event);
					return;
				}
				else {
					started = false;
					return;
				}
			case SWT.MouseExit:
			case SWT.MouseMove:
				if (started) {
					pan(checkedPoint(event));
					return;
				}
				else {
					return;
				}
			case SWT.MouseUp:
				if (started && event.button == 2) {
					started = false;
					updateCursor();
					return;
				}
				else {
					return;
				}
			case SWT.KeyDown:
				switch (event.keyCode) {
				case SWT.ARROW_DOWN:
					if (update(getVerticalBar(), fPanIncrement)) {
						scrollV();
					}
					return;
				case SWT.ARROW_UP:
					if (update(getVerticalBar(), -fPanIncrement)) {
						scrollV();
					}
					return;
				case SWT.ARROW_RIGHT:
					if (update(getHorizontalBar(), fPanIncrement)) {
						scrollH();
					}
					return;
				case SWT.ARROW_LEFT:
					if (update(getHorizontalBar(), -fPanIncrement)) {
						scrollH();
					}
					return;
				default:
					return;
				}
			}
		}
		
		private Point checkedPoint(final Event event) {
			if (event.item == RGraphicComposite.this) {
				return new Point(event.x, event.y);
			}
			else {
				final Point point = ((Control) event.widget).toDisplay(event.x, event.y);
				return RGraphicComposite.this.toControl(point);
			}
		}
		
		private void pan(Point point) {
			if (update(getHorizontalBar(), startMouse.x - point.x)) {
				scrollH();
			}
			if (update(getVerticalBar(), startMouse.y - point.y)) {
				scrollV();
			}
			startMouse = point;
		}
		
		private boolean update(ScrollBar bar, int step) {
			if (bar != null && bar.isVisible()) {
				int selection = bar.getSelection() + step;
				if (selection < 0) {
					selection = 0;
				}
				else if (selection > bar.getMaximum()) {
					selection = bar.getMaximum() - bar.getThumb();
				}
				bar.setSelection(selection);
				return true;
			}
			return false;
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
	
	private boolean fChangedLayout;
	private boolean fChangedContent;
	
	private int fPanIncrement;
	private final PanListener fPanListener;
	
	private MouseListener fLocatorListener;
	
	
	public RGraphicComposite(final Composite parent, final IERGraphic graphic) {
		super(parent, checkStyle(SWT.H_SCROLL | SWT.V_SCROLL));
		super.setLayout(new ScrolledCompositeLayout());
		
		fPanIncrement = 10;
		
		final ScrollBar hBar = getHorizontalBar();
		hBar.setVisible(false);
		hBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				scrollH();
			}
		});
		hBar.setIncrement(fPanIncrement);
		final ScrollBar vBar = getVerticalBar();
		vBar.setVisible(false);
		vBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				scrollV();
			}
		});
		vBar.setIncrement(fPanIncrement);
		addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {
//				checkContentSize();
			}
		});
		createCanvas();
		final Listener updateListener = new Listener() {
			public void handleEvent(final Event event) {
				if (fChangedContent) {
					updateGraphic();
				}
			}
		};
		addListener(SWT.Show, updateListener);
		addListener(SWT.Activate, updateListener);
		
		fPanListener = new PanListener();
		addListener(SWT.MouseDown, fPanListener);
		addListener(SWT.MouseMove, fPanListener);
		addListener(SWT.MouseExit, fPanListener);
		addListener(SWT.MouseUp, fPanListener);
		addListener(SWT.KeyDown, fPanListener);
		fCanvas.addListener(SWT.MouseDown, fPanListener);
		fCanvas.addListener(SWT.MouseMove, fPanListener);
		fCanvas.addListener(SWT.MouseExit, fPanListener);
		fCanvas.addListener(SWT.MouseUp, fPanListener);
		fCanvas.addListener(SWT.KeyDown, fPanListener);
		
		setGraphic(graphic);
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				disconnect();
			}
		});
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
		if (hBar != null && hBar.isVisible()) {
			final int hSelection = hBar.getSelection();
			fCanvas.setLocation(-hSelection, location.y);
		}
	}
	
	private void scrollV() {
		final Point location = fCanvas.getLocation();
		final ScrollBar vBar = getVerticalBar();
		if (vBar != null && vBar.isVisible()) {
			final int vSelection = vBar.getSelection();
			fCanvas.setLocation(location.x, -vSelection);
		}
	}
	
	
	protected void disconnect() {
		if (fGraphic != null) {
			fGraphic.removeListener(this);
		}
		locatorStopped();
	}
	
	public void setGraphic(final IERGraphic graphic) {
		disconnect();
		
		fGraphic = graphic;
		if (fGraphic != null) {
			fGraphic.addListener(this);
		}
		
		instructionsChanged(true, null);
		
		if (fGraphic != null && fGraphic.isLocatorStarted()) {
			locatorStarted();
		}
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
	
	public void instructionsChanged(final boolean reset, final List<IERGraphicInstruction> added) {
		fChangedContent = true;
		if (isVisible()) {
			updateGraphic();
		}
	}
	
	public void drawingStopped() {
	}
	
	public void locatorStarted() {
		if (fLocatorListener == null) {
			fLocatorListener = new MouseListener() {
				public void mouseDown(final MouseEvent e) {
					switch (e.button) { 
					case 1:
						fGraphic.returnLocator(
								fCanvas.widget2graphicsX(e.x), fCanvas.widget2graphicY(e.y));
						break;
					case 3:
						fGraphic.stopLocator(IERGraphic.LOCATOR_DONE);
						break;
					default:
						break;
					}
				}
				public void mouseUp(final MouseEvent e) {
				}
				public void mouseDoubleClick(final MouseEvent e) {
				}
			};
			fCanvas.addMouseListener(fLocatorListener);
			updateCursor();
		}
	}
	
	public void locatorStopped() {
		if (fLocatorListener != null) {
			fCanvas.removeMouseListener(fLocatorListener);
			fLocatorListener = null;
			updateCursor();
		}
	}
	
	private void updateCursor() {
		if (fPanListener.started) {
			fCanvas.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
		}
		else if (fLocatorListener != null) {
			fCanvas.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_CROSS));
		}
		else {
			fCanvas.setCursor(null);
		}
	}
	
	private void checkContentSize() {
		final Point size = fCanvas.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (fWidth == size.x && fHeight == size.y) {
			return;
		}
		fChangedLayout = true;
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
	
	private void updateGraphic() {
		fChangedContent = false;
		final List<? extends IERGraphicInstruction> instructions = (fGraphic != null) ?
				fGraphic.getInstructions() : RGraphicCanvas.NO_GRAPHIC;
		fCanvas.setInstructions(instructions);
		checkContentSize();
		fCanvas.redraw();
	}
	
	public double[] getGraphicFitSize() {
		final Rectangle bounds = getBounds();
		return new double[] { bounds.width, bounds.height };
	}
	
	public Control getControl() {
		return this;
	}
	
	/**
	 * Returns the control on which the graphic is painted
	 * 
	 * @since 1.0
	 */
	public Control getGraphicWidget() {
		return fCanvas;
	}
	
	/**
	 * Converts a horizontal display coordinate of the {@link #getGraphicWidget() graphic widget}
	 * to its graphic coordinate value.
	 * 
	 * @since 1.0
	 * @see IERGraphic Coordinate systems of R graphics
	 */
	public double convertWidget2GraphicX(final int x) {
		return fCanvas.widget2graphicsX(x);
	}
	
	/**
	 * Converts a vertical display coordinate of the {@link #getGraphicWidget() graphic widget}
	 * to its graphic coordinate value.
	 * 
	 * @since 1.0
	 * @see IERGraphic Coordinate systems of R graphics
	 */
	public double convertWidget2GraphicY(final int y) {
		return fCanvas.widget2graphicY(y);
	}
	
}
