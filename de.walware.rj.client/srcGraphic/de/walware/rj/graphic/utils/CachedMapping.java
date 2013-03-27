/*******************************************************************************
 * Copyright (c) 2011-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.graphic.utils;


public final class CachedMapping implements CharMapping {
	
	
	private final CharMapping mapping;
	
	private String lastString;
	private String lastEncoded;
	
	
	public CachedMapping(final CharMapping mapping) {
		this.mapping = mapping;
	}
	
	
	@Override
	public int encode(final int codepoint) {
		return this.mapping.encode(codepoint);
	}
	
	@Override
	public String encode(final String s) {
		if (s.equals(this.lastString)) {
			return this.lastEncoded;
		}
		return (this.lastEncoded = this.mapping.encode(s));
	}
	
}
