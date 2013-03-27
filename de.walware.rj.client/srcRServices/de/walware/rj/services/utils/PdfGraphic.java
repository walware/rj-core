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

package de.walware.rj.services.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;


public class PdfGraphic extends Graphic {
	
	
	public PdfGraphic() {
	}
	
	
	@Override
	public void setSize(final double width, final double height, final String unit) {
		if (UNIT_PX.equals(unit)) {
			throw new IllegalArgumentException("Pixel not supported by PDF graphic.");
		}
		super.setSize(width, height, unit);
	}
	
	@Override
	protected void prepare(final String filename, final RService service, final IProgressMonitor monitor) throws CoreException {
		final FunctionCall png = service.createFunctionCall("pdf"); //$NON-NLS-1$
		png.addChar("file", filename); //$NON-NLS-1$
		if (this.resolution > 0) {
			png.addInt("res", this.resolution); //$NON-NLS-1$
		}
		if (this.sizeUnit != null) {
			if (this.sizeUnit.equals(UNIT_IN)) {
				png.addNum("width", this.sizeWidth); //$NON-NLS-1$
				png.addNum("height", this.sizeHeight); //$NON-NLS-1$
			}
			else if (this.sizeUnit.equals(UNIT_CM)) {
				png.addNum("width", this.sizeWidth/2.54); //$NON-NLS-1$
				png.addNum("height", this.sizeHeight/2.54); //$NON-NLS-1$
			}
			else if (this.sizeUnit.equals(UNIT_MM)) {
				png.addNum("width", this.sizeWidth/25.4); //$NON-NLS-1$
				png.addNum("height", this.sizeHeight/25.4); //$NON-NLS-1$
			}
		}
		png.evalVoid(monitor);
	}
	
}
