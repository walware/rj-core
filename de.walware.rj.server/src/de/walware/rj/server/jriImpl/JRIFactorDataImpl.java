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

import de.walware.rj.data.RJIO;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RFactorDataImpl;


public class JRIFactorDataImpl extends RFactorDataImpl {
	
	
	public JRIFactorDataImpl(final int[] values, final boolean isOrdered, final String[] levelLabels) {
		super(values, isOrdered, levelLabels);
	}
	
	public JRIFactorDataImpl(final RJIO io) throws IOException {
		super(io);
	}
	
	
	@Override
	protected RCharacterDataImpl readLabels(final RJIO io) throws IOException {
		return new JRICharacterDataImpl(io);
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
