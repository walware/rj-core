/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.internal.graphics;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import de.walware.ecommons.collections.IntHashMap;
import de.walware.ecommons.collections.IntMap.IntEntry;


public class ColorManager {
	
	
	private final Display fDisplay;
	
	private final IntHashMap<Color> fColors = new IntHashMap<Color>(0x7f);
	
	
	public ColorManager(final Display display) {
		fDisplay = display;
	}
	
	
	public synchronized Color getColor(final int rgb) {
		Color color = fColors.get(rgb);
		if (color == null) {
			color = new Color(fDisplay,
					(rgb & 0xff), ((rgb >>> 8) & 0xff), ((rgb >>> 16) & 0xff) );
			fColors.put(rgb, color);
		}
		return color;
	}
	
	
	public void dispose() {
		for (final IntEntry<Color> entry : fColors.entryIntSet()) {
			entry.getValue().dispose();
		}
		fColors.clear();
	}
	
}
