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

import org.eclipse.swt.graphics.Font;

import de.walware.rj.graphic.RFontSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class FontSetting extends RFontSetting implements IERGraphicInstruction {
	
	
	public final Font swtFont;
	
	
	public FontSetting(final String family, final int face, final double pointSize, final double cex, final double lineHeight,
			final Font swtFont) {
		super(family, face, pointSize, cex, lineHeight);
		this.swtFont = swtFont;
	}
	
	
}
