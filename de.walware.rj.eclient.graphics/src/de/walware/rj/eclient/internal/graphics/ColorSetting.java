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

import org.eclipse.swt.graphics.Color;

import de.walware.rj.graphic.RColorSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class ColorSetting extends RColorSetting implements IERGraphicInstruction {
	
	
	public final Color swtColor;
	
	
	public ColorSetting(final int color, final Color swtColor) {
		super(color);
		this.swtColor = swtColor;
	}
	
	
}
