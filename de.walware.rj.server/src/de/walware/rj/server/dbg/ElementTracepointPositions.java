/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
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


public class ElementTracepointPositions implements RJIOExternalizable {
	
	
	private final SrcfileData fileInfo;
	
	private final String elementId;
	private final int[] elementSrcref;
	
	private final List<TracepointPosition> positions;
	
	
	public ElementTracepointPositions(final SrcfileData fileInfo,
			final String elementId, final int[] elementSrcref) {
		if (fileInfo == null) {
			throw new NullPointerException("fileInfo");
		}
		if (elementId == null) {
			throw new NullPointerException("elementId");
		}
		this.fileInfo = fileInfo;
		this.elementId = elementId;
		this.elementSrcref = elementSrcref;
		this.positions = new ArrayList<TracepointPosition>(4);
	}
	
	public ElementTracepointPositions(final RJIO io) throws IOException {
		this.fileInfo = new SrcfileData(io);
		this.elementId = io.readString();
		this.elementSrcref = io.readIntArray();
		final int l = io.readInt();
		this.positions = new ArrayList<TracepointPosition>(l);
		for (int i = 0; i < l; i++) {
			this.positions.add(new TracepointPosition(io));
		}
	}
	
	public void writeExternal(final RJIO io) throws IOException {
		this.fileInfo.writeExternal(io);
		io.writeString(this.elementId);
		io.writeIntArray(this.elementSrcref, (this.elementSrcref != null) ? 6 : -1);
		final int l = this.positions.size();
		io.writeInt(l);
		for (int i = 0; i < l; i++) {
			this.positions.get(i).writeExternal(io);
		}
	}
	
	
	public SrcfileData getSrcfile() {
		return this.fileInfo;
	}
	
	public String getElementId() {
		return this.elementId;
	}
	
	public int[] getElementSrcref() {
		return this.elementSrcref;
	}
	
	public List<? extends TracepointPosition> getPositions() {
		return this.positions;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ElementTracepointPositions");
		sb.append(" for ").append(this.elementId);
		sb.append("\n").append("list (count= ").append(this.positions.size()).append("):");
		for (int i = 0; i < this.positions.size(); i++) {
			sb.append("\n").append(this.positions.get(i).toString());
		}
		return sb.toString();
	}
	
}
