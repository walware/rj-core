/*******************************************************************************
 * Copyright (c) 2013 Stephan Wahlbrink (WalWare.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.services.utils;

import static de.walware.rj.server.client.AbstractRJComClient.RJ_CLIENT_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.walware.rj.renv.RPkgType;
import de.walware.rj.renv.RPkgUtil;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;


/**
 * Utility to install an R package from a local file.
 * <p>
 * Run {@link #install(RService, IProgressMonitor)} to install the package.</p>
 * <p>
 * The class can be reused.</p>
 */
public class RPkgInstallation {
	
	
	private final String pkgName;
	
	private final File file;
	
	
	public RPkgInstallation(final File file) throws CoreException {
		if (file == null) {
			throw new NullPointerException();
		}
		this.pkgName = RPkgUtil.checkPkgFileName(file.getName());
		this.file = file;
	}
	
	
	public String getPkgName() {
		return this.pkgName;
	}
	
	protected String getPkgFileName() {
		return this.file.getName();
	}
	
	protected void uploadPkgFile(final String target, final RService r,
			final IProgressMonitor monitor) throws CoreException, IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(this.file);
			r.uploadFile(in, this.file.length(), target, 0, monitor);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (final IOException e) {}
			}
		}
	}
	
	public void install(final RService r, final IProgressMonitor monitor) throws CoreException {
		final String source = getPkgFileName();
		final RPkgType pkgType = RPkgUtil.checkPkgType(source, r.getPlatform());
		
		Exception error = null;
		String target = null;
		try {
			{	final FunctionCall call = r.createFunctionCall("dir.create"); //$NON-NLS-1$
				call.addChar("rpkgs");
				call.addLogi("showWarnings", false);
				call.evalVoid(monitor);
				
				target = "rpkgs/" + source;
			}
			
			uploadPkgFile(target, r, monitor);
			
			{	final FunctionCall call = r.createFunctionCall("install.packages"); //$NON-NLS-1$
				call.addChar(target);
				call.addNull("repos"); //$NON-NLS-1$
				call.addChar("type", RPkgUtil.getPkgTypeInstallKey(r.getPlatform(), pkgType)); //$NON-NLS-1$
				call.evalVoid(monitor);
			}
			
			clear(target, r, monitor);
			target = null;
		}
		catch (final IOException e) {
			error = e;
		}
		catch (final CoreException e) {
			error = e;
		}
		finally {
			if (target != null) {
				try {
					clear(target, r, monitor);
				}
				catch (final Exception e) {}
			}
		}
		if (error != null) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
					MessageFormat.format("An error occurred when installing R package from {0}.",
							source), error ));
		}
	}
	
	private void clear(final String target, final RService r, final IProgressMonitor monitor) throws CoreException {
		final FunctionCall call = r.createFunctionCall("file.remove"); //$NON-NLS-1$
		call.addChar(target);
		call.evalVoid(monitor);
	}
	
}
