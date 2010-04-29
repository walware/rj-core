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

import java.io.IOException;



public interface RObjectFactory {
	
	/** Flag to fetch only the structure but not the data (store) of the objects */
	int F_ONLY_STRUCT = 0x1;
	
	/** XXX: Not yet implemented */
	int F_WITH_ATTR = 0x2;
	
	
	int O_WITH_ATTR = 0x2;
	
	int O_CLASS_NAME = 0x4;
	
	int O_NO_CHILDREN = 0x8;
	
	int O_WITH_NAMES = 0x10;
	
	
//	RArgument createArgument(String name, String defaultSource);
//	RFunction createFunction(RArgument[] argument);
//	
//	RLogicalData createLogicalData(boolean[] boolValues, int[] naIdxs);
//	RLogicalData createLogicalData(int[] intValues, int trueCode, int naCode);
//	RNumericData createRealData(double[] realValues, int[] naIdxs);
//	RIntegerData createIntegerData(int[] intValues, int[] naIdxs);
//	RComplexData createComplexData(double[] realValues, double[] imaginaryValues, int[] naIdxs);
//	RCharacterData createCharacterData(String[] charValues);
//	RFactor createFactor(int[] codes, String[] labels);
//	RRawData createRawData(byte[] values);
	
	<DataType extends RStore> RVector<DataType> createVector(DataType data);
	<DataType extends RStore> RArray<DataType> createArray(DataType data, int[] dim);
	<DataType extends RStore> RArray<DataType> createMatrix(DataType data, int dim1, int dim2);
	RList createList(RObject[] components, String[] names);
//	RDataFrame createDataFrame(RData[] columns, String[] columnNames, String[] rowNames);
	
	void writeObject(RObject object, RJIO io) throws IOException;
	RObject readObject(RJIO io) throws IOException;
	
	void writeStore(RStore data, RJIO io) throws IOException;
	RStore readStore(RJIO io) throws IOException;
	
	void writeAttributeList(RList list, RJIO io) throws IOException;
	RList readAttributeList(RJIO io) throws IOException;
	
	void writeNames(RStore names, RJIO io) throws IOException;
	RStore readNames(RJIO io) throws IOException;
	
}
