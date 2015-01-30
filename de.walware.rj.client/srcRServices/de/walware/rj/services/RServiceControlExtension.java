/*=============================================================================#
 # Copyright (c) 2011-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * 
 * @provisional
 */
public interface RServiceControlExtension {
	
	/**
	 * Adds a cancel handler called when the tool is canceled to the stack of cancel handlers.
	 * <p>
	 * The cancel handler should return <code>true</code> if the cancel event was handled
	 * completely and the other handlers will not be called.</p>
	 * 
	 * @param handler the handler
	 */
	void addCancelHandler(Callable<Boolean> handler);
	
	/**
	 * Removes a cancel handler from the stack of cancel handlers.
	 * 
	 * @param handler the handler
	 */
	void removeCancelHandler(Callable<Boolean> handler);
	
	/**
	 * The lock for wait operations.
	 * 
	 * @return the lock
	 */
	Lock getWaitLock();
	
	/**
	 * Waits in the current tool thread.
	 * <p>
	 * If short background operations are waiting for execution, they are executed (depends on
	 * implementation).</p>
	 * <p>
	 * The current thread must hold the lock {@link #getWaitLock()}. The method returns after a 
	 * short waiting time, operations are executed <b>or</b> {@link #resume()} is called.</p>
	 * 
	 * @param monitor the current monitor
	 */
	void waitingForUser(IProgressMonitor monitor);
	
	/**
	 * Resumes the tool thread which is waiting in {@link #waitingForUser(IProgressMonitor)}.
	 */
	void resume();
	
}
