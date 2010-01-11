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

import de.walware.rj.data.defaultImpl.RRawDataImpl;


public class JRIRawDataImpl extends RRawDataImpl {
	
	
	public JRIRawDataImpl(final byte[] values) {
		super(values);
	}
	
	public JRIRawDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		super(in);
	}
	
	
	public byte[] getJRIValueArray() {
		if (this.byteValues.length == this.length) {
			return this.byteValues;
		}
		final byte[] array = new byte[this.length];
		System.arraycopy(this.byteValues, 0, array, 0, this.length);
		return array;
	}
	
}
