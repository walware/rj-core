/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data;


/**
 * An R object is of the type {@link RObject#TYPE_S4OBJECT S4 object}, if the R 
 * command <code>isS4(object)</code> returns true.  Such an R S4 object is
 * represented by an instance of this interface.
 * <p>
 * Even the interface extends {@link RList}, the objects are not a list in R!
 * The inheritance is only for a uniform API.</p>
 * <p>
 * If an S4 object in R simulates also a simple data type, this data is accessible
 * by its data slot (<code>.Data</code>).  For easier work with the data slot the
 * class provides the methods {@link #hasDataSlot()} to test if there is such a
 * slot, {@link #getDataSlot()} to get the R object in that slot and {@link #getData()}
 * returns directly the data store by the data slot.</p>
 * <p>
 * The count of all slots, its names and value objects are accessible by the inherited
 * methods {@link #getLength()}, {@link #getName(int)}, {@link #get(int)} and
 * {@link #get(String)}.</p>
 */
public interface RS4Object extends RList {
	
	
	/**
	 * Returns the S4 class name of this object in R.
	 * 
	 * @return the R class name
	 */
	@Override
	String getRClassName();
	
	/**
	 * Returns the length of the object. The length of an {@link RObject#TYPE_S4OBJECT S4 object}
	 * is the count of slots.
	 * <p>
	 * An equivalent command in R is for example <code>length(slotNames(obj))</code>, but
	 * not <code>length(obj)</code>.</p>
	 * 
	 * At moment, the length of an {@link RObject#TYPE_S4OBJECT S4 object} is always &le; 2<sup>31</sup>-1
	 * (representable by Java int).
	 * 
	 * @return the slot count
	 */
	@Override
	long getLength();
	
	/**
	 * Tests if the object respectively its class provides a data slot.
	 * <p>
	 * The data slot is the slot with the name <code>.Data</code>.</p>
	 * 
	 * @return <code>true</code> if there is a data slot, otherwise <code>false</code>
	 */
	boolean hasDataSlot();
	
	/**
	 * Returns the data slot, if it exists.
	 * 
	 * @return the object in the data slot
	 */
	RObject getDataSlot();
	
	/**
	 * Returns the data store of the data slot, if it exists.
	 * 
	 * @return the data of the data slot
	 */
	@Override
	RStore getData();
	
	/**
	 * Returns the names of the slots.
	 * 
	 * @return the slot names
	 */
	@Override
	RCharacterStore getNames();
	
	/**
	 * Returns the name of the slot at the given index.
	 * <p>
	 * This is equivalent to <code>getNames().getChar(idx)</code>.</p>
	 * 
	 * @param idx the index of the slot
	 * @return the slot names
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; slot count
	 */
	@Override
	String getName(int idx);
	
	/**
	 * Returns the value of the slot at the given index.
	 * 
	 * @param idx the index of the slot
	 * @return the value object of the slot
	 * @throws IndexOutOfBoundsException if <code>idx</code> &lt; 0 or <code>idx</code> &ge; slot count
	 */
	@Override
	RObject get(int idx);
	
	/**
	 * Returns the value of the slot with the given name.
	 * 
	 * @param name the name of the slot
	 * @return the value of the slot with the given name
	 * @throws IllegalArgumentException if the S4 type doesn't have a slot with the given name
	 */
	@Override
	RObject get(String name);
	
}
