/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.srvImpl;

import java.net.URL;
import java.net.URLClassLoader;


public abstract class RJClassLoader extends URLClassLoader {
	
	
	public static final int OS_WIN = 1;
	public static final int OS_NIX = 2;
	public static final int OS_MAC = 3;
	
	
	public RJClassLoader(final URL[] urls, final ClassLoader parent) {
		super(urls, parent);
	}
	
	
	public abstract int getOSType();
	
	public abstract void addRLibrary(final String name, final String path);
	
	public abstract void addClassPath(final String cp);
	
}
