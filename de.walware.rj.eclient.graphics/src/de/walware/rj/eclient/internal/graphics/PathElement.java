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

import org.eclipse.swt.graphics.Path;

import de.walware.rj.graphic.RPath;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class PathElement extends RPath implements IERGraphicInstruction {
	
	
	public final Path swtPath;
	
	
	public PathElement(final int[] n, final double[] x, final double[] y, final int mode,
			final Path swtPath) {
		super(n, x, y, mode);
		
		this.swtPath = swtPath;
	}
	
	
}
