/*******************************************************************************
 * Copyright (c) 2010-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

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
