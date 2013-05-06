/*******************************************************************************
 * Copyright (c) 2012-2013 Stephan Wahlbrink (WalWare.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.renv;

import static de.walware.rj.server.client.AbstractRJComClient.RJ_CLIENT_ID;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.walware.rj.services.RPlatform;


/**
 * @since 2.0
 */
public class RPkgUtil {
	
	
	private static boolean isWin(final RPlatform rPlatform) {
		return rPlatform.getOsType().equals(RPlatform.OS_WINDOWS);
	}
	
	private static boolean isMac(final RPlatform rPlatform) {
		return rPlatform.getOSName().regionMatches(true, 0, "Mac OS", 0, 6); //$NON-NLS-1$
	}
	
	public static RPkgType getPkgType(final String fileName, final RPlatform rPlatform) {
		if (fileName.endsWith(".tar.gz")) { //$NON-NLS-1$
			return RPkgType.SOURCE;
		}
		if (isWin(rPlatform)) {
			if (fileName.toLowerCase().endsWith(".zip")) { //$NON-NLS-1$
				return RPkgType.BINARY;
			}
		}
		else if (isMac(rPlatform)) {
			if (fileName.endsWith(".tgz")) { //$NON-NLS-1$
				return RPkgType.BINARY;
			}
		}
		return null;
	}
	
	public static RPkgType checkPkgType(final String fileName, final RPlatform rPlatform) throws CoreException {
		final RPkgType pkgType = getPkgType(fileName, rPlatform);
		if (pkgType == null) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
					MessageFormat.format("Invalid file name ''{0}'' (unsupported extension) for R package on {1}.",
							fileName, rPlatform.getOSName() )));
		}
		return pkgType;
	}
	
	/**
	 * Checks if the given file name has the standard format <code>name_version.extension</code>.
	 * 
	 * @param fileName the file name to check
	 * @return a R package object with the detected name and version.
	 * @throws CoreException if the file name has not the standard format.
	 */
	public static IRPkg checkPkgFileName(final String fileName) throws CoreException {
		final int extIdx;
		if (fileName.endsWith(".tar.gz")) { //$NON-NLS-1$
			extIdx = fileName.length() - 7;
		}
		else if (fileName.endsWith(".zip") || fileName.endsWith(".tgz")) { //$NON-NLS-1$ //$NON-NLS-2$
			extIdx = fileName.length() - 4;
		}
		else {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
					MessageFormat.format("Invalid file name ''{0}'' (unsupported extension) for R package.",
							fileName )));
		}
		
		final int versionIdx = fileName.indexOf('_');
		if (versionIdx < 0) {
			throw new CoreException(new Status(IStatus.ERROR, RJ_CLIENT_ID,
					MessageFormat.format("Invalid file name ''{0}'' (missing version) for R package.",
							fileName )));
		}
		return new RPkg(fileName.substring(0, versionIdx),
				RNumVersion.create(fileName.substring(versionIdx + 1, extIdx)) );
	}
	
	public static String getPkgTypeInstallKey(final RPlatform rPlatform, final RPkgType pkgType) {
		if (pkgType == RPkgType.SOURCE) {
			return "source"; //$NON-NLS-1$
		}
		if (pkgType == RPkgType.BINARY) {
			if (isWin(rPlatform)) {
				return "win.binary"; //$NON-NLS-1$
			}
			else if (isMac(rPlatform)) {
				return "mac.binary.leopard"; //$NON-NLS-1$
			}
		}
		return null;
	}
	
	
}
