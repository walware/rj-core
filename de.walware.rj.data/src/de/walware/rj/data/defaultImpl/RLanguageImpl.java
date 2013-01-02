/*******************************************************************************
 * Copyright (c) 2010-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data.defaultImpl;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RLanguage;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;


public class RLanguageImpl extends AbstractRObject
		implements RLanguage, ExternalizableRObject {
	
	
	public static final String getBaseClassname(final byte type) {
		switch (type) {
		case NAME:
			return CLASSNAME_NAME;
		case CALL:
			return CLASSNAME_CALL;
		default:
			return CLASSNAME_EXPRESSION;
		}
	}
	
	
	private byte type;
	
	private String className1;
	
	private String source;
	
	
	public RLanguageImpl(final byte type, final String source, final String className1) {
		this.type = type;
		this.className1 = (className1 != null) ? className1 : getBaseClassname(type);
		this.source = source;
	}
	
	public RLanguageImpl(final byte type, final String className1) {
		this.type = type;
		this.className1 = (className1 != null) ? className1 : getBaseClassname(type);
	}
	
	public RLanguageImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int options = io.readInt();
		this.type = io.readByte();
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ? io.readString() :
				getBaseClassname(this.type);
		//-- data
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0) {
			this.source = io.readString();
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		int options = 0;
		final boolean customClass = !this.className1.equals(getBaseClassname(this.type));
		if (customClass) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		io.writeInt(options);
		io.writeByte(this.type);
		//-- special attributes
		if (customClass) {
			io.writeString(this.className1);
		}
		//-- data
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0) {
			io.writeString(this.source);
		}
	}
	
	
	public byte getRObjectType() {
		return TYPE_LANGUAGE;
	}
	
	public byte getLanguageType() {
		return this.type;
	}
	
	public String getRClassName() {
		return this.className1;
	}
	
	
	public int getLength() {
		return 0;
	}
	
	public String getSource() {
		return this.source;
	}
	
	public RStore getData() {
		return null;
	}
	
}
