/*******************************************************************************
 * Copyright (c) 2008-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.walware.rj.data.RJIO;


/**
 * Client-to-Server list with {@link MainCmdItem}s.
 */
public final class MainCmdC2SList implements RjsComObject, Externalizable {
	
	
	private final RJIO privateIO;
	
	private MainCmdItem first;
	
	
	public MainCmdC2SList(final RJIO io) {
		this.privateIO = io;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public MainCmdC2SList() {
		this.privateIO = null;
		this.first = null;
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		MainCmdItem item = this.first;
		if (item != null) {
			final RJIO io;
			if (this.privateIO != null) {
				io = this.privateIO;
				io.out = out;
			}
			else {
				io = RJIO.get(out);
			}
			do {
				out.writeByte(item.getCmdType());
				item.writeExternal(io);
			} while ((item = item.next) != null);
			io.out = null;
		}
		out.writeByte(MainCmdItem.T_NONE);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		final RJIO io;
		{	// first
			final byte type = in.readByte();
			switch (type) {
			case MainCmdItem.T_NONE:
				this.first = null;
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				io = RJIO.get(in);
				this.first = new ConsoleReadCmdItem(io);
				break;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				io = RJIO.get(in);
				this.first = new ConsoleWriteOutCmdItem(io);
				break;
			case MainCmdItem.T_MESSAGE_ITEM:
				io = RJIO.get(in);
				this.first = new ConsoleMessageCmdItem(io);
				break;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				io = RJIO.get(in);
				this.first = new ExtUICmdItem(io);
				break;
			case MainCmdItem.T_GRAPH_ITEM:
				io = RJIO.get(in);
				this.first = new GDCmdItem.Answer(io);
				break;
			case MainCmdItem.T_DATA_ITEM:
				io = RJIO.get(in);
				this.first = new DataCmdItem(io);
				break;
			default:
				throw new ClassNotFoundException("Unknown cmdtype id: "+type);
			}
		}
		
		MainCmdItem item = this.first;
		while (true) {
			final byte type = in.readByte();
			switch (type) {
			case MainCmdItem.T_NONE:
				io.in = null;
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				item = item.next = new ConsoleReadCmdItem(io);
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				item = item.next = new ConsoleWriteOutCmdItem(io);
				continue;
			case MainCmdItem.T_MESSAGE_ITEM:
				item = item.next = new ConsoleMessageCmdItem(io);
				continue;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				item = item.next = new ExtUICmdItem(io);
				continue;
			case MainCmdItem.T_GRAPH_ITEM:
				item = item.next = new GDCmdItem.Answer(io);
				continue;
			case MainCmdItem.T_DATA_ITEM:
				item = item.next = new DataCmdItem(io);
				continue;
			default:
				io.in = null;
				throw new ClassNotFoundException("Unknown cmdtype id: "+type);
			}
		}
	}
	
	
	public void clear() {
		this.first = null;
	}
	
	public void setObjects(final MainCmdItem first) {
		this.first = first;
	}
	
	
	public int getComType() {
		return RjsComObject.T_MAIN_LIST;
	}
	
	public MainCmdItem getItems() {
		return this.first;
	}
	
	public boolean testEquals(final MainCmdC2SList other) {
		MainCmdItem thisItem = this.first;
		MainCmdItem otherItem = other.first;
		while (thisItem != null && otherItem != null) {
			if (!thisItem.equals(otherItem)) {
				return false;
			}
			thisItem = thisItem.next;
			otherItem = otherItem.next;
		}
		if (thisItem != null || otherItem != null) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(100);
		sb.append("MainCmdC2SList (");
		sb.append("):");
		if (this.first == null) {
			sb.append("\n<ITEM />");
		}
		else {
			MainCmdItem item = this.first;
			int i = 0;
			while (item != null) {
				sb.append("\n<ITEM i=\"");
				sb.append(i);
				sb.append("\">\n");
				sb.append(item.toString());
				sb.append("\n</ITEM>");
				item = item.next;
				i++;
			}
		}
		return sb.toString();
	}
	
}
