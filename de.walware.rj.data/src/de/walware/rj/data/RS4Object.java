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
 * S4 object in R
 * 
 * An object is an S4 object if:
 *   <code>isS4(x) == true</code>
 * The mode/type of this object in R is not necessarily S4,
 * only if there is no data slot.
 */
public interface RS4Object extends RList {
	
	/**
	 * Tests if the S4 type provides a data slot
	 */
	public boolean hasDataSlot();
	
	/**
	 * @return the data of the data slot, if exists
	 */
	public RStore getData();
	
	/**
	 * @return the slot names (at no time <code>null</code>)
	 */
	public RCharacterStore getNames();
	
	/**
	 * @return the value of the slot
	 */
	public RObject get(int idx);
	
	/**
	 * @return the value of the slot with the given name
	 * @throws IllegalArgumentException if the S4 type doesn't have a slot with the given name
	 */
	public RObject get(String name);
	
}
