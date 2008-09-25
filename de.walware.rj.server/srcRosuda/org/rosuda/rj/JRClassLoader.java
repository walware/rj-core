/*******************************************************************************
 * Copyright (c) 2005-2008 RoSuDa (www.rosuda.org) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *    RoSuDa, University Augsburg - initial API and implementation
 *    Stephan Wahlbrink - adjustments to RJ
 *******************************************************************************/

package org.rosuda.rj;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class JRClassLoader extends URLClassLoader {
	
	private static JRClassLoader instance;
	
	public static JRClassLoader getRJavaClassLoader() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		synchronized (JRClassLoader.class) {
			if (instance != null) {
				return instance;
			}
			instance = (JRClassLoader) Class.forName("RJavaClassLoader").newInstance();
			return instance;
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger("org.rosuda.rjava");
	public static boolean verbose = false;
	
	public static void setDebug(final int level) {
		verbose = (level > 0);
		if (verbose) {
			LOGGER.setLevel(Level.ALL);
		}
	}
	
	private static final Pattern PATH_SPLITTER = Pattern.compile(Pattern.quote(File.pathSeparator));
	
	
	private static class UnixFile extends java.io.File {
		
		private static final long serialVersionUID = 6120112960030467453L;
		
		long lastModStamp;
		public Object cache;
		
		public UnixFile(final String fn) {
			super((separatorChar != '/') ? fn.replace('/', separatorChar) : fn);
			this.lastModStamp = 0;
		}
		
		public boolean hasChanged() {
			final long curMod = lastModified();
			return (curMod != this.lastModStamp);
		}
		
		public void update() {
			this.lastModStamp = lastModified();
		}
		
	}
	
	
	private String rJavaPath;
	private final HashMap<String, UnixFile> libMap;
	private final Vector<UnixFile> classPath;
	
	private final Set<String> defaultLibPath;
	
	private final boolean useSystem = true;
	private final boolean useSecond = false;
	
	
	protected JRClassLoader() {
		super(new URL[0], getSystemClassLoader());
		this.libMap = new HashMap<String, UnixFile>();
		this.classPath = new Vector<UnixFile>();
		this.defaultLibPath = new LinkedHashSet<String>();
		initLibPath();
		
		if (this.rJavaPath == null) {
			this.rJavaPath = System.getProperty("rjava.path");
			if (this.rJavaPath == null) {
				this.rJavaPath = searchLibrary("rJava");
			}
			if (this.rJavaPath == null) {
				final String message = "Path to rJava package not found. Use R_LIBS or java property 'rjava.path' to specify the location.";
				LOGGER.log(Level.SEVERE, message);
				throw new IllegalArgumentException(message);
			}
		}
		
		final String rJavaClassPath = System.getProperty("rjava.class.path");
		if (rJavaClassPath != null) {
			addClassPath(PATH_SPLITTER.split(rJavaClassPath));
		}
		
		String rJavaLibPath = System.getProperty("rjava.rjavalibs");
		if (rJavaLibPath == null) {
			rJavaLibPath = this.rJavaPath + "/java";
		}
		addClassPath(this.rJavaPath + "/java");
		UnixFile so = new UnixFile(rJavaLibPath + "/rJava.so");
		if (!so.exists()) {
			so = new UnixFile(rJavaLibPath + "/rJava.dll");
		}
		if (so.exists()) {
			this.libMap.put("rJava", so);
		}
		
		String jriLibPath = System.getProperty("rjava.jrilibs");
		if (jriLibPath == null) {
			jriLibPath = this.rJavaPath + "/jri";
		}
		UnixFile jri = new UnixFile(jriLibPath + "/libjri.so");
		final String rarch = System.getProperty("r.arch");
		if (rarch != null && rarch.length() > 0) {
			final UnixFile af = new UnixFile(jriLibPath + rarch + "/libjri.so");
			if (af.exists()) {
				jri = af;
			}
		}
		if (!jri.exists()) {
			jri = new UnixFile(jriLibPath + "/libjri.jnilib");
		}
		if (!jri.exists()) {
			jri = new UnixFile(jriLibPath + "/jri.dll");
		}
		if (jri.exists()) {
			this.libMap.put("jri", jri);
			if (verbose) {
				LOGGER.log(Level.CONFIG, "registered JRI: " + jri);
			}
		}
		
		final String jriJar1 = jriLibPath + "/JRI.jar";
		if (new UnixFile(jriJar1).exists()) {
			addClassPath(jriJar1);
		}
		else {
			final String jriJar2 = this.rJavaPath + "/jri/JRI.jar";
			if (!jriJar2.equals(jriJar1) && new UnixFile(jriJar2).exists()) {
				addClassPath(jriJar2);
			}
			else {
				if (verbose) {
					LOGGER.log(Level.WARNING, "JRI.jar not found and not added to classpath");
				}
			}
		}
	}
	
	private void initLibPath() {
		synchronized (this.defaultLibPath) {
			this.defaultLibPath.clear();
			String rHome = System.getenv("R_HOME");
			if (rHome != null) {
				if (rHome.length() == 0) {
					rHome = null;
				}
				else {
					rHome = rHome.replace(File.separatorChar, '/');
					if (rHome.charAt(rHome.length()-1) == '/') {
						rHome = rHome.substring(0, rHome.length()-1);
					}
				}
			}
			
			final String libsSite = System.getenv("R_LIBS_SITE");
			if (libsSite != null && libsSite.length() > 0) {
				final String[] l = PATH_SPLITTER.split(libsSite);
				for (int i = 0; i < l.length; i++) {
					this.defaultLibPath.add(l[i].replace(File.separatorChar, '/'));
				}
			}
			else if (rHome != null) {
				if (rHome.startsWith("/usr/lib")) {
					this.defaultLibPath.add("/usr/local/lib"+rHome.substring(8)+'/'+"site-library");
				}
				this.defaultLibPath.add(rHome+'/'+"site-library");
			}
			
			final String libsS = System.getenv("R_LIBS");
			if (libsS != null && libsS.length() > 0) {
				final String[] l = PATH_SPLITTER.split(libsS);
				for (int i = 0; i < l.length; i++) {
					this.defaultLibPath.add(l[i].replace(File.separatorChar, '/'));
				}
			}
			else if (rHome != null) {
				this.defaultLibPath.add(rHome+'/'+"library");
			}
			
			final String libsUser = System.getenv("R_LIBS_USER");
			if (libsUser != null && libsUser.length() > 0) {
				final String[] l = PATH_SPLITTER.split(libsUser);
				for (int i = 0; i < l.length; i++) {
					this.defaultLibPath.add(l[i].replace(File.separatorChar, '/'));
				}
			}
			
			if (verbose) {
				LOGGER.log(Level.CONFIG, "JR library path: " + this.defaultLibPath.toString());
			}
		}
	}
	
	private String searchLibrary(final String name) {
		synchronized (this.defaultLibPath) {
			for (final String l : this.defaultLibPath) {
				try {
					final String p = l+'/'+name;
					final File dir = new File(p);
					if (dir.exists() && dir.isDirectory()) {
						return p;
					}
				}
				catch (final Exception e) {}
			}
			return null;
		}
	}
	
	private String classNameToFile(final String cls) {
		// convert . to /
		return cls.replace('.', '/');
	}
	
	private InputStream findClassInJAR(final UnixFile jar, final String cl) {
		final String cfn = classNameToFile(cl) + ".class";
		
		if (jar.cache == null || jar.hasChanged()) {
			try {
				if (jar.cache != null) {
					((ZipFile) jar.cache).close();
				}
			} catch (final Exception tryCloseX) {
			}
			jar.update();
			try {
				jar.cache = new ZipFile(jar);
			} catch (final Exception zipCacheX) {
			}
			if (verbose) {
				LOGGER.log(Level.INFO, "Creating cache for " + jar);
			}
		}
		try {
			final ZipFile zf = (ZipFile) jar.cache;
			if (zf == null) {
				return null;
			}
			final ZipEntry e = zf.getEntry(cfn);
			if (e != null) {
				return zf.getInputStream(e);
			}
		} catch (final Exception e) {
			if (verbose) {
				LOGGER.log(Level.WARNING, "Exception/find "+cl+" in "+jar+".", e);
			}
		}
		return null;
	}
	
	private URL findInJARURL(final String jar, final String fn) {
		try {
			final ZipInputStream ins = new ZipInputStream(new FileInputStream(jar));
			
			ZipEntry e;
			while ((e = ins.getNextEntry()) != null) {
				if (e.getName().equals(fn)) {
					ins.close();
					try {
						return new URL("jar:"
								+ (new UnixFile(jar)).toURI().toURL().toString() + "!"
								+ fn);
					} catch (final Exception ex) {
					}
					break;
				}
			}
		} catch (final Exception e) {
			if (verbose) {
				LOGGER.log(Level.WARNING, "Exception/find "+fn+" in "+jar+".", e);
			}
		}
		return null;
	}
	
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		Class cl = null;
		if ("RJavaClassLoader".equals(name)) {
			return getClass();
		}
		if (this.useSystem) {
			try {
				cl = super.findClass(name);
				if (cl != null) {
					if (verbose) {
						LOGGER.log(Level.FINE, "Found class '" + name + "' using URL loader");
					}
					return cl;
				}
			}
			catch (final Exception e) {
				if (verbose) {
					LOGGER.log(Level.FINE, "URL Loader could not find class", e);
				}
				if (!this.useSecond && e instanceof ClassNotFoundException) {
					throw (ClassNotFoundException) e;
				}
			}
		}
		if (this.useSecond) {
			if (verbose) {
				System.out.println("RJavaClassLoader.findClass(\"" + name + "\")");
			}
			
			InputStream ins = null;
			
			for (final Enumeration<UnixFile> e = this.classPath.elements(); e.hasMoreElements();) {
				final UnixFile cp = e.nextElement();
				
				if (verbose) {
					System.out.println(" - trying class path \"" + cp + "\"");
				}
				try {
					ins = null;
					if (cp.isFile()) {
						ins = findClassInJAR(cp, name);
					}
					if (ins == null && cp.isDirectory()) {
						final UnixFile class_f = new UnixFile(cp.getPath() + "/"
								+ classNameToFile(name) + ".class");
						if (class_f.isFile()) {
							ins = new FileInputStream(class_f);
						}
					}
					if (ins != null) {
						int al = 128 * 1024;
						byte fc[] = new byte[al];
						int n = ins.read(fc);
						int rp = n;
						// System.out.println(" loading class file, initial n =
						// "+n);
						while (n > 0) {
							if (rp == al) {
								int nexa = al * 2;
								if (nexa < 512 * 1024) {
									nexa = 512 * 1024;
								}
								final byte la[] = new byte[nexa];
								System.arraycopy(fc, 0, la, 0, al);
								fc = la;
								al = nexa;
							}
							n = ins.read(fc, rp, fc.length - rp);
							// System.out.println(" next n = "+n+" (rp="+rp+",
							// al="+al+")");
							if (n > 0) {
								rp += n;
							}
						}
						ins.close();
						n = rp;
						if (verbose) {
							System.out.println("RJavaClassLoader: loaded class "
									+ name + ", " + n + " bytes");
						}
						cl = defineClass(name, fc, 0, n);
						// System.out.println(" - class = "+cl);
						return cl;
					}
				} catch (final Exception ex) {
					// System.out.println(" * won't work: "+ex.getMessage());
				}
			}
		}
		// System.out.println("=== giving up");
		if (cl == null) {
			throw (new ClassNotFoundException(name));
		}
		return cl;
	}
	
	@Override
	public URL findResource(final String name) {
		if (verbose) {
			LOGGER.entering("RJavaClassLoader", "findResource", name);
		}
		if (this.useSystem) {
			try {
				final URL u = super.findResource(name);
				if (u != null) {
					if (verbose) {
						LOGGER.log(Level.FINE, "Found resource '"+name+"' at '"+ u + "' using URL loader.");
					}
					return u;
				}
			}
			catch (final Exception e) {
			}
		}
		if (this.useSecond) {
			for (final Enumeration<UnixFile> e = this.classPath.elements(); e.hasMoreElements();) {
				final UnixFile cp = e.nextElement();
				
				try {
					if (cp.isFile()) {
						final URL u = findInJARURL(cp.getPath(), name);
						if (u != null) {
							if (verbose) {
								System.out.println(" - found in a JAR file, URL "
										+ u);
							}
							return u;
						}
					}
					if (cp.isDirectory()) {
						final UnixFile res_f = new UnixFile(cp.getPath() + "/" + name);
						if (res_f.isFile()) {
							if (verbose) {
								System.out.println(" - find as a file: " + res_f);
							}
							return res_f.toURI().toURL();
						}
					}
				} catch (final Exception iox) {
				}
			}
		}
		return null;
	}
	
	/** add a library to path mapping for a native library */
	public void addRLibrary(final String name, final String path) {
		this.libMap.put(name, new UnixFile(path));
	}
	
	public void addClassPath(final String cp) {
		final UnixFile f = new UnixFile(cp);
		if (this.useSystem) {
			try {
				addURL(f.toURI().toURL());
				// return; // we need to add it anyway
			}
			catch (final Exception e) {
			}
		}
		if (this.useSecond) {
			if (!this.classPath.contains(f)) {
				this.classPath.add(f);
				System.setProperty("java.class.path", System.getProperty("java.class.path")
						+ File.pathSeparator + f.getPath());
			}
		}
	}
	
	public void addClassPath(final String[] cp) {
		int i = 0;
		while (i < cp.length) {
			addClassPath(cp[i++]);
		}
	}
	
	public String[] getClassPath() {
		final int j = this.classPath.size();
		final String[] s = new String[j];
		int i = 0;
		while (i < j) {
			s[i] = (this.classPath.elementAt(i)).getPath();
			i++;
		}
		return s;
	}
	
	@Override
	protected String findLibrary(final String name) {
		if (verbose) {
			LOGGER.entering("RJavaClassLoader", "findLibrary", name);
		}
		
		// if (name.equals("rJava"))
		// return rJavaLibPath+"/"+name+".so";
		
		final UnixFile u = this.libMap.get(name);
		String s = null;
		if (u != null && u.exists()) {
			s = u.getPath();
		}
		if (verbose) {
			LOGGER.log(Level.FINE, "Mapping to " + ((s == null) ? "<none>" : s));
		}
		
		return s;
	}
	
	public Class<?> loadRJavaClass(final String name) throws ClassNotFoundException {
		final Class<?> clazz = findClass(name);
		resolveClass(clazz);
		return clazz;
	}
	
}
