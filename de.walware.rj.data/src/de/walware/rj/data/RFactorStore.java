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
 * Interface for R data stores of type {@link RStore#FACTOR}.
 * <p>
 * An R data store implements this interface if the R function
 * <code>class(object)</code> returns 'factor'.</p>
 */
public interface RFactorStore extends RIntegerStore {
	
	// TODO Docu && Constructors (-> 1-based codes)
	// TODO getFactorName(code) for 1-based code
	
	boolean isOrdered();
	
	RCharacterStore getLevels();
	int getLevelCount();
	
//	void addLevel(final String label);
//	void renameLevel(final String oldLabel, final String newLabel);
//	void insertLevel(final int position, final String label);
//	void removeLevel(final String label);
	
	RCharacterStore toCharacterData();
	
}
