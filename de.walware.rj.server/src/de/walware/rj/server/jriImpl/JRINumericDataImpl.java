/*******************************************************************************
 * Copyright (c) 2009-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
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

import de.walware.rj.data.defaultImpl.RNumericDataBImpl;


public class JRINumericDataImpl extends RNumericDataBImpl {
	
	
	public JRINumericDataImpl(final double[] values) {
		super(values);
	}
	
	public JRINumericDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		super(in);
	}
	
	
	public double[] getJRIValueArray() {
		if (this.realValues.length == this.length) {
			return this.realValues;
		}
		final double[] array = new double[this.length];
		System.arraycopy(this.realValues, 0, array, 0, this.length);
		return array;
	}
	
}
