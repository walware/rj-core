/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services.utils.dataaccess;


public class RDataSubset {
	
	
	private final long rowBeginIdx;
	private final long rowCount;
	private final long columnBeginIdx;
	private final long columnCount;
	
	
	public RDataSubset(final long rowBeginIdx, final long rowCount,
			final long columnBeginIdx, final long columnCount) {
		this.rowBeginIdx= rowBeginIdx;
		this.rowCount= rowCount;
		this.columnBeginIdx= columnBeginIdx;
		this.columnCount= columnCount;
	}
	
	
	/**
	 * Returns the row begin index of the subset.
	 * 
	 * @return the begin index (zero-based)
	 */
	public final long getRowBeginIdx() {
		return this.rowBeginIdx;
	}
	
	/**
	 * Returns the row end index (exclusive) of the subset.
	 * 
	 * @return the end index (zero-based)
	 */
	public final long getRowEndIdx() {
		return this.rowBeginIdx + this.rowCount;
	}
	
	/**
	 * Returns the row count of the subset.
	 * 
	 * @return the count
	 */
	public final long getRowCount() {
		return this.rowCount;
	}
	
	public long toLocalRowIdx(final long rowIdx) {
		final long idx;
		if (rowIdx < this.rowBeginIdx
				|| (idx = rowIdx - this.rowBeginIdx) >= this.rowCount) {
			throw new IndexOutOfBoundsException(Long.toString(rowIdx));
		}
		return idx;
	}
	
	/**
	 * Returns the column begin index of the subset.
	 * 
	 * @return the index (zero-based)
	 */
	public final long getColumnBeginIdx() {
		return this.columnBeginIdx;
	}
	
	/**
	 * Returns the column end index (exclusive) of the subset.
	 * 
	 * @return the index
	 */
	public final long getColumnEndIdx() {
		return this.columnBeginIdx + this.columnCount;
	}
	
	/**
	 * Returns the column count of the subset.
	 * 
	 * @return the count
	 */
	public final long getColumnCount() {
		return this.columnCount;
	}
	
	public long toLocalColumnIdx(final long columnIdx) {
		final long idx;
		if (columnIdx < this.columnBeginIdx
				|| (idx= columnIdx - this.columnBeginIdx) >= this.columnCount) {
			throw new IndexOutOfBoundsException(Long.toString(columnIdx));
		}
		return idx;
	}
	
	
	public long getLength() {
		return (this.rowCount * this.columnCount);
	}
	
	
}
