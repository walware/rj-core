/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;


/**
 * Exception indicating that the R data (object or store) is missing or not of the expected type.
 */
public class UnexpectedRDataException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	
	public UnexpectedRDataException(final String message) {
		super(message);
	}
	
}
