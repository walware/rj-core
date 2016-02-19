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

package de.walware.rj.eclient.graphics.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.ecommons.ts.ITool;

import de.walware.rj.eclient.AbstractRToolRunnable;
import de.walware.rj.eclient.IRToolService;
import de.walware.rj.eclient.graphics.IERGraphic;


public class CopyToDevRunnable extends AbstractRToolRunnable {
	
	
	private final IERGraphic fGraphic;
	private final String fToDev;
	private final String fToDevFile;
	private final String fToDevArgs;
	
	
	public CopyToDevRunnable(final IERGraphic graphic, final String toDev,
			final String toDevFile, final String toDevArgs) {
		super("r/rj/gd/copy", "Copy R Graphic ('" + toDev + "')"); //$NON-NLS-1$
		fGraphic = graphic;
		fToDev = toDev;
		fToDevFile = toDevFile;
		fToDevArgs = toDevArgs;
	}
	
	@Override
	public boolean changed(final int event, final ITool tool) {
		switch (event) {
		case MOVING_FROM:
			return false;
		default:
			return true;
		}
	}
	
	@Override
	public void run(final IRToolService r,
			final IProgressMonitor monitor) throws CoreException {
		fGraphic.copy(fToDev, fToDevFile, fToDevArgs, monitor);
	}
	
}
