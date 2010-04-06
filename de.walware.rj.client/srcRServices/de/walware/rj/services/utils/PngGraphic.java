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

package de.walware.rj.services.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RPlatform;
import de.walware.rj.services.RService;


public class PngGraphic extends Graphic {
	
	
	public PngGraphic() {
	}
	
	
	@Override
	protected void prepare(final String filename, final RService service, final IProgressMonitor monitor) throws CoreException {
		final FunctionCall png = service.createFunctionCall("png");
		png.addChar("filename", filename);
		if (this.resolution > 0) {
			png.addInt("res", this.resolution);
		}
		if (this.sizeUnit != null) {
			png.addNum("width", this.sizeWidth);
			png.addNum("height", this.sizeHeight);
			png.addChar("unit", this.sizeUnit);
		}
		if (service.getPlatform().getOsType().equals(RPlatform.OS_WINDOWS)) {
			png.addLogi("restoreConsole", false);
		}
		png.evalVoid(monitor);
	}
	
}
