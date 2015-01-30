/*=============================================================================#
 # Copyright (c) 2012-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.renv;


/**
 * Basic immutable R package, implementation of {@link IRPkg}.
 * 
 * @since 2.0
 */
public class RPkg implements IRPkg {
	
	
	private final String name;
	
	private final RNumVersion version;
	
	
	public RPkg(final String name, final RNumVersion version) {
		if (name == null) {
			throw new NullPointerException("name"); //$NON-NLS-1$
		}
		if (version == null) {
			throw new NullPointerException("version"); //$NON-NLS-1$
		}
		this.name= name;
		this.version= version;
	}
	
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public RNumVersion getVersion() {
		return this.version;
	}
	
	
	@Override
	public int hashCode() {
		return this.name.hashCode() + this.version.hashCode() * 7;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IRPkg)) {
			return false;
		}
		final IRPkg other= (IRPkg) obj;
		return (this.name.equals(other.getName()) && this.version.equals(other.getVersion()));
	}
	
	
	@Override
	public String toString() {
		if (this.version == RNumVersion.NONE) {
			return this.name;
		}
		final StringBuilder sb= new StringBuilder(this.name.length() + this.version.toString().length() + 12);
		sb.append(this.name);
		sb.append(" (" + "version= ").append(this.version.toString());
		sb.append(')');
		return sb.toString();
	}
	
}
