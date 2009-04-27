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


public interface RjsStatus {
	
	
	public static final int OK =               0x0;
	public static final int INFO =             0x1;
	public static final int WARNING =          0x2;
	public static final int ERROR =            0x4;
	public static final int CANCEL =           0x8;
	
	
	public static final RjsStatusImpl1 OK_STATUS = new RjsStatusImpl1(OK, 0);
	
	
	public int getSeverity();
	
	public int getCode();
	
	public String getTextDetail();
	
}
