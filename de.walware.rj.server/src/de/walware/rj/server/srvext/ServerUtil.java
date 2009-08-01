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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.walware.rj.RjInvalidConfigurationException;


public class ServerUtil {
	
	
	public static final String RJ_DATA = "de.walware.rj.data";
	public static final String RJ_SERVER = "de.walware.rj.server";
	public static final String RJ_SERVI = "de.walware.rj.servi";
	
	
	private static final Pattern URL_SPACES = Pattern.compile("\\ ");
	
	
	/**
	 * Split at next ':'
	 * @param arg the argument to split
	 * @return String array of length 2
	 */
	public static String[] getArgSubValue(final String arg) {
		final String[] args = new String[2];
		if (arg != null && arg.length() > 0) {
			final int idx = arg.indexOf(':');
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
	public static String[] getArgConfigValue(final String arg) {
		final String[] args = new String[2];
		if (arg != null && arg.length() > 0) {
			final int idx = arg.indexOf('=');
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
	public static List<String> getArgValueList(final String arg) {
		if (arg != null && arg.length() > 0) {
			return Arrays.asList(arg.split(","));
		}
		else {
			return Arrays.asList(new String[0]);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void prettyPrint(final Map map, final StringBuilder sb) {
		final String sep = System.getProperty("line.separator")+"\t";
		final Set<Entry> entrySet = map.entrySet();
		for (final Entry entry : entrySet) {
			sb.append(sep);
			sb.append(entry.getKey());
			sb.append('=');
			final Object value = entry.getValue();
			if (value != null) {
				sb.append(value);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void prettyPrint(final Collection list, final StringBuilder sb) {
		final String sep = System.getProperty("line.separator")+"\t";
		for (final Object value : list) {
			sb.append(sep);
			if (value != null) {
				sb.append(value);
			}
		}
	}
	
	
	private static class PathEntry implements Comparable {
		final File dir;
		final String child;
		public PathEntry(final File dir, final String child) {
			this.dir = dir;
			this.child = child;
		}
		public int compareTo(final Object o) {
			return this.child.compareTo(((PathEntry) o).child);
		}
	}
	
	public static PathEntry searchLib(final List<PathEntry> files, final String baseName) {
		PathEntry found = null;
		for (final PathEntry entry : files) {
			if (entry.child.startsWith(baseName)) {
				if (entry.child.length() == baseName.length()) {
					return entry;
				}
				if (entry.child.charAt(baseName.length()) == '_') {
					found = entry;
				}
			}
		}
		return found;
	}
	
	private static String check(final PathEntry entry) {
		final File file = new File(entry.dir, entry.child);
		if (file.isDirectory()) {
			final File binFile = new File(file, "bin");
			if (binFile.exists() && binFile.isDirectory()) {
				return binFile.getPath();
			}
		}
		return file.getPath();
	}
	
	public static String[] searchRJLibs(final String libPath, final String[] libs) throws RjInvalidConfigurationException {
		String path = System.getProperty("de.walware.rj.path");
		if (path == null) {
			path = libPath; 
		}
		
		if (path != null && path.length() > 0) {
			final String[] resolved = new String[libs.length];
			final String[] paths = path.split(Pattern.quote(File.pathSeparator));
			final List<PathEntry> files = new ArrayList<PathEntry>(paths.length*10);
			for (int i = 0; i < paths.length; i++) {
				if (paths[i].length() > 0) {
					final File dir = new File(paths[i]);
					final String[] list = dir.list();
					for (final String child : list) {
						files.add(new PathEntry(dir, child));
					}
				}
			}
			
			StringBuilder sb = null;
			if (!files.isEmpty()) {
				Collections.sort(files);
				for (int i = 0; i < libs.length; i++) {
					final PathEntry entry = searchLib(files, libs[i]);
					if (entry == null) {
						if (sb == null) {
							sb = new StringBuilder("Missing RJ library ");
						}
						else {
							sb.append(',');
						}
						sb.append(" '");
						sb.append(libs[i]);
						sb.append("'");
					}
					else {
						resolved[i] = check(entry);
					}
				}
			}
			if (sb != null) {
				sb.append('.');
				throw new RjInvalidConfigurationException(sb.toString());
			}
			return resolved;
		}
		throw new RjInvalidConfigurationException("Missing or invalid RJ library location.");
	}
	
	
	public static String concatPathVar(final String[] entries) {
		if (entries.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (final String entry : entries) {
			sb.append(entry);
			sb.append(File.pathSeparatorChar);
		}
		return sb.substring(0, sb.length()-1);
	}
	
	public static String concatCodebase(final String[] entries) {
		if (entries.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (String entry : entries) {
			entry = URL_SPACES.matcher(entry).replaceAll("%20");
			sb.append("file://");
			if (entry.charAt(0) != '/') {
				entry = '/' + entry;
			}
			if (entry.charAt(entry.length()-1) != '/' && new File(entry).isDirectory()) {
				entry = entry + '/';
			}
			sb.append(entry);
			sb.append(' ');
		}
		return sb.substring(0, sb.length()-1);
	}
	
	public static void delDir(final File dir) {
		final String[] children = dir.list();
		for (final String child : children) {
			final File file = new File(dir, child);
			if (file.isDirectory()) {
				delDir(file);
			}
			else {
				file.delete();
			}
		}
		dir.delete();
	}
	
	public static void cleanDir(final File dir, final String exclude) {
		final String[] children = dir.list();
		for (final String child : children) {
			if (child.equals(exclude)) {
				continue;
			}
			final File file = new File(dir, child);
			if (file.isDirectory()) {
				delDir(file);
			}
			else {
				file.delete();
			}
		}
		dir.delete();
	}
	
}
