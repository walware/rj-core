/*******************************************************************************
 * Copyright (c) 2012-2013 WalWare OpenSource (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.walware.ecommons.ts.IToolCommandHandler;
import de.walware.ecommons.ts.IToolService;


/**
 * @since 1.2
 */
public abstract class AbstractRToolCommandHandler implements IToolCommandHandler {
	
	
	@Override
	public IStatus execute(String id, IToolService service, Map<String, Object> data,
			IProgressMonitor monitor) throws CoreException {
		return execute(id, (IRToolService) service, data, monitor);
	}
	
	protected abstract IStatus execute(String id, IRToolService r, Map<String, Object> data,
			IProgressMonitor monitor) throws CoreException;
	
}
