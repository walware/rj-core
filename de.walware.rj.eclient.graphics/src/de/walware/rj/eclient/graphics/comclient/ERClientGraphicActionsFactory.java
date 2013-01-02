/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics.comclient;

import de.walware.rj.server.client.AbstractRJComClient;
import de.walware.rj.server.client.AbstractRJComClientGraphicActions;

import de.walware.ecommons.ts.ITool;


public class ERClientGraphicActionsFactory implements AbstractRJComClientGraphicActions.Factory {
	
	
	public AbstractRJComClientGraphicActions create(final AbstractRJComClient rjs, final Object rHandle) {
		return new ERClientGraphicActions(rjs, (rHandle instanceof ITool) ? (ITool) rHandle : null);
	}
	
}
