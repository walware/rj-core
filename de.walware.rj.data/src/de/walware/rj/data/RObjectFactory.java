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
import java.io.ObjectInput;
import java.io.ObjectOutput;


public interface RObjectFactory {
	
	/** Flag to fetch only the structure but not the data (store) of the objects */
	public static final int F_ONLY_STRUCT = 0x1;
	
	/** XXX: Not yet fully implemented */
	public static final int O_WITH_ATTR = 0x2;
	public static final int F_WITH_ATTR = 0x2;
	
	public static final int O_CLASS_NAME = 0x4;
	public static final int F_CLASS_NAME = 0x4;
	
	public static final int O_NOCHILDREN = 0x8;
	public static final int F_NOCHILDREN = 0x8;
	
	
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
	
	void writeObject(RObject object, ObjectOutput out, int flags) throws IOException;
	RObject readObject(ObjectInput in, int flags) throws IOException, ClassNotFoundException;
	
	void writeStore(RStore data, ObjectOutput out, int flags) throws IOException;
	RStore readStore(ObjectInput in, int flags) throws IOException, ClassNotFoundException;
	
	void writeAttributeList(RList list, ObjectOutput out, int flags) throws IOException;
	RList readAttributeList(ObjectInput in, int flags) throws IOException, ClassNotFoundException;
	
}
