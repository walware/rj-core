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

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.defaultImpl.RRawDataImpl;


public class JRIRawDataImpl extends RRawDataImpl {
	
	
	public JRIRawDataImpl(final byte[] values) {
		super(values);
	}
	
	public JRIRawDataImpl(final RJIO io) throws IOException {
		super(io);
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
