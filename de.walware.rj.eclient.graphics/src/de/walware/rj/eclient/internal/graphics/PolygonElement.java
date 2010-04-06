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

import de.walware.rj.graphic.RPolygon;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class PolygonElement extends RPolygon implements IERGraphicInstruction {
	
	
	public PolygonElement(final double[] x, final double[] y) {
		super(x, y);
	}
	
	
}
