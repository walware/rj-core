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

package de.walware.rj.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.walware.rj.server.gr.RjsGraphicManager;


/**
 * Interface from R to Java side of the RJ server
 */
public class RJ {
	
	
	private static RJ instance;
	
	protected byte currentSlot;
	
	private final Object clientPropertiesLock = new Object();
	private Map<String, Object>[] clientPropertiesMaps = new Map[2];
	
	
	public final static RJ get() {
		return instance;
	}
	
	
	protected RJ() {
		if (instance != null) {
			throw new IllegalStateException();
		}
		instance = this;
	}
	
	
	public void onRExit() {
		instance = null;
	}
	
	public byte getCurrentSlot() {
		return this.currentSlot;
	}
	
	public RjsGraphicManager getGraphicManager() {
		return null;
	}
	
	protected void setClientProperty(final byte slot, final String key, final Object value) {
		Map<String, Object> map;
		synchronized (this.clientPropertiesLock) {
			if (slot >= this.clientPropertiesMaps.length) {
				final Map<String, Object>[] newMaps = new Map[this.clientPropertiesMaps.length];
				System.arraycopy(this.clientPropertiesMaps, 0, newMaps, 0, slot+1);
				this.clientPropertiesMaps = newMaps;
			}
			map = this.clientPropertiesMaps[slot];
			if (map == null) {
				this.clientPropertiesMaps[slot] = new HashMap<String, Object>();
			}
		}
		map.put(key, value);
	}
	
	protected void setClientProperties(final byte slot, final Map<String, ? extends Object> properties) {
		Map<String, Object> map;
		synchronized (this.clientPropertiesLock) {
			if (slot >= this.clientPropertiesMaps.length) {
				final Map<String, Object>[] newMaps = new Map[this.clientPropertiesMaps.length];
				System.arraycopy(this.clientPropertiesMaps, 0, newMaps, 0, slot+1);
				this.clientPropertiesMaps = newMaps;
			}
			map = this.clientPropertiesMaps[slot];
			if (map == null) {
				this.clientPropertiesMaps[slot] = map = new HashMap<String, Object>();
			}
		}
		for (final Entry<String, ? extends Object> entry : properties.entrySet()) {
			if (entry.getValue() != null) {
				map.put(entry.getKey(), entry.getValue());
			}
			else {
				map.remove(entry.getKey());
			}
		}
	}
	
	public Object getClientProperty(final byte slot, final String key) {
		final Map<String, Object>[] clients = this.clientPropertiesMaps;
		if (slot >= clients.length) {
			return null;
		}
		final Map<String, Object> map = clients[slot];
		if (map == null) {
			return null;
		}
		synchronized (map) {
			return map.get(key);
		}
	}
	
	public MainCmdItem sendMainCmd(final MainCmdItem cmd) {
		throw new UnsupportedOperationException();
	}
	
}
