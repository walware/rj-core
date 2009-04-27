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

package de.walware.rj.server.srvext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


public class ServerUtil {
	
	
	/**
	 * Split at next ':'
	 * @param arg the argument to split
	 * @return String array of length 2
	 */
	public static String[] getArgSubValue(String arg) {
		String[] args = new String[2];
		if (arg != null && arg.length() > 0) {
			int idx = arg.indexOf(':');
			if (idx >= 0) {
				args[0] = arg.substring(0, idx);
				args[1] = arg.substring(idx+1);
			}
			else {
				args[0] = arg;
			}
		}
		else {
			args[0] = "";
		}
		return args;
	}
	
	/**
	 * Split at next '='
	 * @param arg the argument to split
	 * @return String array of length 2
	 */
	public static String[] getArgConfigValue(String arg) {
		String[] args = new String[2];
		if (arg != null && arg.length() > 0) {
			int idx = arg.indexOf('=');
			if (idx >= 0) {
				args[0] = arg.substring(0, idx);
				args[1] = arg.substring(idx+1);
			}
			else {
				args[0] = arg;
			}
		}
		else {
			args[0] = "";
		}
		return args;
	}
	
	/**
	 * Split at ','
	 * @param arg the argument to split
	 * @return List with String
	 */
	public static List<String> getArgValueList(String arg) {
		if (arg != null && arg.length() > 0) {
			return Arrays.asList(arg.split(","));
		}
		else {
			return Arrays.asList(new String[0]);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void prettyPrint(Map map, StringBuilder sb) {
		String sep = System.getProperty("line.separator")+"\t";
		Set<Entry> entrySet = map.entrySet();
		for (Entry entry : entrySet) {
			sb.append(sep);
			sb.append(entry.getKey());
			sb.append('=');
			Object value = entry.getValue();
			if (value != null) {
				sb.append(value);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void prettyPrint(Collection list, StringBuilder sb) {
		String sep = System.getProperty("line.separator")+"\t";
		for (Object value : list) {
			sb.append(sep);
			if (value != null) {
				sb.append(value);
			}
		}
	}
	
}
