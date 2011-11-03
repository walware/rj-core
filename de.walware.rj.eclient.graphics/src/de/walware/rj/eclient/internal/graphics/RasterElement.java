/*******************************************************************************
 * Copyright (c) 2011 WalWare OpenSource (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.internal.graphics;

import org.eclipse.swt.graphics.Image;

import de.walware.rj.graphic.RRaster;

import de.walware.rj.eclient.graphics.IERGraphicInstruction;


public class RasterElement extends RRaster implements IERGraphicInstruction {
	
	
	public final Image swtImage;
	
	
	public RasterElement(final byte[] imgData, final int imgWidth, final int imgHeight,
			final double x, final double y, final double w, final double h,
			final double rDeg, final boolean interpolate,
			final Image swtImage) {
		super(imgData, imgWidth, imgHeight, x, y, w, h, rDeg, interpolate);
		
		this.swtImage = swtImage;
	}
	
	
}
