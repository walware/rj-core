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

package de.walware.rj;


/**
 * Exception indicating that the initialization failed
 */
public class RjInitFailedException extends RjException {
	
	
	private static final long serialVersionUID = -6199674887137008313L;
	
	
	public RjInitFailedException(final String message) {
		super(message);
	}
	
	public RjInitFailedException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
}
