/*=============================================================================#
 # Copyright (c) 2011-2016 Stephan Wahlbrink (WalWare.de) and others.
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
import de.walware.rj.data.RJIOExternalizable;


public class TracepointInstallationReport implements RJIOExternalizable {
	
	
	public static final int NOTFOUND= 1;
	public static final int FOUND_UNCHANGED= 2;
	public static final int FOUND_UNSET= 3;
	public static final int FOUND_SET= 4;
	
	
	private final int[] results;
	
	
	public TracepointInstallationReport(final int[] resultCodes) {
		this.results= resultCodes;
	}
	
	public TracepointInstallationReport(final RJIO io) throws IOException {
		this.results= io.readIntArray();
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		io.writeIntArray(this.results, this.results.length);
	}
	
	
	public int[] getInstallationResults() {
		return this.results;
	}
	
}
