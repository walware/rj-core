/*=============================================================================#
 # Copyright (c) 2010-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of either (per the licensee's choosing)
 #   - the Eclipse Public License v1.0
 #     which accompanies this distribution, and is available at
 #     http://www.eclipse.org/legal/epl-v10.html, or
 #   - the GNU Lesser General Public License v2.1 or newer
 #     which accompanies this distribution, and is available at
 #     http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

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
		//-- options
		final int options = io.readInt();
		//-- special attributes
		this.type = io.readByte();
		this.className1 = ((options & RObjectFactory.O_CLASS_NAME) != 0) ?
				io.readString() :
				getBaseClassname(this.type);
		//-- data
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0) {
			this.source = io.readString();
		}
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		int options = 0;
		if (!this.className1.equals(getBaseClassname(this.type))) {
			options |= RObjectFactory.O_CLASS_NAME;
		}
		io.writeInt(options);
		io.writeByte(this.type);
		//-- special attributes
		if ((options & RObjectFactory.O_CLASS_NAME) != 0) {
			io.writeString(this.className1);
		}
		//-- data
		if ((io.flags & RObjectFactory.F_ONLY_STRUCT) == 0) {
			io.writeString(this.source);
		}
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_LANGUAGE;
	}
	
	@Override
	public byte getLanguageType() {
		return this.type;
	}
	
	@Override
	public String getRClassName() {
		return this.className1;
	}
	
	
	@Override
	public long getLength() {
		return 0;
	}
	
	@Override
	public String getSource() {
		return this.source;
	}
	
	@Override
	public RStore<?> getData() {
		return null;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RObject type=RLanguage, class=").append(getRClassName());
		if (this.source != null) {
			sb.append("\n\tsource: ");
			final int idx= this.source.indexOf('\n');
			if (idx >= 0) {
				sb.append(this.source.substring(0, idx));
				sb.append(" ...");
			}
			else {
				sb.append(this.source);
			}
		}
		return sb.toString();
	}
	
}
