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

import de.walware.rj.graphic.RPolyline;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class PolylineElement extends RPolyline implements IERGraphicInstruction {
	
	
	public PolylineElement(final double[] x, final double[] y) {
		super(x, y);
	}
	
	
}
