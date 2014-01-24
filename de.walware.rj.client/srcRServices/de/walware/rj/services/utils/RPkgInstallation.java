/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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

import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RIntegerStore;
import de.walware.rj.data.RVector;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.renv.IRPkg;
import de.walware.rj.renv.RNumVersion;
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
 * 
 * @since 2.0
 */
public class RPkgInstallation {
	
	
	private final IRPkg pkgInfo;
	
	private final File file;
	
	
	public RPkgInstallation(final File file) throws CoreException {
		if (file == null) {
			throw new NullPointerException();
		}
		this.pkgInfo = RPkgUtil.checkPkgFileName(file.getName());
		this.file = file;
	}
	
	
	public IRPkg getPkg() {
		return this.pkgInfo;
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
		String serverFile = null;
		String libLoc = null;
		try {
			{	final RVector<RIntegerStore> data = RDataUtil.checkRIntVector(r.evalData(
						"rj:::.renv.isValidLibLoc(.libPaths()[1])", monitor )); //$NON-NLS-1$
				final int state = RDataUtil.checkSingleIntValue(data);
				libLoc = data.getNames().getChar(0);
				if (state != 0) {
					throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
							MessageFormat.format("The library location ''{0}'' is not writable.", libLoc) ));
				}
			}
			
			{	final FunctionCall call = r.createFunctionCall("dir.create"); //$NON-NLS-1$
				call.addChar("rpkgs"); //$NON-NLS-1$
				call.addLogi("showWarnings", false); //$NON-NLS-1$
				call.evalVoid(monitor);
				
				serverFile = "rpkgs/" + source; //$NON-NLS-1$
			}
			
			uploadPkgFile(serverFile, r, monitor);
			
			{	final FunctionCall call = r.createFunctionCall("install.packages"); //$NON-NLS-1$
				call.addChar(serverFile);
				call.addChar("lib", libLoc); //$NON-NLS-1$
				call.addNull("repos"); //$NON-NLS-1$
				call.addChar("type", RPkgUtil.getPkgTypeInstallKey(r.getPlatform(), pkgType)); //$NON-NLS-1$
				call.evalVoid(monitor);
			}
			
			{	final FunctionCall call = r.createFunctionCall("packageDescription"); //$NON-NLS-1$
				call.addChar("pkg", this.pkgInfo.getName()); //$NON-NLS-1$
				call.addChar("lib.loc", libLoc); //$NON-NLS-1$
				call.addChar("fields", "Version"); //$NON-NLS-1$ //$NON-NLS-2$
				final RVector<?> data = RDataUtil.checkRVector(call.evalData(monitor));
				try {
					final String s = RDataUtil.checkSingleCharValue(data);
					final RNumVersion installedVersion = RNumVersion.create(s);
					if (!installedVersion.equals(this.pkgInfo.getVersion())) {
						throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
								MessageFormat.format("Validation of package installation failed: installed package has different version (found= {0}, expected= {1}).",
										installedVersion.toString(), this.pkgInfo.getVersion().toString() )));
					}
				}
				catch (final UnexpectedRDataException e) {
					throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
							"Validation of package installation failed: no installed package found." ));
				}
			}
			
			clear(serverFile, r, monitor);
			serverFile = null;
		}
		catch (final IOException e) {
			error = e;
		}
		catch (final CoreException e) {
			error = e;
		}
		catch (final UnexpectedRDataException e) {
			error = e;
		}
		finally {
			if (serverFile != null) {
				try {
					clear(serverFile, r, monitor);
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
