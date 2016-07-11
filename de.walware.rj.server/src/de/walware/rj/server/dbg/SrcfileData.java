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
import java.util.Objects;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class SrcfileData implements RJIOExternalizable {
	
	
	private final String path;
	private final String name;
	private final long timestamp;
	
	
	public SrcfileData(final String path, final String name, final long timestamp) {
		this.path= path;
		this.name= name;
		this.timestamp= timestamp;
	}
	
	public SrcfileData(final RJIO io) throws IOException {
		this.path= io.readString();
		this.name= io.readString();
		this.timestamp= io.readLong();
	}
	
	@Override
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
	public int hashCode() {
		return Objects.hashCode(this.path) * 17
				^ Objects.hashCode(this.name)
				^ (int)(this.timestamp ^ (this.timestamp >>> 32));
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof SrcfileData) {
			final SrcfileData other= (SrcfileData) obj;
			return (Objects.equals(this.path, other.path)
					&& Objects.equals(this.name, other.name)
					&& this.timestamp == other.timestamp );
		}
		return super.equals(obj);
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb= new StringBuilder("SrcfileData"); //$NON-NLS-1$
		sb.append("\n\t" + "path= ").append(this.path); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n\t" + "name= ").append(this.name); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n\t" + "timestamp= ").append(this.timestamp); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}
	
}
