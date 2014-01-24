/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.client;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


class DaemonThreadFactory implements ThreadFactory {
	
	
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final String namePrefix;
	final String nameSuffix = "]";
	
	
	public DaemonThreadFactory(final String name) {
		this.namePrefix = name + " [Thread-";
	}
	
	
	@Override
	public Thread newThread(final Runnable r) {
		final Thread t = new Thread(r,
				this.namePrefix + this.threadNumber.getAndIncrement() + this.nameSuffix);
		
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
	
}
