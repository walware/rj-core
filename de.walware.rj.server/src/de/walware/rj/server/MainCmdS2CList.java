/*******************************************************************************
 * Copyright (c) 2008-2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
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


/**
 * Server-to-Client list with {@link MainCmdItem}s.
 */
public final class MainCmdS2CList implements RjsComObject, Externalizable {
	
	
	static final AutoIdMap<ComHandler> gComHandlers = new AutoIdMap<ComHandler>();
	
	
	private int id;
	
	private boolean isBusy;
	
	private MainCmdItem first;
	
	
	public MainCmdS2CList(final MainCmdItem first, final boolean isBusy) {
		this.isBusy = isBusy;
		this.first = first;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public MainCmdS2CList() {
		this.isBusy = false;
		this.first = null;
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeShort(this.id);
		
		out.writeBoolean(this.isBusy);
		
		MainCmdItem item = this.first;
		while (item != null) {
			out.writeByte(item.getCmdType());
			item.writeExternal(out);
			item = item.next;
		}
		out.writeByte(MainCmdItem.T_NONE);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.id = in.readUnsignedShort();
		if (this.id != 0) {
			gComHandlers.get(this.id).processMainCmd(in);
			return;
		}
		
		this.isBusy = in.readBoolean();
		
		{	// first
			final byte type = in.readByte();
			switch (type) {
			case MainCmdItem.T_NONE:
				this.first = null;
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				this.first = new ConsoleReadCmdItem(in);
				break;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				this.first = new ConsoleWriteOutCmdItem(in);
				break;
			case MainCmdItem.T_MESSAGE_ITEM:
				this.first = new ConsoleMessageCmdItem(in);
				break;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				this.first = new ExtUICmdItem(in);
				break;
			case MainCmdItem.T_DATA_ITEM:
				this.first = new DataCmdItem(in);
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
				return;
			case MainCmdItem.T_CONSOLE_READ_ITEM:
				item = item.next = new ConsoleReadCmdItem(in);
				continue;
			case MainCmdItem.T_CONSOLE_WRITE_OUT_ITEM:
				item = item.next = new ConsoleWriteOutCmdItem(in);
				continue;
			case MainCmdItem.T_MESSAGE_ITEM:
				item = item.next = new ConsoleMessageCmdItem(in);
				continue;
			case MainCmdItem.T_EXTENDEDUI_ITEM:
				item = item.next = new ExtUICmdItem(in);
				continue;
			case MainCmdItem.T_DATA_ITEM:
				item = item.next = new DataCmdItem(in);
				continue;
			default:
				throw new ClassNotFoundException("Unknown cmdtype id: "+type);
			}
		}
	}
	
	
	public void clear() {
		MainCmdItem item = this.first;
		while (item != null) {
			final MainCmdItem tmp = item;
			item = item.next;
			tmp.next = null;
		}
		this.first = null;
	}
	
	public boolean isEmpty() {
		return (this.first == null);
	}
	
	public void setId(final int id) {
		this.id = (short) id;
	}
	
	public void setBusy(final boolean isBusy) {
		this.isBusy = isBusy;
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
	
	public boolean isBusy() {
		return this.isBusy;
	}
	
	
	public boolean testEquals(final MainCmdS2CList other) {
		if (this.isBusy != other.isBusy()) {
			return false;
		}
		
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
		sb.append("MainCmdS2CList (isBusy=");
		sb.append(this.isBusy);
		sb.append(')');
		if (this.first != null) {
			sb.append(':');
		}
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
		return sb.toString();
	}
	
}
