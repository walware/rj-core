/*******************************************************************************
 * Copyright (c) 2008 Stephan Wahlbrink and others.
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
			case T_CONSOLE_WRITE_ITEM:
			case T_MESSAGE_ITEM:
				this.list[i] = new ConsoleCmdItem(type, in);
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
	
}
