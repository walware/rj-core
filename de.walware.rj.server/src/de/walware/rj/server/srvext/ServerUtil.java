/*******************************************************************************
 * Copyright (c) 2009-2012 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import de.walware.rj.RjInvalidConfigurationException;
import de.walware.rj.server.RjsStatus;


/**
 * Server utilities.
 */
public class ServerUtil {
	
	
	public static final String RJ_DATA_ID = "de.walware.rj.data";
	public static final String RJ_SERVER_ID = "de.walware.rj.server";
	
	public static final int[] RJ_VERSION = new int[] { 1, 1, 0 };
	
	public static final RjsStatus MISSING_ANSWER_STATUS = new RjsStatus(RjsStatus.ERROR, 121, "Server error (missing answer).");
	
	
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
			return Collections.emptyList();
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
	
	public static void prettyPrint(final Collection list, final StringBuilder sb) {
		final String sep = System.getProperty("line.separator")+"\t";
		for (final Object value : list) {
			sb.append(sep);
			if (value != null) {
				sb.append(value);
			}
		}
	}
	
	public static void prettyPrintVersion(final int[] version, final StringBuilder sb) {
		if (version == null || version.length == 0) {
			sb.append("<missing>");
		}
		else {
			sb.append(version[0]);
			for (int i = 1; i < version.length; i++) {
				sb.append('.');
				sb.append(version[i]);
			}
		}
	}
	
	
	private static class PathEntry implements Comparable<PathEntry> {
		final File dir;
		final String child;
		public PathEntry(final File dir, final String child) {
			this.dir = dir;
			this.child = child;
		}
		public int compareTo(final PathEntry o) {
			return this.child.compareTo((o).child);
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
					if (list != null) {
						for (final String child : list) {
							files.add(new PathEntry(dir, child));
						}
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
		for (final String entry : entries) {
			sb.append(new File(entry).toURI().toString());
			sb.append(' ');
		}
		return sb.substring(0, sb.length()-1);
	}
	
	public static boolean delDir(final File dir) {
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
		return dir.delete();
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
	}
	
}
