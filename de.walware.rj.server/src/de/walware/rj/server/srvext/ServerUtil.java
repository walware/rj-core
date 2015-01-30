/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.srvext;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.walware.rj.server.RjsStatus;


/**
 * Server utilities.
 */
public class ServerUtil {
	
	
	public static final String RJ_DATA_ID = "de.walware.rj.data";
	public static final String RJ_SERVER_ID = "de.walware.rj.server";
	
	public static final int[] RJ_VERSION = new int[] { 2, 0, 0 };
	
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
		final Set<Entry<?, ?>> entrySet = map.entrySet();
		for (final Entry<?, ?> entry : entrySet) {
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
	
	private static String encodeCodebaseEntry(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		URI uri= null;
		try {
			if (path.startsWith("file:")) {
				path= path.substring(5);
				uri= new URI("file", null, path, null);
			}
			else {
				if (File.separatorChar == '\\') {
					path= path.replace('\\', '/');
				}
				if (path.charAt(0) != '/') {
					path= '/' + path;
				}
				uri= new URI("file", null, path, null);
			}
			return uri.toString();
		}
		catch (final URISyntaxException e) {
			throw new IllegalArgumentException("Invalid entry for codebase", e);
		}
	}
	
	/**
	 * Concats the specified entries to a valid codebase property value.
	 * The entries have to be path in the local file system. It is recommend to specify the entries
	 * as URL with the schema 'file'.
	 */
	public static String concatCodebase(final String[] entries) {
		if (entries.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (final String entry : entries) {
			String path= encodeCodebaseEntry(entry);
			if (path != null) {
				sb.append(path);
				sb.append(' ');
			}
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
