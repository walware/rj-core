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

package de.walware.rj.data;


public interface RDataFrame extends RList, RObject {
	
	int getColumnCount();
	public RCharacterStore getColumnNames();
	String getColumnName(int idx);
	public RStore getColumn(int idx);
	
	int getRowCount();
	RStore getRowNames();
	
//	void setColumn(int idx, RStore column);
//	void insertColumn(int idx, RStore column);
//	void removeColumn(int idx);
	
//	void insertRow(int idx);
//	void removeRow(int idx);
	
}
