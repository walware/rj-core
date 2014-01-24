/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.graphics.comclient;

import de.walware.ecommons.ts.ITool;

import de.walware.rj.server.client.AbstractRJComClient;
import de.walware.rj.server.client.AbstractRJComClientGraphicActions;


public class ERClientGraphicActionsFactory implements AbstractRJComClientGraphicActions.Factory {
	
	
	@Override
	public AbstractRJComClientGraphicActions create(final AbstractRJComClient rjs, final Object rHandle) {
		return new ERClientGraphicActions(rjs, (rHandle instanceof ITool) ? (ITool) rHandle : null);
	}
	
}
