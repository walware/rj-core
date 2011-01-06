/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.defaultImpl.RLogicalDataIntImpl;


public class JRILogicalDataImpl extends RLogicalDataIntImpl {
	
	
	public JRILogicalDataImpl(final int[] value) {
		super(value);
	}
	
	public JRILogicalDataImpl(final RJIO io) throws IOException {
		super(io);
	}
	
	
	public int[] getJRIValueArray() {
		if (this.boolValues.length == this.length) {
			return this.boolValues;
		}
		final int[] array = new int[this.length];
		System.arraycopy(this.boolValues, 0, array, 0, this.length);
		return array;
	}
	
}
