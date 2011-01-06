/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.srvext;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;


/**
 * Server utilities when using the Eclipse Platform.
 */
public class EServerUtil extends ServerUtil {
	
	
	public static String[] searchRJLibsInPlatform(final String[] libs, final boolean is64) {
		final Set<String> resolved = new LinkedHashSet<String>();
		for (final String lib : libs) {
			final Bundle pluginBundle = Platform.getBundle(lib);
			if (pluginBundle != null) {
				addPath(pluginBundle, resolved, is64);
				final Bundle[] fragments = Platform.getFragments(pluginBundle);
				if (fragments != null) {
					for (final Bundle fragmentBundle : fragments) {
						addPath(fragmentBundle, resolved, is64);
					}
				}
			}
		}
		return resolved.toArray(new String[resolved.size()]);
	}
	
	private static void addPath(final Bundle bundle, final Set<String> classpath, final boolean is64) {
//		String location = bundle.getLocation();
//		if (location.startsWith("initial@")) {
//			location = location.substring(8);
//		}
//		if (location.startsWith("reference:file:")) { //$NON-NLS-1$
//			location = location.substring(15);
//			IPath path = new Path(location);
//			if (!path.isAbsolute()) {
//				path = new Path(Platform.getInstallLocation().getURL().getFile()).append(path);
//			}
//			String checked = path.lastSegment();
//			if (checked.contains("motif")) { //$NON-NLS-1$
//				checked = checked.replaceAll("motif", "gtk"); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//			if (checked.contains("gtk")) { //$NON-NLS-1$
//				if (is64 && !checked.contains("64")) { //$NON-NLS-1$
//					checked = checked.replaceAll("x86", "x86_64"); //$NON-NLS-1$ //$NON-NLS-2$
//				}
//				if (!is64 && checked.contains("64")) { //$NON-NLS-1$
//					checked = checked.replaceAll("x86_64", "x86"); //$NON-NLS-1$ //$NON-NLS-2$
//				}
//			}
//			final String s = path.removeLastSegments(1).append(checked).makeAbsolute().toOSString();
//			if (location.endsWith("/")) { // //$NON-NLS-1$
//				if (Platform.inDevelopmentMode()) {
//					classpath.add(s+File.separatorChar+"bin"+File.separatorChar); //$NON-NLS-1$
//				}
//				classpath.add(s+File.separatorChar);
//			}
//			else {
//				classpath.add(s);
//			}
//			return;
//		}
		try {
			String s = FileLocator.resolve(bundle.getEntry("/")).toExternalForm();
			if (s.startsWith("jar:") && s.endsWith("!/")) {
				s = s.substring(4, s.length()-2);
			}
			if (s.startsWith("file:")) {
				s = s.substring(5);
			}
			if (Platform.inDevelopmentMode() && s.endsWith("/")) {
				classpath.add(s+"bin/");
			}
			classpath.add(s);
			return;
		}
		catch (final Exception e) {}
		handle(new Status(IStatus.WARNING, RJ_SERVER_ID, 
				"Unknown location for plug-in: '"+bundle.getBundleId()+"'. May cause fail to startup RJ (RMI/JRI)")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static void handle(final IStatus status) {
		Platform.getLog(Platform.getBundle(RJ_SERVER_ID)).log(status);
	}
	
}
