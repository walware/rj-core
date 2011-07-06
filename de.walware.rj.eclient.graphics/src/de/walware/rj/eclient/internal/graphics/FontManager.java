/*******************************************************************************
 * Copyright (c) 2011 WalWare/RJ-Project (www.walware.de/goto/opensource).
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
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;


public class FontManager {
	
	private static final int[] R2SWT_STYLE = new int[] {
			SWT.NONE,
			SWT.BOLD,
			SWT.ITALIC,
			SWT.BOLD | SWT.ITALIC,
	};
	private static final int R_STYLE_COUNT = 4;
	
	
	private static final int HR_FONTSIZE = 60;
	private static final double HR_FACTOR = HR_FONTSIZE;
	private static final int HR_MIN_FONTSIZE = HR_FONTSIZE / 2;
	
	
	private static final class FontInstance {
		
		private static final double[] UNINITIALIZED = new double[0];
		
		public final int size;
		public final Font swtFont;
		
		private double[] fSWTFontProperties; // [ baselineOffset ] -> FontSetting
		
		private int fBaseLine;
		private double fMetricTolerance = 0;
		private double fAscentUp = Integer.MIN_VALUE;
		private double fAscentLow = Integer.MIN_VALUE;
		
		private int[] fCharAbove255;
		private double[] fCharMetrics = UNINITIALIZED;
		private double[] fStrWidth = UNINITIALIZED;
		
		
		public FontInstance(final int size, final Font font) {
			this.size = size;
			this.swtFont = font;
		}
		
		
		public void init(final GC gc) {
			final FontMetrics fontMetrics = gc.getFontMetrics();
			fBaseLine = fontMetrics.getLeading() + fontMetrics.getAscent();
			fMetricTolerance = fontMetrics.getAscent() * 0.05;
			
			fSWTFontProperties = new double[] {
					fBaseLine,
			};
		}
		
		public int checkChar(final int ch) {
			if (ch <= 0) {
				return 0;
			}
			else if (ch <= 255) {
				return ch;
			}
			else if (fCharAbove255 == null){
				fCharAbove255 = new int[256];
				fCharAbove255[0] = ch;
				return 256 + 0;
			}
			else {
				for (int i = 0; i < fCharAbove255.length; i++) {
					final int c = fCharAbove255[i];
					if (c == 0) {
						fCharAbove255[i] = c;
						return 256 + i;
					}
					else if (c == ch) {
						return 256 + i;
					}
				}
				final int[] newChar = new int[fCharAbove255.length + 256];
				System.arraycopy(fCharAbove255, 0, newChar, 0, fCharAbove255.length);
				newChar[fCharAbove255.length] = ch;
				fCharAbove255 = newChar;
				return fCharAbove255.length - 256;
			}
		}
		
	}
	
	
	private static final class TestGC {
		
		
		private final GC fGC;
		
		private final Image fImage;
		private final int fImageWidth;
		private final int fImageHeigth;
		private final int fImagePixelBytes;
		private final int fImageLineBytes;
		private final byte fImageBlankData;
		
		private ImageData fImageData;
		
		private FontInstance fGCFont;
		
		
		public TestGC(final Device device) {
			final GC tempGC = new GC(device);
			final Font tempFont = new Font(device, new FontData(device.getSystemFont().getFontData()[0].getName(), HR_FONTSIZE, 0));
			tempGC.setFont(tempFont);
			fImageHeigth = (int) (tempGC.getFontMetrics().getHeight() * 1.5);
			fImageWidth = fImageHeigth * 2;
			tempGC.dispose();
			tempFont.dispose();
			
			fImage = new Image(device, fImageWidth, fImageHeigth);
			
			fGC = new GC(fImage);
			fGC.setAdvanced(true);
			fGC.setAntialias(SWT.ON);
			fGC.setTextAntialias(SWT.ON);
			fGC.setInterpolation(SWT.HIGH);
			fGC.setAlpha(255);
			fGC.setBackground(device.getSystemColor(SWT.COLOR_WHITE));
			fGC.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			fGC.setFont(null);
			fGCFont = null;
			
			clearImage();
			fImageData = fImage.getImageData();
			fImageLineBytes = fImageData.bytesPerLine;
			fImagePixelBytes = Math.max(fImageData.bytesPerLine / fImageData.width, 1);
			fImageBlankData = fImageData.data[0];
		}
		
		
		public void setFont(final FontInstance font) {
//			if (fGCFont != font) { // E-Bug #319125
				fGCFont = font;
				fGC.setFont(font.swtFont);
//			}
			if (fGCFont.fBaseLine == 0) {
				fGCFont.init(fGC);
				fGC.setFont(font.swtFont); // E-Bug #319125
			}
		}
		
		public FontInstance getFont() {
			return fGCFont;
		}
		
		public int getStringWidth(final String txt) {
			return fGC.stringExtent(txt).x;
		}
		
		public void clearImage() {
			fGC.fillRectangle(0, 0, fImageWidth, fImageHeigth);
			fImageData = null;
		}
		
		private void drawText(final String txt) {
			fGC.drawString(txt, 0, 0, true);
		}
		
		public int findFirstLine() {
			if (fImageData == null) {
				fImageData = fImage.getImageData();
			}
			final byte[] data = fImageData.data;
			for (int i = 0; i < data.length; i += fImagePixelBytes) {
				if (data[i] != fImageBlankData) {
					return (i / fImageLineBytes);
				}
			}
			return -1;
		}
		
		public int findLastLine() {
			if (fImageData == null) {
				fImageData = fImage.getImageData();
			}
			final byte[] data = fImageData.data;
			for (int i = data.length - fImagePixelBytes; i >= 0; i -= fImagePixelBytes) {
				if (data[i] != fImageBlankData) {
					return (i / fImageLineBytes);
				}
			}
			return -1;
		}
		
		
		public void dispose() {
			if (!fGC.isDisposed()) {
				fGC.dispose();
			}
			if (!fImage.isDisposed()) {
				fImage.dispose();
			}
		}
		
	}
	
	
	public final class FontFamily {
		
		private static final double POLISH_SMALL_MAX_FACTOR = 0.4;
		private static final double POLISH_SMALL_ADD_CORR = 0.04;
		private static final double POLISH_ADD_CONST = 0.05;
		private static final double POLISH_ADD_REL = 0.1;
		
		private static final int METRIC_IDX_ADVWIDTH = 0;
		private static final int METRIC_IDX_ASCENT = 1;
		private static final int METRIC_IDX_DESCENT = 2;
		private static final int METRIC_COUNT = 3;
		
		
		final String fName;
		
		final FontInstance[][] fFonts = new FontInstance[R_STYLE_COUNT][];
		
		
		private FontFamily(final String name) {
			fName = name;
		}
		
		
		private FontInstance get(final int style, final int size) {
			FontInstance[] styleFonts = fFonts[style];
			if (styleFonts == null) {
				fFonts[style] = styleFonts = new FontInstance[4];
			}
			int idx;
			if (size == HR_FONTSIZE) {
				idx = 0;
			}
			else {
				idx = styleFonts.length;
				for (int i = 1; i < styleFonts.length; i++) {
					if (styleFonts[i] != null) {
						if (styleFonts[i].size == size) {
							idx = i;
							break;
						}
					}
					else {
						idx = i;
						break;
					}
				}
				if (idx >= styleFonts.length) {
					fFonts[style] = new FontInstance[styleFonts.length+4];
					System.arraycopy(styleFonts, 0, fFonts[style], 0, styleFonts.length);
					styleFonts = fFonts[style];
				}
			}
			if (styleFonts[idx] == null) {
				final FontData fontData = new FontData(fName, size, R2SWT_STYLE[style]);
				styleFonts[idx] = new FontInstance(size,
						new Font(fDisplay, fontData));
			}
			return styleFonts[idx];
		}
		
		public synchronized Font getSWTFont(final int style, final int size) {
			return get(style, size).swtFont;
		}
		
		public synchronized double[] getCharMetrics(final int style, final int size, final int ch) {
			final FontInstance font = get(style, HR_FONTSIZE);
			final double factor = size / HR_FACTOR;
			final int chIdx = font.checkChar(ch) * METRIC_COUNT;
			
			final double[] answer = new double[3];
			
			if (chIdx >= font.fCharMetrics.length
					|| font.fCharMetrics[chIdx] == 0) {
				synchronized (getTestLock()) {
					final TestGC gc = getTestGC();
					gc.setFont(font);
					
					if (font.fCharMetrics.length == 0) {
						font.fCharMetrics = new double[Math.max(512 * METRIC_COUNT,
								chIdx + 128 * METRIC_COUNT)];
						
						font.fCharMetrics[' ' * METRIC_COUNT + METRIC_IDX_ADVWIDTH] =
								(gc.fGC.getAdvanceWidth('m') * 0.2 + gc.fGC.getAdvanceWidth(' ') * 1.0) / 2.0;
						font.fCharMetrics[' ' * METRIC_COUNT + METRIC_IDX_ASCENT] = 0;
						font.fCharMetrics[' ' * METRIC_COUNT + METRIC_IDX_DESCENT] = 0;
						
						font.fAscentUp = checkCharAscentMean(gc, new char[] { 'A', 'M', 'O', 'E' });
						font.fAscentLow = checkCharAscentMean(gc, new char[] { 'a', 'm', 'p', 'w' });
						
						font.fCharMetrics[0 * METRIC_COUNT + METRIC_IDX_ADVWIDTH] =
								font.fCharMetrics['M' * METRIC_COUNT + METRIC_IDX_ADVWIDTH];
						font.fCharMetrics[0 * METRIC_COUNT + METRIC_IDX_ASCENT] =
								(font.fAscentUp > 0) ? font.fAscentUp : 0;
						font.fCharMetrics[0 * METRIC_COUNT + METRIC_IDX_DESCENT] = 0;
					}
					else if (chIdx >= font.fCharMetrics.length) {
						final double[] newArray = new double[(chIdx + 128 * METRIC_COUNT)];
						System.arraycopy(font.fCharMetrics, 0, newArray, 0, font.fCharMetrics.length);
						font.fCharMetrics = newArray;
					}
					
					if (font.fCharMetrics[chIdx] == 0) {
						computeCharHeights(gc, ch, chIdx);
					}
					
//					Point sext = gc.stringExtent(new String(new int[] { fCurrentChar }, 0, 1));
//					Point text = gc.textExtent(new String(new int[] { fCurrentChar }, 0, 1), SWT.TRANSPARENT);
//					System.out.println("height= " + gc.getHeight());
//					System.out.println("leading= " + gc.getLeading());
//					System.out.println("ascent= " + gc.getAscent());
//					System.out.println("descent= " + gc.getDescent());
//					System.out.println("stringExtend.y= " + sext.y);
//					System.out.println("textExtend.y= " + text.y);
//					System.out.println("stringExtend.x= " + sext.x);
//					System.out.println("textExtend.x= " + text.x);
//					System.out.println("advanceWidth= " + gc.getAdvanceWidth((char) fCurrentChar));
//					System.out.println("charWidth= " + gc.getCharWidth((char) fCurrentChar));
//					System.out.println("averageCharWidth= " + gc.getFontMetrics.getAverageCharWidth());
//					System.out.println(fCurrentAnswer[2]);
				}
			}
			
			answer[0] = polish(font.fCharMetrics[chIdx + METRIC_IDX_ASCENT] + 1.01 / factor, factor);
			answer[1] = polish(font.fCharMetrics[chIdx + METRIC_IDX_DESCENT], factor);
			answer[2] = polish(font.fCharMetrics[chIdx + METRIC_IDX_ADVWIDTH], factor);
			
//			System.out.println("-> " + Arrays.toString(answer));
			return answer;
		}
		
		private void computeCharHeights(final TestGC gc, final int ch, final int idx) {
			final FontInstance font = gc.getFont();
			font.fCharMetrics[idx + METRIC_IDX_ADVWIDTH] =
					gc.fGC.getAdvanceWidth((char) ch);
			
			final String s = String.valueOf((char) ch);
			
			gc.clearImage();
			gc.drawText(s);
			final int firstLine = gc.findFirstLine();
			if (firstLine >= 0) {
				double ascent = font.fBaseLine - firstLine;
				if (Math.abs(ascent - font.fAscentUp) <= font.fMetricTolerance) {
					ascent = font.fAscentUp;
				}
				else if (Math.abs(ascent - font.fAscentLow) <= font.fMetricTolerance) {
					ascent = font.fAscentLow;
				}
				int descent = gc.findLastLine() - font.fBaseLine + 1;
				if (Math.abs(descent) <= font.fMetricTolerance) {
					descent = 0;
				}
				font.fCharMetrics[idx + METRIC_IDX_ASCENT] = ascent;
				font.fCharMetrics[idx + METRIC_IDX_DESCENT] = descent;
			}
			else {
				font.fCharMetrics[idx + METRIC_IDX_ASCENT] = 0;
				font.fCharMetrics[idx + METRIC_IDX_DESCENT] = 0;
			}
			
			if (idx / METRIC_COUNT < font.fStrWidth.length
					&& font.fStrWidth[idx / METRIC_COUNT] == 0) {
				font.fStrWidth[idx / METRIC_COUNT] = gc.getStringWidth(s);
			}
		}
		
		private double checkCharAscentMean(final TestGC gc, final char[] chars) {
			final FontInstance font = gc.getFont();
			double mean = 0;
			for (int i = 0; i < chars.length; i++) {
				computeCharHeights(gc, chars[i], chars[i] * METRIC_COUNT);
				mean += font.fCharMetrics[chars[i] * METRIC_COUNT + METRIC_IDX_ASCENT];
				gc.fGC.setFont(font.swtFont); // E-Bug #319125
			}
			mean /= chars.length;
			mean = polish(mean * 2.0, 1.0) / 2.0;
			for (int i = 0; i < chars.length; i++) {
				if (Math.abs(mean - font.fCharMetrics[chars[i] * METRIC_COUNT + METRIC_IDX_ASCENT]) > font.fMetricTolerance) {
					return Integer.MIN_VALUE;
				}
			}
			for (int i = 0; i < chars.length; i++) {
				font.fCharMetrics[chars[i] * METRIC_COUNT + METRIC_IDX_ASCENT] = mean;
			}
			return mean;
		}
		
		public synchronized double[] getStringWidth(final int style, final int size, final String text) {
			final FontInstance font;
			final double factor;
			final int chIdx;
			if (size < HR_MIN_FONTSIZE && text.length() == 1) {
				font = get(style, HR_FONTSIZE);
				factor = size / HR_FACTOR;
				chIdx = font.checkChar(text.charAt(0));
			}
			else {
				font = get(style, size);
				factor = 1.0;
				chIdx = -1;
			}
			
			final double[] answer = new double[] { (8 * text.length()) };
			
			if (chIdx < 0
					|| chIdx >= font.fStrWidth.length
					|| font.fStrWidth[chIdx] == 0) {
				synchronized (getTestLock()) {
					final TestGC gc = getTestGC();
					gc.setFont(font);
					
					final double width = gc.getStringWidth(text);
					answer[0] = width;
					if (chIdx >= 0) {
						if (font.fStrWidth.length == 0) {
							font.fStrWidth = new double[Math.max(512, chIdx + 256)];
						}
						else if (chIdx >= font.fStrWidth.length) {
							final double[] newWidth = new double[chIdx + 128];
							System.arraycopy(font.fStrWidth, 0, newWidth, 0, font.fStrWidth.length);
							font.fStrWidth = newWidth;
						}
						font.fStrWidth[chIdx] = width;
					}
				}
			}
			else {
				answer[0] = font.fStrWidth[chIdx];
			}
			if (factor != 1.0) {
//				fCurrentAnswer[0] = Math.round((fCurrentAnswer[0] * (factor * HR_ROUND_FACTOR)) + HR_ROUND_ADD) / HR_ROUND_FACTOR;
				answer[0] = polish(answer[0], factor);
			}
			
//			System.out.println("-> " + Arrays.toString(answer));
			return answer;
		}
		
		public synchronized double[] getSWTFontProperties(final int style, final int size) {
			final FontInstance font = get(style, size); // no HR!
			if (font.fSWTFontProperties == null) {
				synchronized (getTestLock()) {
					final TestGC gc = getTestGC();
					gc.setFont(font);
				}
			}
			return font.fSWTFontProperties;
		}
		
		private double polish(final double p, final double factor) {
			if (p == 0) {
				return 0;
			}
			final double add = (factor < POLISH_SMALL_MAX_FACTOR) ?
					(POLISH_SMALL_ADD_CORR / factor + POLISH_ADD_CONST) :
					(POLISH_SMALL_ADD_CORR/POLISH_SMALL_MAX_FACTOR + POLISH_ADD_CONST);
			if (p > 0) {
				return Math.round((p + POLISH_ADD_REL) * factor + add);
			}
			else {
				return Math.round((p - POLISH_ADD_REL) * factor - add);
			}
		}
		
		
		public void dispose() {
			for (int style = 0; style < R_STYLE_COUNT; style++) {
				final FontInstance[] styleFonts = fFonts[style];
				if (styleFonts != null) {
					for (int i = 0; i < styleFonts.length; i++) {
						if (styleFonts[i] != null && styleFonts[i].swtFont != null) {
							if (!styleFonts[i].swtFont.isDisposed()) {
								styleFonts[i].swtFont.dispose();
							}
							styleFonts[i] = null;
						}
					}
				}
			}
		}
		
	}
	
	
	private final Display fDisplay;
	
	private final Object fTestGCLock = new Object();
	private TestGC fTestGC;
	private boolean fDisposed;
	
	private final Map<String, FontFamily> fFontFamilies = new HashMap<String, FontFamily>();
	
	
	public FontManager(final Display display) {
		fDisplay = display;
	}
	
	
	public synchronized FontFamily getFamily(final String family) {
		FontFamily fontFamily = fFontFamilies.get(family);
		if (fontFamily == null) {
			fontFamily = new FontFamily(family);
			fFontFamilies.put(family, fontFamily);
		}
		return fontFamily;
	}
	
	
	protected final Object getTestLock() {
		return fTestGCLock;
	}
	
	protected final TestGC getTestGC() {
		if (fTestGC == null) {
			fDisplay.syncExec(new Runnable() {
				public void run() {
					if (!fDisposed) {
						fTestGC = new TestGC(fDisplay);
					}
				}
			});
		}
		return fTestGC;
	}
	
	
	public void dispose() {
		fDisposed = true;
		for (final FontFamily fontFamily : fFontFamilies.values()) {
			fontFamily.dispose();
		}
		fFontFamilies.clear();
		
		if (fTestGC != null) {
			fTestGC.dispose();
			fTestGC = null;
		}
	}
	
}
