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

package de.walware.rj.server.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;


/**
 * Interface for (RJ) graphics offering actions which can be used
 * in graphic views/windows.
 */
public interface RClientGraphicActions {
	
	
	Object getRHandle();
	
	String getRLabel();
	
	IStatus resizeGraphic(int devId, Runnable beforeResize);
	
	IStatus closeGraphic(int devId);
	
	
	void copy(int devId, String toDev, String toDevFile, String toDevArgs,
			IProgressMonitor monitor) throws CoreException;
	double[] convertGraphic2User(int devId, double[] xy,
			IProgressMonitor monitor) throws CoreException;
	double[] convertUser2Graphic(int devId, double[] xy,
			IProgressMonitor monitor) throws CoreException;
	
}
