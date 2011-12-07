/*******************************************************************************
 * Copyright (c) 2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.dbg;


public class Srcref {
	
	
	public static final int BEGIN_LINE = 0;
	public static final int BEGIN_COLUMN = 4;
	public static final int BEGIN_BYTE = 1;
	
	public static final int END_LINE = 2;
	public static final int END_COLUMN = 5;
	public static final int END_BYTE = 3;
	
	public static final int NA = Integer.MIN_VALUE;
	
	
	public static int[] diff(final int[] base, final int[] rel) {
		final int[] diff = new int[6];
		
		if (base[BEGIN_LINE] > 0 && rel[BEGIN_LINE] > 0) {
			diff[BEGIN_LINE] = 1 + rel[BEGIN_LINE] - base[BEGIN_LINE];
			if (diff[BEGIN_LINE] <= 0) {
				return null;
			}
		}
		else {
			diff[BEGIN_LINE] = NA;
		}
		if (diff[BEGIN_LINE] == 1) {
			if (base[BEGIN_COLUMN] > 0 && rel[BEGIN_COLUMN] > 0) {
				diff[BEGIN_COLUMN] = 1 + rel[BEGIN_COLUMN] - base[BEGIN_COLUMN];
				if (diff[BEGIN_COLUMN] <= 0) {
					return null;
				}
			}
			else {
				diff[BEGIN_COLUMN] = NA;
			}
			if (base[BEGIN_BYTE] > 0 && rel[BEGIN_BYTE] > 0) {
				diff[BEGIN_BYTE] = 1 + rel[BEGIN_BYTE] - base[BEGIN_BYTE];
				if (diff[BEGIN_BYTE] <= 0) {
					diff[BEGIN_BYTE] = NA;
				}
			}
			else {
				diff[BEGIN_BYTE] = NA;
			}
		}
		
		if (base[BEGIN_LINE] > 0 && rel[END_LINE] > 0) {
			diff[END_LINE] = 1 + rel[END_LINE] - base[BEGIN_LINE];
			if (diff[END_LINE] <= 0) {
				return null;
			}
		}
		else {
			diff[END_LINE] = NA;
		}
		if (diff[END_LINE] == 1) {
			if (base[BEGIN_COLUMN] > 0 && rel[END_COLUMN] > 0) {
				diff[END_COLUMN] = 1 + rel[END_COLUMN] - base[BEGIN_COLUMN];
				if (diff[END_COLUMN] <= 0) {
					return null;
				}
			}
			else {
				diff[END_COLUMN] = NA;
			}
			if (base[BEGIN_BYTE] > 0 && rel[END_BYTE] > 0) {
				diff[END_BYTE] = 1 + rel[END_BYTE] - base[BEGIN_BYTE];
				if (diff[END_BYTE] <= 0) {
					diff[END_BYTE] = NA;
				}
			}
			else {
				diff[END_BYTE] = NA;
			}
		}
		
		return diff;
	}
	
	public static int[] add(final int[] base, final int[] diff) {
		final int[] sum = new int[6];
		
		if (base[BEGIN_LINE] > 0 && diff[BEGIN_LINE] > 0) {
			sum[BEGIN_LINE] = base[BEGIN_LINE] + diff[BEGIN_LINE] - 1;
		}
		else {
			sum[BEGIN_LINE] = NA;
		}
		if (diff[BEGIN_LINE] == 1) {
			if (base[BEGIN_COLUMN] > 0 && diff[BEGIN_COLUMN] > 0) {
				sum[BEGIN_COLUMN] = base[BEGIN_COLUMN] + diff[BEGIN_COLUMN] - 1;
			}
			else {
				sum[BEGIN_COLUMN] = NA;
			}
			if (base[BEGIN_BYTE] > 0 && diff[BEGIN_BYTE] > 0) {
				sum[BEGIN_BYTE] = base[BEGIN_BYTE] + diff[BEGIN_BYTE] - 1;
			}
			else {
				sum[BEGIN_BYTE] = NA;
			}
		}
		else {
			sum[BEGIN_COLUMN] = base[BEGIN_COLUMN];
			sum[BEGIN_BYTE] = base[BEGIN_BYTE];
		}
		
		if (base[BEGIN_LINE] > 0 && diff[END_LINE] > 0) {
			sum[END_LINE] = base[BEGIN_LINE] + diff[END_LINE] - 1;
		}
		else {
			sum[END_LINE] = NA;
		}
		if (diff[END_LINE] == 1) {
			if (base[BEGIN_COLUMN] > 0 && diff[END_COLUMN] > 0) {
				sum[END_COLUMN] = base[BEGIN_COLUMN] + diff[END_COLUMN] - 1;
			}
			else {
				sum[END_COLUMN] = NA;
			}
			if (base[BEGIN_BYTE] > 0 && diff[END_BYTE] > 0) {
				sum[END_BYTE] = base[BEGIN_BYTE] + diff[END_BYTE] - 1;
			}
			else {
				sum[END_BYTE] = NA;
			}
		}
		else {
			sum[END_COLUMN] = base[END_COLUMN];
			sum[END_BYTE] = base[END_BYTE];
		}
		
		return sum;
	}

}
