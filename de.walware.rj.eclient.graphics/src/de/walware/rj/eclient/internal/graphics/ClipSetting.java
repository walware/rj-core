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

import de.walware.rj.graphic.RClipSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class ClipSetting extends RClipSetting implements IERGraphicInstruction {
	
	
	public ClipSetting(final double x0, final double y0, final double x1, final double y1) {
		super(x0, y0, x1, y1);
	}
	
	
}
