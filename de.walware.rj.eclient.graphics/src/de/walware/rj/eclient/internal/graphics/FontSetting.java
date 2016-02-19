/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.internal.graphics;

import org.eclipse.swt.graphics.Font;

import de.walware.rj.graphic.RFontSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class FontSetting extends RFontSetting implements IERGraphicInstruction {
	
	
	public final Font swtFont;
	public final double[] swtProperties;
	
	
	public FontSetting(final String family, final int face, final double pointSize, final double lineHeight,
			final Font swtFont, final double[] swtProperties) {
		super(family, face, pointSize, lineHeight);
		this.swtFont = swtFont;
		this.swtProperties = swtProperties;
	}
	
	
}
