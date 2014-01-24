/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.internal.graphics;

import de.walware.rj.graphic.RText;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class TextElement extends RText implements IERGraphicInstruction {
	
	
	public final double swtStrWidth;
	
	
	public TextElement(final String text,
			final double x, final double y, final double rDeg, final double hAdj,
			final double swtStrWidth) {
		super(text, x, y, rDeg, hAdj);
		this.swtStrWidth = swtStrWidth;
	}
	
	
}
