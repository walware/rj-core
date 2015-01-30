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

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import de.walware.rj.eclient.internal.graphics.GraphicInitialization;


/**
 * SWT control to paint an R graphic.
 * <p>
 * See {@link RGraphicComposite} for a higher API widget.</p>
 */
public class RGraphicCanvas extends Canvas implements PaintListener {
	
	
	public static final List<IERGraphicInstruction> NO_GRAPHIC = Collections.emptyList();
	
	
	private List<? extends IERGraphicInstruction> fGraphicInstructions;
	private final DefaultGCRenderer fRenderer;
	
	private GraphicInitialization fInit;
	
	
	public RGraphicCanvas(final Composite parent) {
		super(parent, checkStyle(0));
		
		fGraphicInstructions = Collections.emptyList();
		fRenderer = new DefaultGCRenderer();
		addPaintListener(this);
	}
	
	private static int checkStyle(int style) {
		style |= SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND;
		style |= SWT.DOUBLE_BUFFERED;
		return style;
	}
	
	public void setInstructions(final List<? extends IERGraphicInstruction> graphicData) {
		fGraphicInstructions = graphicData;
		if (!fGraphicInstructions.isEmpty()) {
			fInit = (GraphicInitialization) fGraphicInstructions.get(0);
		}
		else {
			fInit = null;
		}
	}
	
	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		final GraphicInitialization init = fInit;
		if (init != null) {
			return new Point((int) (init.width + 0.5), (int) (init.height + 0.5));
		}
		else {
			return super.computeSize(wHint, hHint, changed);
		}
	}
	
	public double widget2graphicsX(final double x) {
		return x; // scale
	}
	
	public double widget2graphicY(final double y) {
		return y; // scale
	}
	
	@Override
	public void paintControl(final PaintEvent e) {
		final GC gc = e.gc;
		
		final Rectangle clientArea = getClientArea();
		setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		drawBackground(gc, 0, 0, clientArea.width, clientArea.height);
		if (fGraphicInstructions.isEmpty()) {
			return;
		}
		fRenderer.clear(1.0f); // scale
		fRenderer.paint(gc, fGraphicInstructions);
	}
	
}
