/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.IOException;
import java.io.ObjectOutput;


public interface MainCmdItem extends RjsComObject {
	
	public boolean waitForClient();
	public int getStatus();
	
	public void setAnswer(final int status);
	
	public int getOption();
	
	public Object getData();
	public String getDataText();
	
	public void writeExternal(ObjectOutput out) throws IOException;
	
}
