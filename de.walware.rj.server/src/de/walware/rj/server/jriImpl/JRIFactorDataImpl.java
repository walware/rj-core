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

import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;


public class JRIFactorDataImpl extends RFactorDataImpl {
	
	
	public JRIFactorDataImpl(final int[] values, final boolean isOrdered, final String[] levelLabels) {
		super(values, isOrdered, levelLabels);
	}
	
	public JRIFactorDataImpl(final ObjectInput in) throws IOException, ClassNotFoundException {
		super(in);
	}
	
	
	@Override
	protected RCharacterDataImpl readLabels(final ObjectInput in) throws IOException, ClassNotFoundException {
		return new JRICharacterDataImpl(in);
	}
	
	
	public int[] getJRIValueArray() {
		if (this.codes.length == this.length) {
			return this.codes;
		}
		final int[] array = new int[this.length];
		System.arraycopy(this.codes, 0, array, 0, this.length);
		return array;
	}
	
	public String[] getJRILevelsArray() {
		return ((JRICharacterDataImpl) this.codeLabels).getJRIValueArray();
	}
	
}
