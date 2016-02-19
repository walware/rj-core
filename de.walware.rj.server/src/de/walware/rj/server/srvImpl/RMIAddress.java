/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
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

package de.walware.rj.server.srvImpl;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.registry.Registry;


/**
 * Address for RMI naming
 */
class RMIAddress {
	
	
	public static final InetAddress LOOPBACK;
	
	public static void validate(final String address) throws MalformedURLException {
		try {
			new RMIAddress(address, false);
		}
		catch (final UnknownHostException e) {
		}
	}
	
	
	private static String checkChars(final String s) throws MalformedURLException {
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			if (c == '?' || c == '#' || c == '[' || c == ']' || c == '@'
					|| c == '!' || c == '$' || c == '&' || c == '\'' || c == '(' || c == ')'
					|| c == '*' || c == '+' || c == ',' || c == ';' || c == '='
					|| c == '"' || c == '\\') {
				throw new MalformedURLException("Character '"+c+"' is not allowed.");
			}
		}
		return s;
	}
	
	private static int checkPort(final String port) throws MalformedURLException {
		final int portNum;
		try {
			portNum = (port != null) ? Integer.parseInt(port) : Registry.REGISTRY_PORT;
		}
		catch (final NumberFormatException e) {
			throw new MalformedURLException("Invalid port, " + e.getMessage());
		}
		return checkPort(portNum);
	}
	
	private static int checkPort(final int portNum) throws MalformedURLException {
		if (portNum < 0 || portNum > 65535) {
			throw new MalformedURLException("Invalid port, " + "Value must be in range 0-65535");
		}
		return portNum;
	}
	
	private static String build(final String host, final int portNum, final String name) {
		final StringBuilder sb = new StringBuilder("//"); //$NON-NLS-1$
		if (host != null) {
			sb.append(host);
		}
		sb.append(':');
		if (portNum >= 0) {
			sb.append(Integer.toString(portNum));
		}
		sb.append('/');
		if (name != null) {
			sb.append(name);
		}
		return sb.toString();
	}
	
	
	static {
		InetAddress loopbackAddress;
		try {
			loopbackAddress = InetAddress.getByAddress("localhost", new byte[] { 127, 0, 0, 1 }); //$NON-NLS-1$
		}
		catch (final UnknownHostException e) {
			loopbackAddress = null;
			e.printStackTrace();
		}
		LOOPBACK = loopbackAddress;
	}
	
	
	private final String host;
	private InetAddress hostAddress;
	private final String port;
	private final int portNum;
	private final boolean ssl;
	private final String path;
	
	private String address;
	private String ser;
	
	
	public RMIAddress(final String address) throws UnknownHostException, MalformedURLException {
		this(address, true);
	}
	
	public RMIAddress(final String host, final int portNum, final String name) throws UnknownHostException, MalformedURLException {
		this(build(host, portNum, name), true);
	}
	
	public RMIAddress(final InetAddress address, final int port, final String name)
			throws MalformedURLException {
		this(address.getHostAddress(), address, Integer.toString(port), checkPort(port), false,
				(name != null) ? checkChars(name) : "");
	}
	
	public RMIAddress(final InetAddress address, final int port, final boolean ssl, final String name)
			throws MalformedURLException {
		this(address.getHostAddress(), address, Integer.toString(port), checkPort(port), ssl,
				(name != null) ? checkChars(name) : "");
	}
	
	public RMIAddress(final RMIAddress registry, final String name) throws MalformedURLException {
		this(registry.host, registry.hostAddress, registry.port, registry.portNum, registry.ssl,
				(name != null) ? checkChars(name) : "");
	}
	
	private RMIAddress(String address, final boolean resolve) throws UnknownHostException, MalformedURLException {
		address = checkChars(address);
		
		if (address.startsWith("ssl:")) { //$NON-NLS-1$
			address = address.substring(4);
			this.ssl = true;
		}
		else {
			this.ssl = false;
		}
		if (address.startsWith("rmi:")) { //$NON-NLS-1$
			address = address.substring(4);
		}
		if (!address.startsWith("//")) { //$NON-NLS-1$
			address = "//"+address; //$NON-NLS-1$
		}
		
		final int idxPort = address.indexOf(':', 2);
		final int idxPath = address.indexOf('/', 2);
		if (idxPort > 0) {
			if (idxPath <= idxPort) {
				throw new IllegalArgumentException();
			}
			this.host = (2 < idxPort) ? address.substring(2, idxPort) : null;
			this.port = (idxPort+1 < idxPath) ? address.substring(idxPort+1, idxPath) : null;
			this.path = address.substring(idxPath+1);
		}
		else if (idxPath > 0){
			this.host = (2 < idxPath) ? address.substring(2, idxPath) : null;
			this.port = null;
			this.path = address.substring(idxPath+1);
		}
		else {
			this.host = null;
			this.port = null;
			this.path = address.substring(2);
		}
		try {
			this.portNum = checkPort(this.port);
		}
		catch (final NumberFormatException e) {
			throw new MalformedURLException("Invalid port, " + e.getLocalizedMessage());
		}
		if (resolve) {
			this.hostAddress = (this.host != null) ? InetAddress.getByName(this.host) : LOOPBACK;
		}
	}
	
	private RMIAddress(final String host, final InetAddress hostAddress, final String port,
			final int portNum, final boolean ssl, final String path) {
		this.host = host;
		this.hostAddress = hostAddress;
		this.port = port;
		this.portNum = portNum;
		this.ssl = ssl;
		this.path = path;
	}
	
	
	/**
	 * @return the host as specified when creating the address
	 */
	public String getHost() {
		return (this.host != null) ? this.host : this.hostAddress.getHostAddress();
	}
	
	public InetAddress getHostAddress() {
		return this.hostAddress;
	}
	
	public boolean isLocalHost() {
		if (this.hostAddress.isLoopbackAddress()) {
			return true;
		}
		try {
			final InetAddress localhost = InetAddress.getLocalHost();
			if (this.hostAddress.equals(localhost)) {
				return true;
			}
		}
		catch (final UnknownHostException e) {}
		catch (final ArrayIndexOutOfBoundsException e) { /* JVM bug */ }
		
		return false;
	}
	
	/**
	 * @return the port
	 */
	public String getPort() {
		return this.port;
	}
	
	public int getPortNum() {
		return this.portNum;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return this.path;
	}
	
	/**
	 * Standard string presentation to use for rmi
	 * @return
	 */
	public String getAddress() {
		if (this.address == null) {
			final StringBuilder sb = new StringBuilder(32);
			sb.append("rmi://"); //$NON-NLS-1$
			if (this.host != null) {
				sb.append(this.host);
			}
			if (this.portNum != Registry.REGISTRY_PORT) {
				sb.append(':');
				sb.append(this.port);
			}
			sb.append('/');
			sb.append(this.path);
			this.address = sb.toString();
		}
		return this.address;
	}
	
	public RMIAddress getRegistryAddress() {
		return new RMIAddress(this.host, this.hostAddress, this.port, this.portNum, this.ssl, "");
	}
	
	/**
	 * 
	 * @return if SSL is enabled
	 * 
	 * @since 1.4
	 */
	public boolean isSSL() {
		return this.ssl;
	}
	
	
	@Override
	public String toString() {
		if (this.ser == null) {
			final String address = getAddress();
			if (this.ssl) {
				this.ser = "ssl:" + address;
			}
			else {
				this.ser = address;
			}
		}
		return this.ser;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof RMIAddress)) {
			return false;
		}
		final RMIAddress other = (RMIAddress) obj;
		return (this.hostAddress.equals(other.hostAddress)
				&& this.portNum == other.portNum
				&& this.ssl == other.ssl
				&& this.path.equals(other.path) );
	}
	
}
