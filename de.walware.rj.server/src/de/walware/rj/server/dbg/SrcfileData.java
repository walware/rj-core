/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.dbg;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class SrcfileData implements RJIOExternalizable {
	
	
	private final String path;
	private final String name;
	private final long timestamp;
	
	
	public SrcfileData(final String path, final String name, final long timestamp) {
		this.path = path;
		this.name = name;
		this.timestamp = timestamp;
	}
	
	public SrcfileData(final RJIO io) throws IOException {
		this.path = io.readString();
		this.name = io.readString();
		this.timestamp = io.readLong();
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeString(this.path);
		io.writeString(this.name);
		io.writeLong(this.timestamp);
	}
	
	
	/**
	 * Returns the file path in the application (e.g. path in Eclipse workspace)
	 * @return the path or <code>null</code>
	 */
	public String getPath() {
		return this.path;
	}
	
	/**
	 * Returns the complete standardized file path usually compatible with 'filename' properties in
	 * R.
	 * @return the path or <code>null</code>
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the modification timestamp of the file compatible with 'timestamp' properties in R 
	 * (seconds, not milliseconds).
	 * 
	 * @return the timestamp or <code>0</code>
	 */
	public long getTimestamp() {
		return this.timestamp;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("SrcfileData");
		sb.append("\n\t").append("path= ").append(this.path);
		sb.append("\n\t").append("name= ").append(this.name);
		sb.append("\n\t").append("timestamp= ").append(this.timestamp);
		return sb.toString();
	}
	
}
