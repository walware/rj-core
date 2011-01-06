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

package de.walware.rj.server;

import java.util.Map;


/**
 * Interface from R to Java side of the RJ server
 */
public class RJ {
	
	
	private static RJ instance;
	
	
	public final static RJ get() {
		return instance;
	}
	
	
	protected RJ() {
		if (instance != null) {
			throw new IllegalStateException();
		}
		instance = this;
	}
	
	
	public void onRExit() {
		instance = null;
	}
	
	public Map<String, Object> execUICommand(final String command, final Map<String, Object> args, final boolean wait) {
		throw new UnsupportedOperationException();
	}
	
	public void registerGraphic(final RjsGraphic graphic) {
	}
	
	public void initLastGraphic(final int devId, final String target) {
	}
	
	public void unregisterGraphic(final RjsGraphic graphic) {
	}
	
	public double[] execGDCommand(final GDCmdItem cmd) {
		throw new UnsupportedOperationException();
	}
	
}
