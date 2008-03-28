/*******************************************************************************
 * Copyright (c) 2005-2008 RoSuDa (www.rosuda.org) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *    RoSuDa, University Augsburg - initial API and implementation
 *******************************************************************************/

/* package (default) */

import java.util.logging.Logger;

import org.rosuda.rj.JRClassLoader;


public class RJavaClassLoader extends JRClassLoader {
	
	
	public static RJavaClassLoader primaryLoader = null;
	
	public static RJavaClassLoader getPrimaryLoader() {
		return primaryLoader;
	}
	
	
	public RJavaClassLoader(final String a, final String b) {
		this();
	}
	
	public RJavaClassLoader() {
		super();
		
		if (primaryLoader == null) {
			primaryLoader = this;
		} else {
			Logger.getLogger("org.rosuda.rjava").warning("Non-primary instance of RJavaClassLoader created");
		}
	}
	
}
