/*******************************************************************************
 * Copyright (c) 2005-2010 RoSuDa (www.rosuda.org) and others.
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *    RoSuDa, University Augsburg - initial API and implementation
 *******************************************************************************/

/* package (default) */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Logger;

import org.rosuda.rj.JRClassLoader;


public class RJavaClassLoader extends JRClassLoader {
	
	
	public static RJavaClassLoader primaryLoader = null;
	
	/**
	 * Returns the singleton instance of RJavaClassLoader
	 */ 
	public static RJavaClassLoader getPrimaryLoader() {
		return primaryLoader;
	}
	
	
	public RJavaClassLoader(final String a, final String b) {
		this();
	}
	
	public RJavaClassLoader() {
		super();
		
		if (primaryLoader == null) {
			primaryLoader = this;
		} else {
			Logger.getLogger("org.rosuda.rjava").warning("Non-primary instance of RJavaClassLoader created");
		}
	}
	
	
	public void bootClass(final String cName, final String mName, final String[] args) throws java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException, java.lang.NoSuchMethodException, java.lang.ClassNotFoundException {
		throw new UnsupportedOperationException();
	}
	
	
	
	
	//----- tools -----
	
	static class RJavaObjectInputStream extends ObjectInputStream {
		public RJavaObjectInputStream(final InputStream in) throws IOException {
			super(in);
		}
		@Override
		protected Class resolveClass(final ObjectStreamClass desc) throws ClassNotFoundException {
			return Class.forName(desc.getName(), false, RJavaClassLoader.getPrimaryLoader());
		}
	}
	
	/** 
	 * Serialize an object to a byte array. (code by CB)
	 *
	 * @param object object to serialize
	 * @return byte array that represents the object
	 * @throws Exception 
	 */
	public static byte[] toByte(final Object object) throws Exception {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(object);
		oos.close();
		return os.toByteArray();
	}
	
	/** 
	 * Deserialize an object from a byte array. (code by CB)
	 *
	 * @param byteArray
	 * @return the object that is represented by the byte array
	 * @throws Exception 
	 */
	public Object toObject(final byte[] byteArray) throws Exception {
		final InputStream is = new ByteArrayInputStream(byteArray);
		final RJavaObjectInputStream ois = new RJavaObjectInputStream(is);
		final Object o = ois.readObject();
		ois.close();
		return o;
	}
	
	/**
	 * converts the byte array into an Object using the primary RJavaClassLoader
	 */
	public static Object toObjectPL(final byte[] byteArray) throws Exception{
		return RJavaClassLoader.getPrimaryLoader().toObject(byteArray);
	}
	
}
