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

package de.walware.rj.server.jriImpl;

import java.io.IOException;
import java.io.ObjectInput;

import de.walware.rj.data.defaultImpl.RIntegerDataImpl;


public class JRIIntegerDataImpl extends RIntegerDataImpl {
	
	
	public JRIIntegerDataImpl(final int[] values) {
		super(values);
	}
	
	public JRIIntegerDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		super(in);
	}
	
	
	public int[] getJRIValueArray() {
		if (this.intValues.length == this.length) {
			return this.intValues;
		}
		final int[] array = new int[this.length];
		System.arraycopy(this.intValues, 0, array, 0, this.length);
		return array;
	}
	
}
