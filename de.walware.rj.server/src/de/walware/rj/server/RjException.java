/*******************************************************************************
 * Copyright (c) 2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;


public class RjException extends Exception{
	
	
	private static final long serialVersionUID = 5870562800737177156L;
	
	
	public RjException(String msg) {
		super(msg);
	}
	
	public RjException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
