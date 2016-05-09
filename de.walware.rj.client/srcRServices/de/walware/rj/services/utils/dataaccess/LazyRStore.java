/*=============================================================================#
 # Copyright (c) 2013-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services.utils.dataaccess;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

/**
 * 
 * 
 * @param <V> type of R object fragments
 * @since 2.0 (provisional)
 */
public class LazyRStore<V> {
	
	
	public final static int DEFAULT_FRAGMENT_SIZE= 2500;
	
	public final static class Fragment<W> extends RDataSubset {
		
		private static final byte SCHEDULED= (byte) 1 << 0;
		private static final byte SET= (byte) 1 << 1;
		
		/** sequential number of fragments, first by fragment col, then by fragment row */
		private final long number;
		
		private W rObject;
		
		private Fragment<W> newer;
		private Fragment<W> older;
		
		private byte state;
		
		
		Fragment(final long number,
				final long rowBeginIdx, final long rowCount,
				final long columnBeginIdx, final long columnCount) {
			super(rowBeginIdx, rowCount, columnBeginIdx, columnCount);
			this.number= number;
		}
		
		
		/**
		 * Returns the R object for this fragment.
		 * 
		 * @return the R object for this fragment or <code>null</code>, if not available.
		 */
		public W getRObject() {
			return this.rObject;
		}
		
		
		@Override
		public String toString() {
			final StringBuilder sb= new StringBuilder("LazyRStore$Fragment "); //$NON-NLS-1$
			sb.append(this.number);
			sb.append("\n\trows= ").append(getRowBeginIdx()).append("...").append(getRowEndIdx()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\n\tcolumns= ").append(getColumnBeginIdx()).append("...").append(getColumnEndIdx()); //$NON-NLS-1$ //$NON-NLS-2$
			return sb.toString();
		}
		
	}
	
	public static interface Updater<T> {
		
		
		void scheduleUpdate(LazyRStore<T> store,
				RDataAssignment assignment, Fragment<T> fragment);
		
	}
	
	
	private final long columnCount;
	private long rowCount;
	
	private final int maxFragmentCount;
	private Fragment<V>[] fragments;
	private int currentFragmentCount= 0;
	private final Fragment<V> topFragment= new Fragment<>(-1, 0, 0, 0, 0);
	private final Fragment<V> bottomFragment= new Fragment<>(-1, 0, 0, 0, 0);
	
	private final int fragmentRowCount;
	private final int fragmentColCount;
	private final long fragmentCountInRow;
	
	private final List<RDataAssignment> assignments= new ArrayList<>();
	
	private int scheduledCount;
	private Fragment<V> scheduleNext;
	private Updater<V> updater;
	
	
	public LazyRStore(final long rowCount, final long columnCount,
			final int maxFragmentCount,
			final Updater<V> updater) {
		this(rowCount, columnCount, maxFragmentCount, DEFAULT_FRAGMENT_SIZE, updater);
	}
	
	public LazyRStore(final long rowCount, final long columnCount,
			final int maxFragmentCount, final int fragmentSize,
			final Updater<V> updater) {
		this.columnCount= columnCount;
		
		this.maxFragmentCount= maxFragmentCount;
		
		this.fragmentColCount= (int) Math.min(columnCount, 25);
		this.fragmentCountInRow= (columnCount - 1) / this.fragmentColCount + 1;
		this.fragmentRowCount= fragmentSize / this.fragmentColCount;
		
		this.updater= updater;
		
		init(rowCount);
	}
	
	public LazyRStore(final long rowCount, final long columnCount,
			final int maxFragmentCount,
			final int fragmentRowCount, final int fragmentColCount,
			final Updater<V> updater) {
		this.columnCount= columnCount;
		
		this.maxFragmentCount= maxFragmentCount;
		
		this.fragmentColCount= fragmentColCount;
		this.fragmentCountInRow= (columnCount - 1) / fragmentColCount + 1;
		this.fragmentRowCount= fragmentRowCount;
		
		this.updater= updater;
		
		init(rowCount);
	}
	
	private void init(final long rowCount) {
		this.fragments= new Fragment[Math.min(16, this.maxFragmentCount)];
		
		clear(rowCount);
	}
	
	private long getNumber(final long rowIdx, final long columnIdx) {
		return (rowIdx / this.fragmentRowCount) * this.fragmentCountInRow +
				(columnIdx / this.fragmentColCount);
	}
	
	
	public LazyRStore.Fragment<V> getFragment(final long rowIdx, final long columnIdx) {
		if (rowIdx >= this.rowCount) {
			return null;
		}
		final long number= getNumber(rowIdx, columnIdx);
		final Fragment<V> fragment= getFragment(number, true);
		
		if ((fragment.state & Fragment.SET) != 0) {
			return fragment;
		}
		
		final boolean scheduleUpdate= (this.scheduledCount == 0);
		
		this.scheduleNext= this.topFragment;
		if ((fragment.state & Fragment.SCHEDULED) == 0) {
			fragment.state= Fragment.SCHEDULED;
			this.scheduledCount++;
			
			if (scheduleUpdate) {
				this.updater.scheduleUpdate(this, null, fragment);
			}
			
			if ((fragment.state & Fragment.SET) != 0) {
				return fragment;
			}
		}
		return null;
	}
	
	public LazyRStore.Fragment<V> getLoadedFragment(final long rowIdx, final long columnIdx) {
		if (rowIdx >= this.rowCount) {
			return null;
		}
		final long number= getNumber(rowIdx, columnIdx);
		final Fragment<V> fragment= getFragment(number, false);
		
		if ((fragment.state & Fragment.SET) != 0) {
			return fragment;
		}
		return null;
	}
	
	public LazyRStore.Fragment<V> getLoadedFragmentAny() {
		Fragment<V> fragment= this.topFragment;
		while (fragment != null) {
			if ((fragment.state & Fragment.SET) != 0) {
				return fragment;
			}
			fragment= fragment.older;
		}
		return null;
	}
	
	public void set(final RDataAssignment assignment) {
		final Fragment<V> fragment= clear(assignment);
		this.assignments.add(assignment);
		
		final boolean scheduleUpdate= (this.scheduledCount == 0);
		
		this.scheduledCount++; // assignment
		
		if (fragment != null) {
			fragment.state= Fragment.SCHEDULED;
			this.scheduledCount++;
		}
		
		if (scheduleUpdate) {
			this.updater.scheduleUpdate(this, assignment, fragment);
		}
	}
	
	public Fragment<V> clear(final RDataSubset subset) {
		Fragment<V> firstFragment= null;
		long columnBeginIdx= (subset.getColumnBeginIdx() / this.fragmentColCount) * this.fragmentColCount; 
		while (columnBeginIdx < subset.getColumnEndIdx()) {
			long rowBeginIdx= (subset.getRowBeginIdx() / this.fragmentRowCount) * this.fragmentRowCount;
			while (rowBeginIdx < subset.getRowEndIdx()) {
				final long number= getNumber(rowBeginIdx, columnBeginIdx);
				final int idx= indexOf(number);
				if (idx >= 0) {
					final Fragment<V> fragment= clearFragment(idx);
					if (firstFragment == null) {
						firstFragment= fragment;
					}
				}
				rowBeginIdx= Math.min(rowBeginIdx + this.fragmentRowCount, this.rowCount);
			}
			columnBeginIdx= Math.min(columnBeginIdx + this.fragmentColCount, this.columnCount);
		}
		return firstFragment;
	}
	
	
	private int indexOf(final long number) {
		final Fragment<V>[] array= this.fragments;
		int low= 0;
		int high= this.currentFragmentCount;
		
		while (low <= high) {
			final int mid= (low + high) >> 1;
			final Fragment<V> fragment= array[mid];
			
			if (fragment.number < number) {
				low= mid + 1;
			}
			else if (fragment.number > number) {
				high= mid - 1;
			}
			else {
				return mid;
			}
		}
		return - (low + 1);
	}
	
	private Fragment<V> getFragment(final long number, final boolean create) {
		if (this.topFragment.older.number == number) {
			return this.topFragment.older;
		}
		
		final Fragment<V>[] array= this.fragments;
		int low= 0;
		{	int high= this.currentFragmentCount - 1;
			while (low <= high) {
				final int mid= (low + high) >> 1;
				final Fragment<V> fragment= array[mid];
				
				if (fragment.number < number) {
					low= mid + 1;
				}
				else if (fragment.number > number) {
					high= mid - 1;
				}
				else {
					fragment.newer.older= fragment.older;
					fragment.older.newer= fragment.newer;
					fragment.newer= this.topFragment;
					fragment.older= this.topFragment.older;
					this.topFragment.older.newer= fragment;
					this.topFragment.older= fragment;
					return fragment;
				}
			}
		}
		if (!create) {
			return null;
		}
		
		final Fragment<V> fragment= createFragment(number);
		{	if (array.length == this.currentFragmentCount) {
				this.fragments= new Fragment[Math.min(this.currentFragmentCount * 2, this.maxFragmentCount)];
				System.arraycopy(array, 0, this.fragments, 0, low);
			}
			System.arraycopy(array, low, this.fragments, low + 1, this.currentFragmentCount - low);
			this.fragments[low]=  fragment;
			this.currentFragmentCount++;
		}
		
		fragment.newer= this.topFragment;
		fragment.older= this.topFragment.older;
		this.topFragment.older.newer= fragment;
		this.topFragment.older= fragment;
		
		if (this.currentFragmentCount >= this.maxFragmentCount) {
			removeOldestFragment();
		}
		
		return fragment;
	}
	
	private Fragment<V> createFragment(final long number) {
		final long rowBeginIdx= (number / this.fragmentCountInRow) * this.fragmentRowCount;
		final long rowEndIdx= Math.min(rowBeginIdx + this.fragmentRowCount, this.rowCount);
		final long columnBeginIdx= (number % this.fragmentCountInRow) * this.fragmentColCount; 
		final long columnEndIdx= Math.min(columnBeginIdx + this.fragmentColCount, this.columnCount);
		return new Fragment<>(number,
				rowBeginIdx, (rowEndIdx - rowBeginIdx),
				columnBeginIdx, (columnEndIdx - columnBeginIdx) );
	}
	
	private Fragment<V> clearFragment(final int idx) {
		final Fragment<V> oldFragment= this.fragments[idx];
		final Fragment<V> newFragment= new Fragment<>(oldFragment.number,
				oldFragment.getRowBeginIdx(), oldFragment.getRowCount(),
				oldFragment.getColumnBeginIdx(), oldFragment.getColumnCount() );
		if (oldFragment.newer != null) {
			newFragment.newer= oldFragment.newer;
			newFragment.newer.older= newFragment;
		}
		if (oldFragment.older != null) {
			newFragment.older= oldFragment.older;
			newFragment.older.newer= newFragment;
		}
		return this.fragments[idx]= newFragment;
	}
	
	private void removeOldestFragment() {
		final Fragment<V> fragment= this.bottomFragment.newer;
		if ((fragment.state & Fragment.SCHEDULED) != 0) {
			fragment.state &= ~Fragment.SCHEDULED;
			this.scheduledCount--;
		}
		if (this.scheduleNext == fragment) {
			this.scheduleNext= fragment.older;
		}
		
		final int idx= indexOf(fragment.number);
		this.currentFragmentCount--;
		System.arraycopy(this.fragments, idx + 1, this.fragments, idx, this.currentFragmentCount - idx);
		this.fragments[this.currentFragmentCount]= null;
		
		fragment.newer.older= this.bottomFragment;
		this.bottomFragment.newer= fragment.newer;
		fragment.newer= null;
		fragment.older= null;
		return;
	}
	
	
	public void clear(final long rowCount) {
		final Fragment<V>[] array= this.fragments;
		for (int i= 0; i < this.currentFragmentCount; i++) {
			array[i].state &= ~Fragment.SCHEDULED;
			array[i]= null;
		}
		this.currentFragmentCount= 0;
		this.topFragment.older= this.bottomFragment;
		this.bottomFragment.newer= this.topFragment;
		
		this.scheduledCount= 0;
		this.scheduleNext= this.topFragment;
		
		if (rowCount >= 0) {
			this.rowCount= rowCount;
		}
	}
	
	public Fragment<V>[] getScheduledFragments() {
		final Fragment<V>[] scheduledFragments= new Fragment[this.scheduledCount];
		Fragment<V> fragment= this.topFragment.older;
		int i= 0;
		while (i < this.scheduledCount) {
			if ((fragment.state & Fragment.SCHEDULED) != 0) {
				scheduledFragments[i++]= fragment;
			}
			fragment= fragment.older;
		}
		return scheduledFragments;
	}
	
	public Fragment<V> getNextScheduledFragment() {
		if (this.scheduledCount == 0) {
			return null;
		}
		Fragment<V> fragment= this.scheduleNext.older;
		while (true) {
			if ((fragment.state & Fragment.SCHEDULED) != 0) {
				this.scheduleNext= fragment;
				return fragment;
			}
			fragment= fragment.older;
		}
	}
	
	public void updateAssignment(final RDataAssignment assignment, final IStatus status) {
		if (this.assignments.remove(assignment)) {
			this.scheduledCount--;
		}
	}
	
	public void updateFragment(final Fragment<V> fragment, final V rObject) {
		if ((fragment.state & Fragment.SET) != 0) {
			throw new IllegalStateException();
		}
		fragment.rObject= rObject;
		if ((fragment.state & Fragment.SCHEDULED) != 0) {
			this.scheduledCount--;
		}
		fragment.state= Fragment.SET;
	}
	
}
