/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.graphic.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Code generation for AdbSymbol
 * 
 * http://unicode.org/Public/MAPPINGS/VENDORS/ADOBE/symbol.txt
 */
class CodeGenAdbSymbol {
	
	
	public static void main(final String[] args) throws Exception {
		final File file = new File("bin/" + CodeGenAdbSymbol.class.getPackage().getName().replace('.', '/') + "/AdbSymbol.txt");
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.length() == 0 || line.startsWith("#")) {
				continue;
			}
			final String[] columns = line.split("\t");
			if (columns.length != 4) {
				throw new IOException(line);
			}
			
			System.out.print("MAPPING[0x");
			System.out.print(columns[0]);
			System.out.print("] = 0x");
			System.out.print(columns[1]);
			System.out.print("; // ");
			System.out.print(columns[2].substring(2));
			
			final int codepoint = Integer.parseInt(columns[0], 16);
			if (new String(new int[] { codepoint }, 0, 1).length() != 1) {
				throw new IOException("Warning: multichar codepoint");
			}
			
			System.out.println();
		}
	}
	
}
