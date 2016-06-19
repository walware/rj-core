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
import java.util.ArrayList;
import java.util.List;

import de.walware.rj.data.RJIO;


public class ElementTracepointInstallationRequest extends TracepointInstallationRequest {
	
	
	private final List<? extends ElementTracepointPositions> requests;
	
	
	public ElementTracepointInstallationRequest(
			final List<? extends ElementTracepointPositions> checkedList) {
		this.requests= checkedList;
	}
	
	public ElementTracepointInstallationRequest(final RJIO io) throws IOException {
		final int l= io.readInt();
		final List<ElementTracepointPositions> list= new ArrayList<>(l);
		for (int i= 0; i < l; i++) {
			list.add(new ElementTracepointPositions(io));
		}
		this.requests= list;
	}
	
	@Override
	public void writeExternal(final RJIO io) throws IOException {
		final int l= this.requests.size();
		io.writeInt(l);
		for (int i= 0; i < l; i++) {
			this.requests.get(i).writeExternal(io);
		}
	}
	
	
	public List<? extends ElementTracepointPositions> getRequests() {
		return this.requests;
	}
	
}
