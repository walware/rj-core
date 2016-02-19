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

import de.walware.rj.graphic.RLineSetting;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class LineSetting extends RLineSetting implements IERGraphicInstruction {
	
	
	public LineSetting(final int type, final double width) {
		super(type, width);
	}
	
	
}
