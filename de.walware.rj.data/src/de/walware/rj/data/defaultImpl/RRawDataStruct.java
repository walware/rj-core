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

package de.walware.rj.data.defaultImpl;

import de.walware.rj.data.RStore;


public class RRawDataStruct extends AbstractRawData {
	
	
	public RRawDataStruct() {
		this.length = -1;
	}
	
	
	@Override
	protected final boolean isStructOnly() {
		return true;
	}
	
	
	public boolean isMissing(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	public Byte get(final int idx) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final Byte[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	public boolean allEqual(final RStore other) {
		return (other.getStoreType() == RAW && other.getLength() == -1);
	}
	
}
