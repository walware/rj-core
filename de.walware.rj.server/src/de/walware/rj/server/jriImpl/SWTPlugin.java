/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jriImpl;

import org.eclipse.swt.widgets.Display;

import de.walware.rj.server.ServerLocalPlugin;


/**
 * Creates a display for native widgets ('native' depends on SWT in classpath).
 */
public class SWTPlugin implements ServerLocalPlugin {
	
	
	private Display display;
	
	
	public SWTPlugin() {
	}
	
	
	public String getSymbolicName() {
		return "swt";
	}
	
	public void rjIdle() throws Exception {
		if (this.display == null) {
			this.display = new Display();
		}
		this.display.readAndDispatch();
	}
	
	public void rjStop(final int regular) throws Exception {
		if (this.display != null) {
			this.display.dispose();
			this.display = null;
		}
	}
	
}
