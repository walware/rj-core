/*=============================================================================#
 # Copyright (c) 2013-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.srvext;

import static de.walware.rj.server.srvext.ServerUtil.RJ_SERVER_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import de.walware.rj.RjInvalidConfigurationException;


public class RJContext {
	
	
	protected static abstract class PathEntry implements Comparable<PathEntry> {
		
		final String name;
		
		public PathEntry(final String child) {
			this.name = child;
		}
		
		@Override
		public int compareTo(final PathEntry o) {
			return this.name.compareTo((o).name);
		}
		
		public abstract String getPath();
		
		@Override
		public abstract String toString();
		
	}
	
	protected static class FilePathEntry extends PathEntry {
		
		final File dir;
		
		public FilePathEntry(final File dir, final String name) {
			super(name);
			this.dir = dir;
		}
		
		@Override
		public String getPath() {
			final File file = new File(this.dir, this.name);
			if (file.isDirectory()) {
				final File binFile = new File(file, "bin");
				if (binFile.exists() && binFile.isDirectory()) {
					return binFile.getPath();
				}
			}
			return file.getPath();
		}
		
		@Override
		public String toString() {
			return '\'' + this.name + "' in '" + this.dir + '\'';
		}
		
	}
	
	
	private String libPath;
	
	
	public RJContext(final String libPath) {
		this.libPath = libPath;
	}
	
	protected RJContext() {
	}
	
	
	public String[] searchRJLibs(final String[] libsIds) throws RjInvalidConfigurationException {
		final List<PathEntry> candidates = getRJLibCandidates();
		
		final String[] resolved = new String[libsIds.length];
		StringBuilder sb = null;
		
		Collections.sort(candidates);
		for (int i = 0; i < libsIds.length; i++) {
			final PathEntry entry = searchLib(candidates, libsIds[i]);
			if (entry == null) {
				if (sb == null) {
					sb = new StringBuilder("Missing RJ library ");
				}
				else {
					sb.append(", ");
				}
				sb.append('\'');
				sb.append(libsIds[i]);
				sb.append('\'');
			}
			else {
				resolved[i] = entry.getPath();
			}
		}
		if (sb != null) {
			sb.append('.');
			throw new RjInvalidConfigurationException(sb.toString());
		}
		return resolved;
	}
	
	protected String[] getLibDirPaths() throws RjInvalidConfigurationException {
		String path = System.getProperty("de.walware.rj.path");
		if (path == null) {
			path = this.libPath; 
		}
		if (path == null || path.isEmpty()) {
			throw new RjInvalidConfigurationException("Missing or invalid RJ library location.");
		}
		return path.split(Pattern.quote(File.pathSeparator));
	}
	
	protected List<PathEntry> getRJLibCandidates() throws RjInvalidConfigurationException {
		final String[] paths = getLibDirPaths();
		
		final List<PathEntry> files = new ArrayList<PathEntry>(paths.length*10);
		for (int i = 0; i < paths.length; i++) {
			if (paths[i].length() > 0) {
				File dir = new File(paths[i]);
				try {
					dir = dir.getCanonicalFile();
				}
				catch (final IOException e) {}
				final String[] list = dir.list();
				if (list != null) {
					for (final String child : list) {
						files.add(new FilePathEntry(dir, child));
					}
				}
			}
		}
		
		return files;
	}
	
	protected PathEntry searchLib(final List<PathEntry> files, final String libId) {
		PathEntry found = null;
		for (final PathEntry entry : files) {
			if (entry.name.startsWith(libId)) {
				// without version
				if (entry.name.length() == libId.length() // equals
						|| (entry.name.length() == libId.length() + 4 && entry.name.endsWith(".jar")) ) {
					return entry;
				}
				// with version suffix
				if (entry.name.length() > libId.length()) {
					if (entry.name.charAt(libId.length()) == '_') {
						found = entry;
					}
				}
			}
		}
		return found;
	}
	
	
	public String getServerPolicyFilePath() throws RjInvalidConfigurationException {
		String serverLib = searchRJLibs(new String[] { RJ_SERVER_ID })[0];
		final File libFile = new File(serverLib);
		if (libFile.isDirectory()) {
			File file = new File(libFile, "localhost.policy");
			if (libFile.getName().equals("bin") && !file.exists()) {
				file = new File(libFile.getParentFile(), "localhost.policy");
			}
			return file.toURI().toString();
		}
		// expect jar file
		serverLib = libFile.toURI().toString();
		return "jar:" + serverLib + "!/localhost.policy";
	}
	
	
	protected String getPropertiesDirPath() {
		return System.getProperty("user.dir");
	}
	
	protected InputStream getInputStream(final String path) throws IOException {
		final File file = new File(path);
		if (!file.exists()) {
			return null;
		}
		return new FileInputStream(file);
	}
	
	protected OutputStream getOutputStream(final String path) throws IOException {
		final File file = new File(path);
		return new FileOutputStream(file, false);
	}
	
	public Properties loadProperties(final String name) throws IOException {
		if (name == null) {
			throw new NullPointerException("name");
		}
		final String path = getPropertiesDirPath() + "/" + name + ".properties";
		final InputStream in = getInputStream(path);
		if (in == null) {
			return null;
		}
		
		final Properties properties = new Properties();
		try {
			properties.load(in);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (final IOException e) {}
			}
		}
		
		return properties;
	}
	
	public void saveProperties(final String name, final Properties properties) throws IOException {
		if (name == null) {
			throw new NullPointerException("name");
		}
		if (properties == null) {
			throw new NullPointerException("properties");
		}
		final String path = getPropertiesDirPath() + "/" + name + ".properties";
		final OutputStream out = getOutputStream(path);
		
		try {
			properties.store(out, null);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (final IOException e) {}
			}
		}
	}
	
}
