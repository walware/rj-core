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

import de.walware.rj.graphic.RText;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class TextElement extends RText implements IERGraphicInstruction {
	
	
	public final double swtStrWidth;
	
	
	public TextElement(final double x, final double y, final double rDeg, final double hAdj,
			final String text, final double swtStrWidth) {
		super(x, y, rDeg, hAdj, text);
		this.swtStrWidth = swtStrWidth;
	}
	
	
}
