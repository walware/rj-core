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
 * Data structure without dimension property in R
 * 
 * An object is a vector if it is a R data type objects without a valid
 * dimension attribute.
 * 
 * @param <DataType> the type of the data store
 */
public interface RVector<DataType extends RStore> extends RObject {
	
	
	RCharacterStore getNames();
	
	DataType getData();
	
	void insert(int idx);
	void remove(int idx);
	
}
