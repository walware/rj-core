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

package de.walware.rj.eclient.internal.graphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import de.walware.rj.server.client.RClientGraphic;
import de.walware.rj.server.client.RClientGraphicActions;
import de.walware.rj.server.client.RClientGraphicFactory;

import de.walware.ecommons.ConstList;
import de.walware.ecommons.FastList;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.rj.eclient.graphics.IERGraphic;
import de.walware.rj.eclient.graphics.IERGraphicInstruction;
import de.walware.rj.eclient.graphics.RGraphics;


/**
 * R graphic implementation of this plug-in. Implements R side API ({@link RClientGraphic})
 * as well as client side API ({@link IERGraphic}, {@link de.walware.rj.graphic.RGraphic}).
 */
public class EclipseRGraphic implements RClientGraphic, IERGraphic {
	
	
	private final int fDevId;
	
	private EclipseRGraphicFactory fManager;
	private boolean fIsRClosed;
	private boolean fIsLocalClosed;
	
	private boolean fIsActive;
	private int fMode;
	
	private int fOptions;
	private RClientGraphicActions fActions;
	private volatile double[] fNextSize;
	private double[] fSize;
	
	private Font fCurrentFont;
	private GC fCurrentGC;
	private FontMetrics fCurrentFontMetrics;
	
	private Display fDisplay;
	private final Map<Integer, Color> fColors = new HashMap<Integer, Color>();
	private final Map<FontData, Font> fFonts = new HashMap<FontData, Font>();
	
	private String fSerifFontName;
	private String fSansFontName;
	private String fMonoFontName;
	
	private final Object fInstructionsLock = new Object();
	private int fInstructionsSize;
	private IERGraphicInstruction[] fInstructions = new IERGraphicInstruction[512];
	
	private final FastList<IERGraphic.Listener> fListeners = new FastList<IERGraphic.Listener>(IERGraphic.Listener.class, FastList.IDENTITY);
	
	private final Runnable fActivatedRunnable = new Runnable() {
		public void run() {
			final Listener[] listeners = fListeners.toArray();
			for (final Listener listener : listeners) {
				listener.activated();
			}
		}
	};
	
	private final Runnable fDeactivatedRunnable = new Runnable() {
		public void run() {
			final Listener[] listeners = fListeners.toArray();
			for (final Listener listener : listeners) {
				listener.deactivated();
			}
		}
	};
	
	private final Runnable fDrawingStartedRunnable = new Runnable() {
		public void run() {
			final Listener[] listeners = fListeners.toArray();
			for (final Listener listener : listeners) {
				listener.drawingStarted();
			}
		}
	};
	
	private final Runnable fDrawingStoppedRunnable = new Runnable() {
		public void run() {
			final Listener[] listeners = fListeners.toArray();
			for (final Listener listener : listeners) {
				listener.drawingStopped();
			}
		}
	};
	
	
	public EclipseRGraphic(final int devId, final double w, final double h,
			final boolean active, final RClientGraphicActions actions, final int options,
			final EclipseRGraphicFactory manager) {
		fDevId = devId;
		fIsActive = active;
		fActions = actions;
		fManager = manager;
		fOptions = options;
		
		fDisplay = UIAccess.getDisplay();
		fColors.put(0x000000, new Color(fDisplay, 0, 0, 0));
		fColors.put(0xffffff, new Color(fDisplay, 0xff, 0xff, 0xff));
		
		fSize = fNextSize = new double[] { w, h };
		initPanel(w, h);
	}
	
	
	public String getLabel() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Device ");
		sb.append(fDevId + 1);
		if (fIsActive) {
			sb.append(" (active)");
		}
		if (fActions != null) {
			final String rLabel = fActions.getRLabel();
			if (rLabel != null && rLabel.length() > 0) {
				sb.append(" – ");
				sb.append(rLabel);
			}
		}
		return sb.toString();
	}
	
	private void add(final IERGraphicInstruction instr) {
		// adding is always in R thread
		if (fInstructionsSize+1 >= fInstructions.length) {
			synchronized (fInstructionsLock) {
				final IERGraphicInstruction[] newArray = new IERGraphicInstruction[fInstructionsSize + 128];
				System.arraycopy(fInstructions, 0, newArray, 0, fInstructionsSize);
				fInstructions = newArray;
			}
		}
		fInstructions[fInstructionsSize] = instr;
		fInstructionsSize++;
	}
	
	protected void initPanel(final double w, final double h) {
		fCurrentFont = null;
		fCurrentFontMetrics = null;
		
		final IPreferencesService preferences = Platform.getPreferencesService();
		fSerifFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY, "", null);
		fSansFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SANS_FONTNAME_KEY, "", null);;
		fMonoFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_MONO_FONTNAME_KEY, "", null);;
		
		add(new GraphicInitialization(w, h));
	}
	
	public void reset(final double w, final double h) {
		synchronized (fInstructionsLock) {
			for (int i = 0; i < fInstructionsSize; i++) {
				fInstructions[i] = null;
			}
			fInstructionsSize = 0;
		}
		
		initPanel(w, h);
	}
	
	public int getDevId() {
		return this.fDevId;
	}
	
	public void setActive(final boolean active) {
		if (fIsActive == active) {
			return;
		}
		this.fIsActive = active;
		if (active) {
			fDisplay.asyncExec(fActivatedRunnable);
		}
		else {
			fDisplay.asyncExec(fDeactivatedRunnable);
		}
	}
	
	public boolean isActive() {
		return fIsActive;
	}
	
	public void setMode(final int mode) {
		fMode = mode;
		if (mode == 1) {
			fDisplay.asyncExec(fDrawingStartedRunnable);
		}
		else if (mode == 0) {
			fDisplay.asyncExec(fDrawingStoppedRunnable);
		}
	}
	
	public double[] computeSize() {
		return fSize;
	}
	
	public double[] computeFontMetric(final int ch) {
		final double[] answer = new double[] { 0.0, 0.0, 0.0 };
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fCurrentGC == null) {
					fCurrentGC = new GC(Display.getCurrent());
				}
				if (fCurrentFontMetrics == null) {
					fCurrentGC.setFont(fCurrentFont);
					fCurrentFontMetrics = fCurrentGC.getFontMetrics();
				}
				answer[0] = fCurrentFontMetrics.getAscent();
				answer[1] = fCurrentFontMetrics.getDescent();
				answer[2] = (ch > 0) ? fCurrentGC.getAdvanceWidth((char) ch) : fCurrentFontMetrics.getAverageCharWidth();
			}
		});
		return answer;
	}
	
	public double[] computeStringWidth(final String txt) {
		final double[] answer = new double[] { (8 * txt.length()) };
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fCurrentGC == null) {
					fCurrentGC = new GC(Display.getCurrent());
				}
				if (fCurrentFontMetrics == null) {
					fCurrentGC.setFont(fCurrentFont);
					fCurrentFontMetrics = fCurrentGC.getFontMetrics();
				}
				answer[0] = fCurrentGC.textExtent(txt, SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_TRANSPARENT).x;
			}
		});
		return answer;
	}
	
	public void addSetClip(final double x0, final double y0, final double x1, final double y1) {
		final ClipSetting instr = new ClipSetting(x0, y0, x1, y1);
		add(instr);
	}
	
	public void addSetColor(final int color) {
		Color swtColor = fColors.get((color & 0xffffff));
		if (swtColor == null) {
			swtColor = new Color(fDisplay,
					(color & 0xff),
					((color >> 8) & 0xff),
					((color >> 16) & 0xff) );
			fColors.put((color & 0xffffff), swtColor);
		}
		final ColorSetting instr = new ColorSetting(color, swtColor);
		add(instr);
	}
	
	public void addSetFill(final int color) {
		Color swtColor = fColors.get((color & 0xffffff));
		if (swtColor == null) {
			swtColor = new Color(fDisplay,
					(color & 0xff),
					((color >> 8) & 0xff),
					((color >> 16) & 0xff) );
			fColors.put((color & 0xffffff), swtColor);
		}
		final FillSetting instr = new FillSetting(color, swtColor);
		add(instr);
	}
	
	public void addSetFont(String family, final int face, final double pointSize,
			final double cex, final double lineHeight) {
		int style;
		switch (face) {
		case 2:
			style = SWT.BOLD;
			break;
		case 3:
			style = SWT.ITALIC;
			break;
		case 4:
			style = SWT.BOLD | SWT.ITALIC;
			break;
//		case 5: Symbolfont
		default:
			style = 0;
			break;
		}
		if (family.equals("serif")) {
			family = fSerifFontName;
		}
		else if (family.equals("sansserif")) {
			family = fSansFontName;
		}
		else if (family.equals("mono")) {
			family = fMonoFontName;
		}
		final FontData fontData = new FontData(family,
				(int) (pointSize * cex + 0.5), style);
		fCurrentFont = fFonts.get(fontData);
		if (fCurrentFont == null) {
			fCurrentFont = new Font(fDisplay, fontData);
			fFonts.put(fontData, fCurrentFont);
		}
		fCurrentFontMetrics = null;
		final FontSetting instr = new FontSetting(family, face, pointSize, cex, lineHeight,
				fCurrentFont);
		add(instr);
	}
	
	public void addSetLine(final int type, final double width) {
		final LineSetting instr = new LineSetting(type, width);
		add(instr);
	}
	
	public void addDrawLine(final double x0, final double y0, final double x1, final double y1) {
		final LineElement instr = new LineElement(x0, y0, x1, y1);
		add(instr);
	}
	
	public void addDrawRect(final double x0, final double y0, final double x1, final double y1) {
		final RectElement instr = new RectElement(x0, y0, x1, y1);
		add(instr);
	}
	
	public void addDrawPolyline(final double[] x, final double[] y) {
		final PolylineElement instr = new PolylineElement(x, y);
		add(instr);
	}
	
	public void addDrawPolygon(final double[] x, final double[] y) {
		final PolygonElement instr = new PolygonElement(x, y);
		add(instr);
	}
	
	public void addDrawCircle(final double x, final double y, final double r) {
		final CircleElement instr = new CircleElement(x, y, r);
		add(instr);
	}
	
	public void addDrawText(final double x, final double y, final double hAdj, final double rDeg, final String text) {
		final TextElement instr = new TextElement(x, y, hAdj, rDeg, text);
		add(instr);
	}
	
	public void closeFromR() {
		fIsRClosed = true;
		if (fIsLocalClosed
				|| (fOptions & RClientGraphicFactory.R_CLOSE_OFF) == 0) {
			dispose();
		}
		setActive(false);
	}
	
	public void dispose() {
		fListeners.clear();
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				for (final Color color : fColors.values()) {
					if (!color.isDisposed()) {
						color.dispose();
					}
				}
				fColors.clear();
				for (final Font font : fFonts.values()) {
					if (!font.isDisposed()) {
						font.dispose();
					}
				}
				fFonts.clear();
				if (fCurrentGC != null && !fCurrentGC.isDisposed()) {
					fCurrentGC.dispose();
					fCurrentGC = null;
				}
			}
		});
	}
	
	
	public List<IERGraphicInstruction> getInstructions() {
		final IERGraphicInstruction[] array;
		synchronized (fInstructionsLock) {
			array = new IERGraphicInstruction[fInstructionsSize];
			System.arraycopy(fInstructions, 0, array, 0, array.length);
		}
		return new ConstList<IERGraphicInstruction>(array);
	}
	
	public Object getRHandle() {
		if (fActions != null) {
			return fActions.getRHandle();
		}
		return null;
	}
	
	public IStatus copy(final String toDev, final String toDevFile, final String toDevArgs) {
		if (fActions != null) {
			return fActions.copyGraphic(fDevId, toDev, toDevFile, toDevArgs);
		}
		return null;
	}
	
	public IStatus resize(final double w, final double h) {
		if (fActions != null) {
			fNextSize = new double[] { w, h };
			return fActions.resizeGraphic(fDevId, new Runnable() {
				public void run() {
					fSize = fNextSize;
				}
			});
		}
		return null;
	}
	
	public IStatus close() {
		if (fIsRClosed) {
			fIsLocalClosed = true;
			dispose();
		}
		if (fActions != null) {
			return fActions.closeGraphic(fDevId);
		}
		else {
			fManager.close(this);
			fIsLocalClosed = true;
			dispose();
		}
		return null;
	}
	
	public void addListener(final Listener listener) {
		fListeners.add(listener);
	}
	
	public void removeListener(final Listener listener) {
		fListeners.remove(listener);
	}
	
}
