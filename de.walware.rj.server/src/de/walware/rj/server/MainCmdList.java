/*******************************************************************************
 * Copyright (c) 2008-2009 Stephan Wahlbrink and others.
 * All rights reserved. This program and the accompanying materials
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
 * Command bundling multiple {@link MainCmdItem}.
 */
public final class MainCmdList implements RjsComObject, Externalizable {
	
	
	private static final MainCmdItem[] EMPTY_LIST = new MainCmdItem[0];
	
	
	private MainCmdItem[] list;
	private boolean isBusy;
	
	
	public MainCmdList() {
		this.list = EMPTY_LIST;
		this.isBusy = false;
	}
	
	public MainCmdList(final MainCmdItem[] list, final boolean isBusy) {
		this.list = list;
		this.isBusy = isBusy;
	}
	
	
	public void writeExternal(final ObjectOutput out) throws IOException {
		final int length = this.list.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			if (this.list[i] != null) {
				out.writeInt(this.list[i].getComType());
				this.list[i].writeExternal(out);
			}
			else {
				out.writeInt(-1);
			}
		}
		out.writeBoolean(this.isBusy);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		final int length = in.readInt();
		this.list = new MainCmdItem[length];
		for (int i = 0; i < length; i++) {
			final int type = in.readInt();
			switch (type) {
			case -1:
				this.list[i] = null;
				continue;
			case T_CONSOLE_READ_ITEM:
				this.list[i] = new ConsoleCmdItem.Read(in);
				continue;
			case T_CONSOLE_WRITE_ITEM:
				this.list[i] = new ConsoleCmdItem.Write(in);
				continue;
			case T_MESSAGE_ITEM:
				this.list[i] = new ConsoleCmdItem.Message(in);
				continue;
			case T_EXTENDEDUI_ITEM:
				this.list[i] = new ExtUICmdItem(in);
				continue;
			default:
				throw new ClassNotFoundException("Unknown cmdtype id: "+type);
			}
		}
		this.isBusy = in.readBoolean();
	}
	
	
	public void clear() {
		this.list = EMPTY_LIST;
	}
	
	public void setBusy(final boolean isBusy) {
		this.isBusy = isBusy;
	}
	
	public void setObjects(final MainCmdItem[] list) {
		this.list = list;
	}
	
	
	public int getComType() {
		return RjsComObject.T_MAIN_LIST;
	}
	
	public MainCmdItem[] getItems() {
		return this.list;
	}
	
	public boolean isBusy() {
		return this.isBusy;
	}
	
	
	public boolean testEquals(MainCmdList other) {
		if (this.isBusy != other.isBusy()) {
			return false;
		}
		MainCmdItem[] otherList = other.getItems();
		if (this.list.length != otherList.length) {
			return false;
		}
		for (int i = 0; i < this.list.length; i++) {
			if (!this.list[i].testEquals(otherList[i])) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(100 + this.list.length*100);
		sb.append("MainCmdList (isBusy=");
		sb.append(this.isBusy);
		sb.append(", items=");
		sb.append(this.list.length);
		sb.append("):");
		for (MainCmdItem item : this.list) {
			sb.append("\n\t");
			sb.append(item.toString());
		}
		return sb.toString();
	}
	
}
