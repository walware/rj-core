/*******************************************************************************
 * Copyright (c) 2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.dbg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RJIOExternalizable;


public class TracepointState implements Tracepoint, Comparable<TracepointState>, RJIOExternalizable {
	
	
	static final List<TracepointState> readList(final RJIO io) throws IOException {
		final int l = io.readInt();
		final List<TracepointState> list = new ArrayList<TracepointState>(l);
		String filePath = null;
		String elementId = null;
		for (int i = 0; i < l; i++) {
			final TracepointState state = new TracepointState();
			list.add(state);
			state.type = io.readInt();
			state.id = io.readLong();
			state.filePath = io.readString();
			if (state.filePath != null) {
				filePath = state.filePath;
			}
			else {
				state.filePath = filePath;
			}
			if (state.type == TYPE_DELETED) {
				continue;
			}
			state.elementId = io.readString();
			if (state.elementId != null) {
				elementId = state.elementId;
			}
			else {
				state.elementId = elementId;
			}
			state.index = io.readIntArray();
			state.elementLabel = io.readString();
			state.flags = io.readInt();
			state.expr = io.readString();
		}
		return list;
	}
	
	static final void writeList(final List<TracepointState> list, final RJIO io) throws IOException {
		final int l = list.size();
		io.writeInt(l);
		String filePath = null;
		String elementId = null;
		for (int i = 0; i < l; i++) {
			final TracepointState state = list.get(i);
			io.writeInt(state.type);
			io.writeLong(state.id);
			if (state.filePath.equals(filePath)) {
				io.writeString(null);
			}
			else {
				filePath = state.filePath;
				io.writeString(filePath);
			}
			if (state.type == TYPE_DELETED) {
				continue;
			}
			if (state.elementId.equals(elementId)) {
				io.writeString(null);
			}
			else {
				elementId = state.elementId;
				io.writeString(elementId);
			}
			io.writeIntArray(state.index, state.index.length);
			io.writeString(state.elementLabel);
			io.writeInt(state.flags);
			io.writeString(state.expr);
		}
	}
	
	
	public static final int FLAG_ENABLED =                  0x00000001;
	
	public static final int FLAG_MB_ENTRY =                 0x00010000;
	public static final int FLAG_MB_EXIT =                  0x00020000;
	
	public static final int FLAG_EXPR_INVALID =             0x01000000;
	public static final int FLAG_EXPR_EVAL_FAILED =         0x02000000;
	
	
	private int type;
	
	private String filePath;
	private long id;
	
	private String elementId;
	private int[] index;
	
	protected String elementLabel;
	protected int flags;
	protected String expr;
	
	
	
	public TracepointState(final int type,
			final String filePath, final long id, final String elementId, final int[] index,
			final String elementLabel, final int flags, final String expr) {
		if (filePath == null || elementId == null || index == null) {
			throw new NullPointerException();
		}
		this.type = type;
		this.filePath = filePath;
		this.id = id;
		this.elementId = elementId;
		this.index = index;
		this.elementLabel = elementLabel;
		this.flags = flags;
		this.expr = expr;
	}
	
	public TracepointState(final int type,
			final String filePath, final long id) {
		if (type != TYPE_DELETED) {
			throw new IllegalArgumentException("type= " + type);
		}
		this.type = type;
		this.filePath = filePath;
		this.id = id;
	}
	
	private TracepointState() {
	}
	
	public TracepointState(final RJIO io) throws IOException {
		this.type = io.readInt();
		this.id = io.readLong();
		this.filePath = io.readString();
		if (this.type == TYPE_DELETED) {
			return;
		}
		this.elementId = io.readString();
		this.index = io.readIntArray();
		this.elementLabel = io.readString();
		this.flags = io.readInt();
		this.expr = io.readString();
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		io.writeInt(this.type);
		io.writeLong(this.id);
		io.writeString(this.filePath);
		if (this.type == TYPE_DELETED) {
			return;
		}
		io.writeString(this.elementId);
		io.writeIntArray(this.index, this.index.length);
		io.writeString(this.elementLabel);
		io.writeInt(this.flags);
		io.writeString(this.expr);
	}
	
	@Override
	public TracepointState clone() {
		return new TracepointState(this.type,
				this.filePath, this.id, this.elementId, this.index,
				this.elementLabel, this.flags, this.expr );
	}
	
	
	public int getType() {
		return this.type;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public long getId() {
		return this.id;
	}
	
	public String getElementId() {
		return this.elementId;
	}
	
	public int[] getIndex() {
		return this.index;
	}
	
	public boolean isEnabled() {
		return ((this.flags & FLAG_ENABLED) != 0);
	}
	
	public String getElementLabel() {
		return this.elementLabel;
	}
	
	public int getFlags() {
		return this.flags;
	}
	
	public String getExpr() {
		return this.expr;
	}
	
	
	public int compareTo(final TracepointState other) {
		{	final int diff = this.filePath.compareTo(other.filePath);
			if (diff != 0) {
				return diff;
			}
		}
		if (this.elementId != null) {
			if (other.elementId != null) {
				final int diff = this.elementId.compareTo(other.elementId);
				if (diff != 0) {
					return diff;
				}
			}
			else {
				return 0x1000;
			}
		}
		else if (other.elementId != null) {
			return -0x1000;
		}
		return (this.id < other.id) ? -0x1 : 0x1;
	}
	
	@Override
	public int hashCode() {
		return (int) (this.filePath.hashCode() * this.id);
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TracepointState)) {
			return false;
		}
		final TracepointState other = (TracepointState) obj;
		return (this.id == other.id
				&& this.filePath.equals(other.filePath) );
	}
	
}
