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

package de.walware.rj.services;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.osgi.framework.Version;


/**
 * Information about the platform R is running on
 * and the running R version.
 * 
 * The properties usually doesn't change for a single RService
 * instance.
 */
public final class RPlatform implements Externalizable {
	
	/**
	 * OS type constant for windows operation systems
	 */
	public static final String OS_WINDOWS = "windows";
	
	/**
	 * OS type constant for unix operation systems
	 */
	public static final String OS_UNIX = "unix";
	
	
	private String osType;
	
	private String fileSep;
	private String pathSep;
	
	private String versionString;
	private transient Version version;
	
	private String osName;
	private String osArch;
	private String osVersion;
	
	
	public RPlatform() {
	}
	
	public RPlatform(final String osType, final String fileSep, final String pathSep,
			final String version,
			final String osName, final String osArch, final String osVersion) {
		this.osType = osType;
		this.fileSep = fileSep;
		this.pathSep = pathSep;
		this.versionString = version;
		
		this.osName = osName;
		this.osArch = osArch;
		this.osVersion = osVersion;
	}
	
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.osType = in.readUTF();
		this.fileSep = in.readUTF();
		this.pathSep = in.readUTF();
		this.versionString = in.readUTF();
		
		this.osName = in.readUTF();
		this.osArch = in.readUTF();
		this.osVersion = in.readUTF();
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeUTF(this.osType);
		out.writeUTF(this.fileSep);
		out.writeUTF(this.pathSep);
		out.writeUTF(this.versionString);
		
		out.writeUTF(this.osName);
		out.writeUTF(this.osArch);
		out.writeUTF(this.osVersion);
	}
	
	
	/**
	 * The OS type as defined in R <code>.Platform$OS.type</code>
	 * 
	 * @see #OS_WINDOWS
	 * @see #OS_UNIX
	 * 
	 * @return the os type constant
	 */
	public String getOsType() {
		return this.osType;
	}
	
	public String getFileSep() {
		return this.fileSep;
	}
	
	public String getPathSep() {
		return this.pathSep;
	}
	
	public Version getRVersion() {
		if (this.version == null) {
			this.version = new Version(this.versionString);
		}
		return this.version;
	}
	
	
	/**
	 * The OS name as defined by the Java property <code>os.name</code>
	 * 
	 * @return the OS name string
	 */
	public String getOSName() {
		return this.osName;
	}
	
	/**
	 * The OS architecture as defined by the Java property <code>os.arch</code>
	 * 
	 * @return the OS architecture string
	 */
	public String getOSArch() {
		return this.osArch;
	}
	
	/**
	 * The OS version as defined by the Java property <code>os.version</code>
	 * 
	 * @return the OS version string
	 */
	public String getOSVersion() {
		return this.osVersion;
	}
	
}
