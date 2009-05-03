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

package de.walware.rj.data;


/**
 * List object in R
 * 
 * Can be a list or a pairlist in R.
 * Other R object types extending this interface does not necessary provide the
 * full functionality of all methods of this interface.
 */
public interface RList extends RObject {
	
	RCharacterStore getNames();
	String getName(int idx);
	RObject get(int idx);
	RObject get(String name);
	
	RObject[] toArray();
	
	void insert(int idx, String name, RObject component);
	void add(String name, RObject component);
	boolean set(int idx, RObject component);
	boolean set(String name, RObject component);
	void remove(int idx);
	
}
