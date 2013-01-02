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


public class RGraphics {
	
	
	public static final String PLUGIN_ID = "de.walware.rj.eclient.graphics"; //$NON-NLS-1$
	
	
	public static final String PREF_DISPLAY_QUALIFIER = PLUGIN_ID + "/display"; //$NON-NLS-1$
	
	/**
	 * Preference key: custom dpi setting (string "horizontal,vertical"), none/empty for default
	 * 
	 * @since 1.0
	 */
	public static final String PREF_DISPLAY_CUSTOM_DPI_KEY = "dpi.xy"; //$NON-NLS-1$
	
	
	public static final String PREF_FONTS_QUALIFIER = PLUGIN_ID + "/fonts"; //$NON-NLS-1$
	public static final String PREF_FONTS_SERIF_FONTNAME_KEY = "serif.name"; //$NON-NLS-1$
	public static final String PREF_FONTS_SANS_FONTNAME_KEY = "sans.name"; //$NON-NLS-1$
	public static final String PREF_FONTS_MONO_FONTNAME_KEY = "mono.name"; //$NON-NLS-1$
	
	/**
	 * Preference key: if (boolean) a special symbol font should be used
	 * 
	 * @since 1.0
	 */
	public static final String PREF_FONTS_SYMBOL_USE_KEY = "symbol.use"; //$NON-NLS-1$
	/**
	 * Preference key: name (string) of the symbol font (if special symbol font is enabled)
	 * 
	 * @since 1.0
	 */
	public static final String PREF_FONTS_SYMBOL_FONTNAME_KEY = "symbol.name"; //$NON-NLS-1$
	/**
	 * Preference key: encoding (known key) of the symbol font (if special symbol font is enabled)
	 * 
	 * @since 1.0
	 */
	public static final String PREF_FONTS_SYMBOL_ENCODING_KEY = "symbol.enc"; //$NON-NLS-1$
	
	
}
