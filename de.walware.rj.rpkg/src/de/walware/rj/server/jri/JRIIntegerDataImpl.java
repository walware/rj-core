/*******************************************************************************
 * Copyright (c) 2009-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
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
import de.walware.rj.data.defaultImpl.RIntegerDataImpl;


public class JRIIntegerDataImpl extends RIntegerDataImpl {
	
	
	public JRIIntegerDataImpl(final int[] values) {
		super(values);
	}
	
	public JRIIntegerDataImpl(final int[] values, final int length) {
		super(values, length);
	}
	
	public JRIIntegerDataImpl(final RJIO io) throws IOException {
		super(io);
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
