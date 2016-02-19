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

package de.walware.rj;


/**
 * Exception indicating that setting the configuration failed because it is invalid
 */
public class RjInvalidConfigurationException extends RjException {
	
	
	private static final long serialVersionUID = 6709030537472523580L;
	
	
	public RjInvalidConfigurationException(final String message) {
		super(message);
	}
	
	public RjInvalidConfigurationException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
}
