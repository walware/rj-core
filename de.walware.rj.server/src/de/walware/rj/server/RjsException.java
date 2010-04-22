/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;



/**
 * Exception indicating that RJ operation failed
 */
public class RjsException extends Exception {
	
	
	private static final long serialVersionUID = 4433450430400305890L;
	
	
	private final int code;
	
	
	public RjsException(final int code, final String message) {
		super(message);
		this.code = code;
	}
	
	public RjsException(final int code, final String message, final Throwable cause) {
		super(message, cause);
		this.code = code;
	}
	
	
	public RjsStatus getStatus() {
		return new RjsStatus(RjsStatus.ERROR, this.code, getMessage());
	}
	
}
