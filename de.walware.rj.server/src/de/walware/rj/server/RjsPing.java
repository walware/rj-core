/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * Command for simple ping. Should be answered directly with a {@link RjsStatus}.
 */
public final class RjsPing implements RjsComObject, Externalizable {
	
	
	public static final RjsPing INSTANCE = new RjsPing();
	
	
	public RjsPing() {
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
	}
	
	
	public int getComType() {
		return RjsComObject.T_PING;
	}
	
	
	@Override
	public int hashCode() {
		return INSTANCE.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof RjsPing);
	}
	
}
