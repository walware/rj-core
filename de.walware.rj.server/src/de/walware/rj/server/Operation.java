/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;


/**
 * Operation
 */
public interface Operation {
	
	
	interface SyncOp extends Operation {
		
	}
	
	interface AsyncOp extends Operation {
		
	}
	
	
	byte getOp();
	
}
