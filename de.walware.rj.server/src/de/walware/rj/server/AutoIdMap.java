/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;


final class AutoIdMap<E> {
	
	
	private Object[] currentArray;
	
	
	public AutoIdMap() {
		this.currentArray = new Object[16];
	}
	
	
	public synchronized int put(final E e) {
		final int size = this.currentArray.length;
		for (int i = 1; i < size; i++) {
			if (this.currentArray[i] == null) {
				this.currentArray[i] = e;
				return i;
			}
		}
		final Object[] newArray = new Object[size+16];
		System.arraycopy(this.currentArray, 0, newArray, 0, size);
		newArray[size] = e;
		this.currentArray = newArray;
		return size;
	}
	
	@SuppressWarnings("unchecked")
	public E get(final int id) {
		return (E) this.currentArray[id];
	}
	
	public void remove(final int id) {
		if (id > 0 && id < this.currentArray.length) {
			this.currentArray[id] = null;
		}
	}
	
}
