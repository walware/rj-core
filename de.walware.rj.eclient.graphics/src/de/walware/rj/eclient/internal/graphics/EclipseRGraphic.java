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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.rj.graphic.utils.CachedMapping;
import de.walware.rj.graphic.utils.CharMapping;
import de.walware.rj.graphic.utils.Unicode2AdbSymbolMapping;
import de.walware.rj.server.client.RClientGraphic;
import de.walware.rj.server.client.RClientGraphicFactory;
import de.walware.rj.services.RService;
import de.walware.rj.services.RServiceControlExtension;

import de.walware.ecommons.FastList;
import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.collections.ConstList;
import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.rj.eclient.graphics.IERGraphic;
import de.walware.rj.eclient.graphics.IERGraphicInstruction;
import de.walware.rj.eclient.graphics.LocatorCallback;
import de.walware.rj.eclient.graphics.RGraphics;
import de.walware.rj.eclient.graphics.comclient.IERClientGraphicActions;
import de.walware.rj.eclient.graphics.utils.CopyToDevRunnable;
import de.walware.rj.eclient.internal.graphics.FontManager.FontFamily;


/**
 * R graphic implementation of this plug-in. Implements R side API ({@link RClientGraphic})
 * as well as client side API ({@link IERGraphic}, {@link de.walware.rj.graphic.RGraphic}).
 */
public class EclipseRGraphic implements RClientGraphic, IERGraphic {
	
	
	private static final CharMapping ADBSYMBOL_MAPPING = new CachedMapping(
			new Unicode2AdbSymbolMapping() );
	
	private static final long MILLI_NANOS = 1000000L;
	
	private static final PaletteData DIRECT_PALETTE = new PaletteData(0xFF00, 0xFF0000, 0xFF000000);
	
	private static final LocatorCallback R_LOCATOR_CALLBACK = new LocatorCallback() {
		
		@Override
		public String getMessage() {
			return "→ Locate a point by mouse click (request from R)";
		}
		
		@Override
		public int located(final double x, final double y) {
			return NEXT;
		}
		
		@Override
		public void stopped(final String type) {
		}
		
	};
	
	
	private static void disposeElements(final IERGraphicInstruction[] instructions,
			final int beginIdx, final int endIdx) {
		try {
			for (int i = beginIdx; i < endIdx; i++) {
				switch (instructions[i].getInstructionType()) {
				case IERGraphicInstruction.DRAW_RASTER: {
					final Image image = ((RasterElement) instructions[i]).swtImage;
					if (image != null && !image.isDisposed()) {
						image.dispose();
					}
					continue; }
				case IERGraphicInstruction.DRAW_PATH: {
					final Path path = ((PathElement) instructions[i]).swtPath;
					if (path != null && !path.isDisposed()) {
						path.dispose();
					}
					continue; }
				default:
					continue;
				}
			}
		}
		catch (final SWTException e) {
			if (e.code != SWT.ERROR_DEVICE_DISPOSED) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID,
						"An error occurred when disposing SWT resources.", e ));
			}
		}
	}
	
	private static class DisposeRunnable implements Runnable {
		
		private final IERGraphicInstruction[] fInstructions;
		private final int fSize;
		
		private boolean fDelay;
		
		public DisposeRunnable(final IERGraphicInstruction[] instructions, final int size) {
			fInstructions = instructions;
			fSize = size;
		}
		
		public void run() {
			if (fDelay) {
				fDelay = false;
				Display.getCurrent().timerExec(5000, this);
			}
			
			disposeElements(fInstructions, 0, fSize);
		}
		
	}
	
	
	private final int fDevId;
	
	private int fCanvasColor;
	
	private int fDrawingStopDelay = 33; // ms after stop
	private int fDrawingForceDelay = 333; // ms after last update
	
	private final EclipseRGraphicFactory fManager;
	private boolean fIsRClosed;
	private boolean fIsLocalClosed;
	
	private final Object fStateLock = new Object();
	private boolean fIsActive;
	private boolean fIsActiveNotified;
	private int fMode = 1;
	private int fModeNotified;
	private long fDrawingStoppedStamp;
	private long fInstructionsNotifiedStamp;
	private boolean fStateNotificationDirectScheduled;
	private boolean fStateNotificationDelayedScheduled;
	private final Runnable fStateNotificationRunnable = new Runnable() {
		public void run() {
			int type = 0;
			Runnable runnable = null;
			try {
				while (true) {
					boolean reset = false;
					List<IERGraphicInstruction> update = null;
					synchronized (fStateLock) {
						if (type == 0
								&& !fStateNotificationDirectScheduled
								&& fStateNotificationDelayedScheduled) {
							fStateNotificationDirectScheduled = true;
							fStateNotificationDelayedScheduled = false;
						}
						if (fIsActive != fIsActiveNotified) {
							fIsActiveNotified = fIsActive;
							type = (fIsActive) ? 1 : 2;
						}
						else if ((fMode == 1 || fInstructionsUpdateSize > 0)
								&& fModeNotified != 1 ) {
							// start
							fModeNotified = 1;
							type = 3;
						}
						else if (fInstructionsUpdateSize > 0) {
							// update
							final long stamp = System.nanoTime();
							int t = fDrawingForceDelay - (int) ((stamp - fInstructionsNotifiedStamp) / MILLI_NANOS);
							if (t > 10 && fMode != 1) {
								t = Math.min(t, fDrawingStopDelay - (int) ((stamp - fDrawingStoppedStamp) / MILLI_NANOS));
							}
							if (t <= 10) {
								reset = (fInstructionsUpdateStart == 0);
								
								synchronized (fInstructionsLock) {
									if (reset && fInstructionsSize > 0) {
										runnable = new DisposeRunnable(fInstructions, fInstructionsSize);
									}
									fInstructions = fInstructionsUpdate;
									fInstructionsSize = fInstructionsUpdateStart + fInstructionsUpdateSize;
								}
								update = new ConstList<IERGraphicInstruction>(fInstructionsUpdate)
										.subList(fInstructionsUpdateStart, fInstructionsUpdateStart + fInstructionsUpdateSize);
//								System.out.println("InstrUpdate: \treset= " + reset + " \tcount= " + update.size() + " \ttdiff= " + ((stamp - fInstructionsNotifiedStamp) / MILLI_NANOS));
								fInstructionsUpdateStart = fInstructionsSize;
								fInstructionsUpdateSize = 0;
								fInstructionsNotifiedStamp = stamp;
								type = 5;
							}
							else {
								if (!fStateNotificationDelayedScheduled) {
									fStateNotificationDelayedScheduled = true;
									fDisplay.timerExec(10 + Math.min(t, fDrawingStopDelay), this);
								}
								fStateNotificationDirectScheduled = false;
								type = 0;
								return;
							}
						}
						else if (fMode != 1 && fModeNotified == 1 ) {
							// stop
							final long stamp = System.nanoTime();
							final int t = fDrawingStopDelay - (int) ((stamp - fDrawingStoppedStamp) / MILLI_NANOS);
							if (t <= 10) {
								fModeNotified = 0;
								type = 4;
							}
							else {
								if (!fStateNotificationDelayedScheduled) {
									fStateNotificationDelayedScheduled = true;
									fDisplay.timerExec(10 + t, this);
								}
								fStateNotificationDirectScheduled = false;
								type = 0;
								return;
							}
						}
						else {
							// done
							fStateNotificationDirectScheduled = false;
							type = 0;
							return;
						}
					}
					
					final Listener[] listeners = fGraphicListeners.toArray();
					for (final Listener listener : listeners) {
						switch (type) {
						case 1:
							listener.activated();
							continue;
						case 2:
							listener.deactivated();
							continue;
						case 3:
							listener.drawingStarted();
							continue;
						case 4:
							listener.drawingStopped();
							continue;
						case 5:
							if (listener instanceof ListenerInstructionsExtension) {
								((ListenerInstructionsExtension) listener).instructionsChanged(reset, update);
							}
//							System.out.println("InstrNotif: ns= " + (System.nanoTime() - fInstructionsNotifiedStamp));
							continue;
						}
					}
				}
			}
			finally {
				if (type != 0) {
					synchronized (fStateLock) {
						fStateNotificationDirectScheduled = false;
					}
				}
				if (runnable != null) {
					execInDisplay(runnable);
				}
			}
		}
	};
	
	
	private final int fOptions;
	private final IERClientGraphicActions fActions;
	private volatile double[] fNextSize;
	private double[] fSize;
	
	private FontFamily fCurrentFontFamily;
	private int fCurrentFontSize;
	private int fCurrentFontStyle;
	private int fCurrentFontRFace;
	private CharMapping fCurrentFontMapping;
	
	private String fLastStringEnc;
	private double[] fLastStringWidth;
	
	private final Display fDisplay;
	private boolean fIsDisposed;
	private final FontManager fSWTFontManager;
	private final ColorManager fSWTColorManager;
	
	private String fSerifFontName;
	private String fSansFontName;
	private String fMonoFontName;
	private String fSymbolFontName;
	private CharMapping fSymbolFontMapping;
	
	/** List of newly added instructions */
	private IERGraphicInstruction[] fInstructionsNew = new IERGraphicInstruction[1]; // initial init
	/** Count of newly added instructions in {@link #fInstructionsNew} */
	private int fInstructionsNewSize;
	/** Current list of instructions, notified + not yet notified */
	private IERGraphicInstruction[] fInstructionsUpdate;
	/** Index of first not yet notified instruction in {@link #fInstructionsUpdate} */
	private int fInstructionsUpdateStart;
	/** Count of not yet notified instructions in {@link #fInstructionsUpdate} */
	private int fInstructionsUpdateSize;
	/** Lock for {@link #fInstructions} and {@link #fInstructionsSize} */
	private final Object fInstructionsLock = new Object();
	/** List of notified instructions */
	private IERGraphicInstruction[] fInstructions;
	/** Count of notified instructions in {@link #fInstructions} */
	private int fInstructionsSize;
	
	private final Object fUserExchangeLock = new Object();
	private String fUserExchangeRType;
	private RServiceControlExtension fUserExchangeRCallback;
	
	private volatile LocatorCallback fLocatorCallback; // != null => started
	private LocatorCallback fLocatorNotified;
	private IStatus fLocatorMessage;
	private Collection<String> fLocatorStopTypes = Collections.emptySet();
	private double[] fLocatorLocationValue; // only used for R
	private final Object fLocatorAnswerLock = new Object(); // pipe for answers
	
	private final FastList<IERGraphic.Listener> fGraphicListeners = new FastList<IERGraphic.Listener>(IERGraphic.Listener.class, FastList.IDENTITY);
	
	private IStatus fMessage = Status.OK_STATUS;
	private final FastList<IStatusChangeListener> fMessageListeners = new FastList<IStatusChangeListener>(IStatusChangeListener.class, FastList.IDENTITY);
	
	private boolean fLocatorNotificationDirectScheduled;
	private boolean fLocatorNotificationDeferredScheduled;
	private long fLocatorDeferredStamp;
	private final Runnable fLocatorNotificationRunnable = new Runnable() {
		public void run() {
			int type = 0;
			try {
				while (true) {
					synchronized (fUserExchangeLock) {
						if (type == 0
								&& !fLocatorNotificationDirectScheduled
								&& fLocatorNotificationDeferredScheduled) {
							fLocatorNotificationDirectScheduled = true;
							fLocatorNotificationDeferredScheduled = false;
						}
						if (fLocatorCallback != null && fLocatorNotified == null
								&& fLocatorDeferredStamp == Long.MIN_VALUE) {
							fLocatorNotified = fLocatorCallback;
							type = 1;
						}
						else if (fLocatorNotified != null
								&& (fLocatorCallback != fLocatorNotified || fLocatorCallback == null) ){
							fLocatorNotified = null;
							type = 2;
						}
						else if (fLocatorDeferredStamp != Long.MIN_VALUE
								&& fLocatorCallback != null) {
							final int t = (int) ((fLocatorDeferredStamp - System.nanoTime()) / 1000000);
							if (t <= 10) {
								internalStopLocator(false);
								type = 3;
								continue;
							}
							else {
								if (!fLocatorNotificationDeferredScheduled) {
									fLocatorNotificationDeferredScheduled = true;
									fDisplay.timerExec(t + 10, this);
								}
								fLocatorNotificationDirectScheduled = false;
								type = 0;
								return;
							}
						}
						else {
							fLocatorNotificationDirectScheduled = false;
							type = 0;
							return;
						}
					}
					
					final Listener[] listeners = fGraphicListeners.toArray();
					for (final Listener listener : listeners) {
						if (listener instanceof ListenerLocatorExtension) {
							switch (type) {
							case 1:
								((ListenerLocatorExtension) listener).locatorStarted();
								continue;
							case 2:
								((ListenerLocatorExtension) listener).locatorStopped();
								continue;
							}
						}
					}
					updateMessage();
				}
			}
			finally {
				if (type != 0) {
					synchronized (fUserExchangeLock) {
						fLocatorNotificationDirectScheduled = false;
					}
				}
			}
		}
	};
	
	
	public EclipseRGraphic(final int devId, final double w, final double h, final InitConfig config,
			final boolean active, final IERClientGraphicActions actions, final int options,
			final EclipseRGraphicFactory manager) {
		fDevId = devId;
		fIsActive = active;
		fActions = actions;
		fManager = manager;
		fOptions = options;
		
		fDisplay = UIAccess.getDisplay();
		fSWTFontManager = manager.getFontManager(fDisplay);
//		fFontManager = new FontManager(fDisplay); // -> dispose!
		fSWTColorManager = manager.getColorManager(fDisplay);
		
		fSize = fNextSize = new double[] { w, h };
		initPanel(w, h, config);
	}
	
	
	public String getLabel() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Device ");
		sb.append(fDevId + 1);
		if (fActions != null) {
			final String rLabel = fActions.getRLabel();
			if (rLabel != null && rLabel.length() > 0) {
				sb.append(" │ ");
				sb.append(rLabel);
			}
		}
		final boolean locator = isLocatorStarted();
		if (fIsActive || locator) {
			sb.append(" \t<"); //$NON-NLS-1$
			if (fIsActive) {
				sb.append("active+"); //$NON-NLS-1$
			}
			if (locator) {
				sb.append("locator+"); //$NON-NLS-1$
			}
			sb.replace(sb.length()-1, sb.length(), ">"); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	private void add(final IERGraphicInstruction instr) {
		// adding is always in R thread
		if (fInstructionsNew == null) {
			fInstructionsNew = new IERGraphicInstruction[512];
		}
		else if (fInstructionsNewSize >= fInstructionsNew.length) {
			final IERGraphicInstruction[] newArray = new IERGraphicInstruction[fInstructionsNewSize + 512];
//			System.out.println("NewArray " + fInstructionsNewSize + " -> " + newArray.length);
			System.arraycopy(fInstructionsNew, 0, newArray, 0, fInstructionsNewSize);
			fInstructionsNew = newArray;
		}
		fInstructionsNew[fInstructionsNewSize] = instr;
		fInstructionsNewSize++;
	}
	
	protected void initPanel(final double w, final double h, final InitConfig config) {
		fDrawingStopDelay = 33;
		fDrawingForceDelay = 333;
		
		fCurrentFontFamily = null;
		fCurrentFontMapping = null;
		
		final IPreferencesService preferences = Platform.getPreferencesService();
		fSerifFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY, "", null);
		fSansFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SANS_FONTNAME_KEY, "", null);
		fMonoFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_MONO_FONTNAME_KEY, "", null);
		if (preferences.getBoolean(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SYMBOL_USE_KEY, true, null)) {
			fSymbolFontName = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SYMBOL_FONTNAME_KEY, "Symbol", null); //$NON-NLS-1$
			final String encoding = preferences.getString(RGraphics.PREF_FONTS_QUALIFIER, RGraphics.PREF_FONTS_SYMBOL_ENCODING_KEY, "AdobeSymbol", null); //$NON-NLS-1$
			if ("AdobeSymbol".equals(encoding)) { //$NON-NLS-1$
				fSymbolFontMapping = ADBSYMBOL_MAPPING;
			}
			else {
				fSymbolFontMapping = null;
			}
		}
		else {
			fSymbolFontName = null;
			fSymbolFontMapping = null;
		}
		fCanvasColor = (config.canvasColor & 0xffffff);
		
		add(new GraphicInitialization(w, h, fCanvasColor, fSWTColorManager.getColor(fCanvasColor)));
	}
	
	public void reset(final double w, final double h, final InitConfig config) {
		synchronized (fStateLock) {
			internalReset();
		}
		
		initPanel(w, h, config);
	}
	
	private void internalReset() {
		if (fInstructionsNew != null) {
			if (fInstructionsNewSize > 0) {
				disposeElements(fInstructionsNew, 0, fInstructionsNewSize);
			}
			fInstructionsNew = null;
			fInstructionsNewSize = 0;
		}
		if (fInstructionsUpdate != null) {
			if (fInstructionsUpdateSize > 0) {
				disposeElements(fInstructionsUpdate, fInstructionsUpdateStart, fInstructionsUpdateStart+fInstructionsUpdateSize);
			}
			fInstructionsUpdate = null;
			fInstructionsUpdateStart = 0;
			fInstructionsUpdateSize = 0;
		}
		fDrawingStoppedStamp = System.nanoTime();
		fInstructionsNotifiedStamp = fDrawingStoppedStamp - 1000 * MILLI_NANOS;
	}
	
	private void execInDisplay(final Runnable runnable) {
		try {
			fDisplay.asyncExec(runnable);
		}
		catch (final SWTException e) {
			if (e.code != SWT.ERROR_DEVICE_DISPOSED) {
				throw e;
			}
		}
	}
	
	public int getDevId() {
		return fDevId;
	}
	
	public void setActive(final boolean active) {
		if (fIsActive == active) {
			return;
		}
		synchronized (fStateLock) {
			fIsActive = active;
			if (fIsDisposed) {
				return;
			}
			if (!fStateNotificationDirectScheduled) {
				fStateNotificationDirectScheduled = true;
				execInDisplay(fStateNotificationRunnable);
			}
		}
	}
	
	public boolean isActive() {
		return fIsActive;
	}
	
	public void setMode(final int mode) {
		synchronized (fStateLock) {
			if (fMode == mode) {
				return;
			}
			if (mode != 1) {
				fDrawingStoppedStamp = System.nanoTime();
				if (fInstructionsNewSize > 0) {
					if (fInstructionsUpdate == null) {
						fInstructionsUpdate = fInstructionsNew;
						fInstructionsUpdateStart = 0;
						fInstructionsUpdateSize = fInstructionsNewSize;
						fInstructionsNew = null;
						fInstructionsNewSize = 0;
					}
					else {
						final int newSize = fInstructionsUpdateStart + fInstructionsUpdateSize + fInstructionsNewSize;
						if (newSize > fInstructionsUpdate.length) {
							final IERGraphicInstruction[] newArray = new IERGraphicInstruction[newSize + 512];
							System.arraycopy(fInstructionsUpdate, 0, newArray, 0, fInstructionsUpdateStart + fInstructionsUpdateSize);
							fInstructionsUpdate = newArray;
						}
						System.arraycopy(fInstructionsNew, 0, fInstructionsUpdate, fInstructionsUpdateStart + fInstructionsUpdateSize, fInstructionsNewSize);
						fInstructionsUpdateSize += fInstructionsNewSize;
						fInstructionsNewSize = 0;
					}
				}
			}
			fMode = mode;
			if (fIsDisposed) {
				return;
			}
			if (mode == 1 && fModeNotified != 1 // need start
					&& !fStateNotificationDirectScheduled ) {
				fStateNotificationDirectScheduled = true;
				execInDisplay(fStateNotificationRunnable);
			}
			else if (mode != 1 // stop
					&& !(fStateNotificationDirectScheduled || fStateNotificationDelayedScheduled) ) {
				fStateNotificationDirectScheduled = true;
				execInDisplay(fStateNotificationRunnable);
			}
		}
	}
	
	public double[] computeSize() {
		return fSize;
	}
	
	
	protected void printFont() {
		System.out.println(fCurrentFontFamily.fName + " " + fCurrentFontStyle + " " + fCurrentFontSize);
	}
	
	public double[] computeFontMetric(final int ch) {
//		System.out.println("==\nTextMetrics: \"" + ((char) ch) + "\" (" + ch + ")"); printFont();
		return fCurrentFontFamily.getCharMetrics(fCurrentFontStyle, fCurrentFontSize,
				(fCurrentFontMapping != null) ? fCurrentFontMapping.encode(ch) : ch );
	}
	
	public double[] computeStringWidth(final String txt) {
//		System.out.println("==\nTextWidth: \"" + txt + "\""); printFont();
		return computeStringWidthEnc((fCurrentFontMapping != null) ? fCurrentFontMapping.encode(txt) : txt);
	}
	
	protected final double[] computeStringWidthEnc(final String text) {
		if (text.equals(fLastStringEnc)) {
			return fLastStringWidth;
		}
		
		final double[] answer = fCurrentFontFamily.getStringWidth(fCurrentFontStyle, fCurrentFontSize, text);
		
		fLastStringEnc = text;
		return (fLastStringWidth = answer);
	}
	
	
	public void addSetClip(final double x0, final double y0, final double x1, final double y1) {
		final ClipSetting instr = new ClipSetting(x0, y0, x1, y1);
		add(instr);
	}
	
	public void addSetColor(final int color) {
		final ColorSetting instr = new ColorSetting(color,
				fSWTColorManager.getColor((color & 0xffffff)) );
		add(instr);
	}
	
	public void addSetFill(final int color) {
		final FillSetting instr = new FillSetting(color,
				fSWTColorManager.getColor((color & 0xffffff)) );
		add(instr);
	}
	
	public void addSetFont(String family, final int face, final double pointSize,
			final double lineHeight) {
//		System.out.println("==\nSetFont: \"" + family + "\" " + face + " " + pointSize + " (cex= " + cex + ")");
		switch (face) {
		case 2:
		case 3:
		case 4:
			family = getFontName(family);
			fCurrentFontStyle = face - 1;
			fCurrentFontMapping = null;
			break;
		case 5:
			if (fSymbolFontName != null) {
				family = fSymbolFontName;
				fCurrentFontStyle = 0;
				fCurrentFontMapping = fSymbolFontMapping;
				break;
			}
			//$FALL-THROUGH$
		default:
			family = getFontName(family);
			fCurrentFontStyle = 0;
			fCurrentFontMapping = null;
			break;
		}
		fCurrentFontFamily = fSWTFontManager.getFamily(family);
		fCurrentFontRFace = face;
		fCurrentFontSize = (int) (pointSize + 0.5);
		
		fLastStringEnc = null;
		
		final FontSetting instr = new FontSetting(family, face, pointSize, lineHeight,
				fCurrentFontFamily.getSWTFont(fCurrentFontStyle, fCurrentFontSize),
				fCurrentFontFamily.getSWTFontProperties(fCurrentFontStyle, fCurrentFontSize) );
		add(instr);
	}
	
	private String getFontName(final String family) {
		if (family.length() == 0 || family.equals("sansserif")) {
			return fSansFontName;
		}
		else if (family.equals("serif")) {
			return fSerifFontName;
		}
		else if (family.equals("mono")) {
			return fMonoFontName;
		}
		else {
			return family;
		}
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
	
	public void addDrawPath(final int[] n, final double[] x, final double[] y, final int winding) {
		final Path path = new Path(fDisplay);
		int k = 0, end = 0;
		for (int i = 0; i < n.length; i++) {
			end += n[i];
			path.moveTo((float) Math.floor(x[k] + 0.5), (float) Math.floor(y[k++] + 0.5));
			while (k < end) {
				path.lineTo((float) Math.floor(x[k] + 0.5), (float) Math.floor(y[k++] + 0.5));
			}
			path.close();
		}
		final PathElement instr = new PathElement(n, x, y, winding, path);
		add(instr);
	}
	
	public void addDrawCircle(final double x, final double y, final double r) {
		final CircleElement instr = new CircleElement(x, y, r);
		add(instr);
	}
	
	public void addDrawText(final String txt,
			final double x, final double y, final double rDeg, final double hAdj) {
//		System.out.println("==\nDrawText: " + x + ", " + y + " " + hAdj + " \"" + txt + "\""); printFont();
		final String text = (fCurrentFontMapping != null) ? fCurrentFontMapping.encode(txt) : txt;
		final TextElement instr = new TextElement(text, x, y, rDeg, hAdj,
				(hAdj != 0) ? computeStringWidthEnc(text)[0] : 0);
		add(instr);
	}
	
	public void addDrawRaster(final byte[] imgData, final boolean hasAlpha,
			final int imgWidth, final int imgHeight,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate) {
		final ImageData imageData = new ImageData(imgWidth, imgHeight, 32, DIRECT_PALETTE, 4, imgData);
		if (hasAlpha) {
			final byte[] alpha = new byte[imgWidth*imgHeight];
			for (int i = 0; i < alpha.length; i++) {
				alpha[i] = imgData[i*4 + 3];
			}
			imageData.alphaData = alpha;
		}
		final Image swtImage = new Image(fDisplay, imageData);
		final RasterElement instr = new RasterElement(imgData, imgWidth, imgHeight, x, y, w, h,
				rDeg, interpolate, swtImage );
		add(instr);
	}
	
	
	protected void waitRUserExchange(final String type, final RService r, final IProgressMonitor monitor,
			final Callable<Boolean> cancelListener) {
		final RServiceControlExtension rControl = (r instanceof RServiceControlExtension) ?
				(RServiceControlExtension) r : null;
		if (rControl != null && cancelListener != null) {
			rControl.addCancelHandler(cancelListener);
			rControl.getWaitLock().lock();
		}
		try {
			while (true) {
				synchronized (fUserExchangeLock) {
					if (fUserExchangeRType != type) {
						fUserExchangeRCallback = null;
						return;
					}
					if (fIsLocalClosed || fIsRClosed || monitor.isCanceled() ) {
						fUserExchangeRType = null;
						fUserExchangeRCallback = null;
						return;
					}
					fUserExchangeRCallback = rControl;
				}
				
				if (rControl != null) {
					rControl.waitingForUser(monitor);
				}
				else {
					try {
						Thread.sleep(50);
					}
					catch (final InterruptedException e) {
					}
				}
			}
		}
		finally {
			if (rControl != null && cancelListener != null) {
				rControl.getWaitLock().unlock();
				rControl.removeCancelHandler(cancelListener);
			}
		}
	}
	
	
	private void internalStartLocator(final LocatorCallback callback) {
		fLocatorCallback = callback;
		fLocatorMessage = new Status(IStatus.INFO, RGraphics.PLUGIN_ID, callback.getMessage());
		fLocatorStopTypes = callback.getStopTypes();
		fLocatorDeferredStamp = Long.MIN_VALUE;
		
		if (fDisplay.isDisposed()) {
			return;
		}
		if (!fLocatorNotificationDirectScheduled) {
			execInDisplay(fLocatorNotificationRunnable);
		}
	}
	
	private void internalStopLocator(final boolean deferred) {
		if (deferred) {
			fLocatorDeferredStamp = System.nanoTime() + 500 * MILLI_NANOS;
			if (fDisplay.isDisposed()) {
				return;
			}
			if (!(fLocatorNotificationDirectScheduled || fLocatorNotificationDeferredScheduled)) {
				fLocatorNotificationDirectScheduled = true;
				execInDisplay(fLocatorNotificationRunnable);
			}
			return;
		}
		
		fLocatorCallback = null;
		fLocatorMessage = null;
		fLocatorStopTypes = Collections.emptySet();
		fLocatorDeferredStamp = Long.MIN_VALUE;
		
		if (fDisplay.isDisposed()) {
			return;
		}
		if (!fLocatorNotificationDirectScheduled) {
			fLocatorNotificationDirectScheduled = true;
			execInDisplay(fLocatorNotificationRunnable);
		}
	}
	
	public double[] runRLocator(final RService r, final IProgressMonitor monitor) {
		synchronized (fUserExchangeLock) {
			if (fLocatorCallback != null && fLocatorCallback != R_LOCATOR_CALLBACK) {
				return null;
			}
			fUserExchangeRType = "locator";
			internalStartLocator(R_LOCATOR_CALLBACK);
			
			fLocatorLocationValue = null;
		}
		waitRUserExchange("locator", r, monitor, new Callable<Boolean>() {
			public Boolean call() {
				return Boolean.valueOf(answerLocator(null, null, true));
			}
		});
		final double[] value;
		synchronized (fUserExchangeLock) {
			value = fLocatorLocationValue;
			if (fUserExchangeRType == "locator") {
				fUserExchangeRType = null;
			}
			// avoid flickering as well as stale locators
			internalStopLocator(value != null);
		}
		return value;
	}
	
	public IStatus startLocalLocator(final LocatorCallback callback) {
		if (callback == null) {
			throw new NullPointerException("callback"); //$NON-NLS-1$
		}
		synchronized (fUserExchangeLock) {
			if (fLocatorCallback != null && fLocatorCallback != callback) {
				return new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, "Another locator is already started.");
			}
			internalStartLocator(callback);
		}
		return Status.OK_STATUS;
	}
	
	public boolean isLocatorStarted() {
		return (fLocatorCallback != null);
	}
	
	public Collection<String> getLocatorStopTypes() {
		return fLocatorStopTypes;
	}
	
	public void returnLocator(final double x, final double y) {
		answerLocator(null, new double[] { x, y }, false);
	}
	
	public void stopLocator(final String type) {
		answerLocator(type, null, false);
	}
	
	private boolean answerLocator(final String type, final double[] xy,
			final boolean onlyR) {
		synchronized (fLocatorAnswerLock) {
			RServiceControlExtension rControl = null;
			LocatorCallback callback;
			synchronized (fUserExchangeLock) {
				if (fLocatorCallback == null || fLocatorDeferredStamp != Long.MIN_VALUE) {
					return false;
				}
				if (fLocatorCallback == R_LOCATOR_CALLBACK) {
					if (fUserExchangeRType == "locator") { //$NON-NLS-1$
						fUserExchangeRType = null;
						rControl = fUserExchangeRCallback;
					}
					else {
						return false;
					}
				}
				else if (onlyR) {
					return false;
				}
				if (xy == null && type != null && !fLocatorStopTypes.contains(type)) {
					return false;
				}
				fLocatorLocationValue = xy;
				callback = fLocatorCallback;
			}
			
			final int code;
			if (callback == R_LOCATOR_CALLBACK) {
				if (xy != null) {
					code = -1;
				}
				else {
					code = LocatorCallback.STOP;
				}
				if (rControl != null) {
					rControl.getWaitLock().lock();
					try {
						rControl.resume();
					}
					finally {
						rControl.getWaitLock().unlock();
					}
				}
			}
			else {
				if (xy != null) {
					code = callback.located(xy[0], xy[1]);
				}
				else {
					code = LocatorCallback.STOP;
					callback.stopped(type);
				}
			}
			synchronized (fUserExchangeLock) {
				if (code == LocatorCallback.NEXT) {
					internalStartLocator(callback);
				}
				else {
					internalStopLocator((code == -1));
				}
			}
			return true;
		}
	}
	
	
	public void closeFromR() {
		fIsRClosed = true;
		if (fIsLocalClosed
				|| (fOptions & RClientGraphicFactory.R_CLOSE_OFF) == 0) {
			dispose();
		}
		answerLocator(null, null, true);
		setActive(false);
	}
	
	protected void dispose() {
		fGraphicListeners.clear();
		Runnable runnable = null;
		synchronized (fStateLock) {
			if (!fIsDisposed) {
				fIsDisposed = true;
				internalReset();
				synchronized (fInstructionsLock) {
					if (fInstructionsSize > 0) {
						runnable = new DisposeRunnable(fInstructions, fInstructionsSize);
					}
					fInstructionsSize = 0;
					fInstructions = null;
				}
			}
		}
		if (runnable != null) {
			execInDisplay(runnable);
		}
	}
	
	
	public List<IERGraphicInstruction> getInstructions() {
		synchronized (fInstructionsLock) {
			return (fInstructionsSize > 0) ?
					new ConstList<IERGraphicInstruction>(fInstructions).subList(0, fInstructionsSize) :
					new ConstList<IERGraphicInstruction>();
		}
	}
	
	public ITool getRHandle() {
		if (fActions != null) {
			return fActions.getRHandle();
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
			answerLocator(null, null, false);
			return fActions.closeGraphic(fDevId);
		}
		else {
			fIsLocalClosed = true;
			answerLocator(null, null, false);
			fManager.close(this);
			dispose();
		}
		return null;
	}
	
	public void addListener(final Listener listener) {
		fGraphicListeners.add(listener);
	}
	
	public void removeListener(final Listener listener) {
		fGraphicListeners.remove(listener);
	}
	
	protected void updateMessage() {
		IStatus message;
		if (fLocatorMessage != null) {
			message = fLocatorMessage;
		}
		else {
			message = Status.OK_STATUS;
		}
		if (!fMessage.equals(message)) {
			fMessage = message;
			final IStatusChangeListener[] listeners = fMessageListeners.toArray();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].statusChanged(message);
			}
		}
	}
	
	public IStatus getMessage() {
		return fMessage;
	}
	
	public void addMessageListener(final IStatusChangeListener listener) {
		fMessageListeners.add(listener);
	}
	
	public void removeMessageListener(final IStatusChangeListener listener) {
		fMessageListeners.remove(listener);
	}
	
	
	protected void preAction() throws CoreException {
		if (fActions == null || fActions.getRHandle() == null) {
			throw new UnsupportedOperationException();
		}
		if (fIsRClosed) {
			throw new CoreException(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID,
					"The R graphic device is already closed." ));
		}
	}
	
	@Deprecated
	public IStatus copy(final String toDev, final String toDevFile, final String toDevArgs)
			throws CoreException {
		preAction();
		return fActions.getRHandle().getQueue().add(new CopyToDevRunnable(this,
				toDev, toDevFile, toDevArgs ));
	}
	
	public void copy(final String toDev, final String toDevFile, final String toDevArgs,
			final IProgressMonitor monitor) throws CoreException {
		preAction();
		fActions.copy(fDevId, toDev, toDevFile, toDevArgs, monitor);
	}
	
	public double[] convertGraphic2User(final double[] xy,
			final IProgressMonitor monitor) throws CoreException {
		preAction();
		return fActions.convertGraphic2User(fDevId, xy, monitor);
	}
	
	public double[] convertUser2Graphic(final double[] xy,
			final IProgressMonitor monitor) throws CoreException {
		preAction();
		return fActions.convertUser2Graphic(fDevId, xy, monitor);
	}
	
}
