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

package de.walware.rj.eclient.internal.graphics;

import org.eclipse.swt.graphics.Color;

import de.walware.rj.graphic.RFillSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class FillSetting extends RFillSetting implements IERGraphicInstruction {
	
	
	public final Color swtColor;
	
	
	public FillSetting(final int color, final Color swtColor) {
		super(color);
		this.swtColor = swtColor;
	}
	
	
}
