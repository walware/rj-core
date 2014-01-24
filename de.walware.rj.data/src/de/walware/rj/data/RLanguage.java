/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
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
 * R object of type call
 */
public interface RLanguage extends RObject {
	
	
	byte NAME =                                             0x01;
	
	byte EXPRESSION =                                       0x02;
	
	byte CALL =                                             0x03;
	
	
	byte getLanguageType();
	
	String getSource();
	
	
}
