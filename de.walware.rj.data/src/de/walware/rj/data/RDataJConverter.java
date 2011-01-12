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

package de.walware.rj.data;



/**
 * Converts R data objects to classic Java objects and the other way around.
 */
public class RDataJConverter {
	
	
	private boolean keepArray1;
	
	private RObjectFactory rObjectFactory;
	
	
	public void setKeepArray1(final boolean enable) {
		this.keepArray1 = enable;
	}
	
	public void setRObjectFactory(final RObjectFactory factory) {
		this.rObjectFactory = factory;
	}
	
	public RObjectFactory getRObjectFactory() {
		return this.rObjectFactory;
	}
	
	
	public Object toJava(final RObject rObject) {
		switch (rObject.getRObjectType()) {
		case RObject.TYPE_VECTOR:
			return toJava(rObject.getData());
		}
		return null;
	}
	
	public Object toJava(final RStore rData) {
		final Object[] array = rData.toArray();
		if (!this.keepArray1 && array != null && array.length == 1) {
			return array[0];
		}
		return array;
	}
	
	
	
	public RObject toRJ(final Object javaObj) {
		if (javaObj instanceof Boolean) {
			return this.rObjectFactory.createVector(
					this.rObjectFactory.createLogiData(new boolean[] {
							((Boolean) javaObj).booleanValue() }));
		}
		if (javaObj instanceof String) {
			return this.rObjectFactory.createVector(
					this.rObjectFactory.createCharData(new String[] {
							(String) javaObj }));
		}
		if (javaObj instanceof String[]) {
			return this.rObjectFactory.createVector(
					this.rObjectFactory.createCharData((String[]) javaObj) );
		}
		return null;
	}
	
}
