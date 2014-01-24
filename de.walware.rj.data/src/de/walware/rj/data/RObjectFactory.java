/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;

import java.io.IOException;



public interface RObjectFactory {
	
	/** Flag to fetch only the structure but not the data (store) of the objects */
	int F_ONLY_STRUCT = 0x1;
	
	/** XXX: Not yet implemented */
	int F_WITH_ATTR = 0x2;
	
	/** Flag to load environments directly instead of the reference only */
	int F_LOAD_ENVIR = 0x10;
	
	/** Flag to eval all promises directly */
	int F_LOAD_PROMISE = 0x20;
	
	
	int O_LENGTHGRADE_MASK = 7; // 3 bits
	
	int O_WITH_ATTR = 1 << 3;
	
	int O_CLASS_NAME = 1 << 4;
	
	int O_NO_CHILDREN = 1 << 5;
	
	int O_WITH_NAMES = 1 << 6;
	
	
//	RArgument createArgument(String name, String defaultSource);
//	RFunction createFunction(RArgument[] argument);
//	
	<DataType extends RStore> RVector<DataType> createVector(DataType data);
	<DataType extends RStore> RArray<DataType> createArray(DataType data, int[] dim);
	<DataType extends RStore> RArray<DataType> createMatrix(DataType data, int dim1, int dim2);
	RList createList(RObject[] components, String[] names);
//	RDataFrame createDataFrame(RData[] columns, String[] columnNames, String[] rowNames);
	
	RLanguage createName(String name);
	RLanguage createExpression(String expr);
	
	RLogicalStore createLogiData(boolean[] logiValues);
	RIntegerStore createIntData(int[] intValues);
	RNumericStore createNumData(double[] numValues);
	RComplexStore createCplxData(double[] reValues, double[] imValues);
	RCharacterStore createCharData(String[] charValues);
	RRawStore createRawData(byte[] values);
	RFactorStore createFactorData(int[] codes, String[] levels);
	
	void writeObject(RObject object, RJIO io) throws IOException;
	RObject readObject(RJIO io) throws IOException;
	
	void writeStore(RStore data, RJIO io) throws IOException;
	RStore readStore(RJIO io, long length) throws IOException;
	
	void writeAttributeList(RList list, RJIO io) throws IOException;
	RList readAttributeList(RJIO io) throws IOException;
	
	void writeNames(RStore names, RJIO io) throws IOException;
	RStore readNames(RJIO io, long length) throws IOException;
	
}
