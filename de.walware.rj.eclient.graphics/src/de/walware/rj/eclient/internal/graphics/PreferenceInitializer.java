/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.internal.graphics;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import de.walware.rj.eclient.graphics.RGraphics;


public class PreferenceInitializer extends AbstractPreferenceInitializer {
	
	
	public PreferenceInitializer() {
	}
	
	
	@Override
	public void initializeDefaultPreferences() {
		final DefaultScope defaultScope = new DefaultScope();
		final IEclipsePreferences node = defaultScope.getNode(RGraphics.PREF_FONTS_QUALIFIER);
		if (Platform.getOS().startsWith("win")) {
			node.put(RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY, "Times New Roman");
			node.put(RGraphics.PREF_FONTS_SANS_FONTNAME_KEY, "Arial");
			node.put(RGraphics.PREF_FONTS_MONO_FONTNAME_KEY, "Courier New");
		}
		else {
			node.put(RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY, "Serif");
			node.put(RGraphics.PREF_FONTS_SANS_FONTNAME_KEY, "Sans");
			node.put(RGraphics.PREF_FONTS_MONO_FONTNAME_KEY, "Mono");
		}
	}
	
}
