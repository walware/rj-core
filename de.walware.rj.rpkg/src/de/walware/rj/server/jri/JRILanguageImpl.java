/*******************************************************************************
 * Copyright (c) 2010-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import java.io.IOException;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RLanguage;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.AbstractRObject;
import de.walware.rj.data.defaultImpl.ExternalizableRObject;
import de.walware.rj.data.defaultImpl.RLanguageImpl;


public class JRILanguageImpl extends AbstractRObject
		implements RLanguage, ExternalizableRObject {
	
	
	private byte type;
	
	private String className1;
	
	private String source;
	
	
	public JRILanguageImpl(final byte type, final String source, final String className1) {
		this.type = type;
		this.className1 = className1;
		this.source = source;
	}
	
	public JRILanguageImpl(final byte type, final String className1) {
		this.type = type;
		this.className1 = className1;
	}
	
	public JRILanguageImpl(final RJIO io, final RObjectFactory factory) throws IOException {
		readExternal(io, factory);
	}
	
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		final int options = io.readInt();
		this.type = io.readByte();
		
		//-- special attributes
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ? io.readString() :
				RLanguageImpl.getBaseClassname(this.type);
		//-- data
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0) {
			this.source = io.readString();
		}
	}
	
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		int options = 0;
		final boolean customClass = (this.className1 != null
				&& !this.className1.equals(RLanguageImpl.getBaseClassname(this.type)) );
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
