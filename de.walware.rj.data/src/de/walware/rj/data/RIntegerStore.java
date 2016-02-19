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

package de.walware.rj.data;


/**
 * Interface for R data stores of type {@link RStore#INTEGER}.
 * <p>
 * An R data store implements this interface if the R function
 * <code>typeof(object)</code> returns 'integer'.</p>
 */
public interface RIntegerStore extends RStore<Integer> {
	
	
	@Override
	Integer get(int idx);
	@Override
	Integer get(long idx);
	
	@Override
	Integer[] toArray();
	
}
