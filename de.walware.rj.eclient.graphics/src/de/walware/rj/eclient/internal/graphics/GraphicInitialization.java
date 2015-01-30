/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
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

import de.walware.rj.graphic.RGraphicInitialization;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class GraphicInitialization extends RGraphicInitialization implements IERGraphicInstruction {
	
	
	public final Color swtCanvasColor;
	
	
	public GraphicInitialization(final double width, final double height, final int canvasColor,
			final Color swtCanvasColor) {
		super(width, height, canvasColor);
		
		this.swtCanvasColor = swtCanvasColor;
	}
	
	
}
