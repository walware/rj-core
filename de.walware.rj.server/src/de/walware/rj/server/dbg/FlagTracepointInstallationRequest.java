/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.dbg;

import java.io.IOException;

import de.walware.rj.data.RJIO;


public class FlagTracepointInstallationRequest extends TracepointInstallationRequest {
	
	
	private final byte[] types;
	private final int[] flags;
	
	
	public FlagTracepointInstallationRequest(final byte[] types, final int[] flags) {
		if (types.length != flags.length) {
			throw new IllegalArgumentException("types.length != flags.length");
		}
		this.types= types;
		this.flags= flags;
	}
	
	public FlagTracepointInstallationRequest(final RJIO in) throws IOException {
		final int l= in.readInt();
		this.types= in.readByteData(new byte[l], l);
		this.flags= in.readIntData(new int[l], l);
	}
	
	@Override
	public void writeExternal(final RJIO out) throws IOException {
		final int l= this.types.length;
		out.writeInt(l);
		out.writeByteData(this.types, l);
		out.writeIntData(this.flags, l);
	}
	
	
	public byte[] getTypes() {
		return this.types;
	}
	
	public int[] getFlags() {
		return this.flags;
	}
	
}
