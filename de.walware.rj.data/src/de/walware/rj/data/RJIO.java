/*******************************************************************************
 * Copyright (c) 2010 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * IO implementation for RJ data serialization.
 */
public final class RJIO {
	
	
	private static final ThreadLocal<RJIO> INSTANCES = new ThreadLocal<RJIO>() {
		
		@Override
		protected RJIO initialValue() {
			return new RJIO();
		}
		
	};
	
	public static RJIO get(final ObjectOutput out) {
		final RJIO io = INSTANCES.get();
		io.out = out;
		return io;
	}
	
	public static RJIO get(final ObjectInput in) {
		final RJIO io = INSTANCES.get();
		io.in = in;
		return io;
	}
	
	
	private static final int BB_LENGTH = 16384;
	private static final int BA_LENGTH = BB_LENGTH;
	private static final int BB_PART = BB_LENGTH / 4;
	private static final int CB_LENGTH = BB_LENGTH / 2;
	private static final int CA_LENGTH = BB_LENGTH * 4;
	private static final int IB_LENGTH = BB_LENGTH / 4;
	
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	
	private final ByteBuffer bb;
	private final byte[] ba;
	private final CharBuffer cb;
	private final char[] ca;
	private final IntBuffer ib;
	
	public ObjectInput in;
	
	public ObjectOutput out;
	
	public int flags;
	
	
	public RJIO() {
		this.bb = ByteBuffer.allocateDirect(BB_LENGTH);
		if (this.bb.hasArray()) {
			this.ba = this.bb.array();
		}
		else {
			this.ba = new byte[BB_LENGTH];
		}
		this.cb = this.bb.asCharBuffer();
		this.ca = new char[CA_LENGTH];
		this.ib = this.bb.asIntBuffer();
	}
	
	
	public void writeIntArray(final int[] array, final int length) throws IOException {
		final ObjectOutput out = this.out;
		out.writeInt(length);
		if (length <= 32) {
			for (int i = 0; i < length; i++) {
				out.writeInt(array[i]);
			}
		}
		else if (length <= IB_LENGTH) {
			final int bcount = (length << 2);
			this.ib.clear();
			this.ib.put(array, 0, length);
			if (!this.bb.hasArray()) {
				this.bb.clear();
				this.bb.get(this.ba, 0, bcount);
			}
			this.out.write(this.ba, 0, bcount);
		}
		else {
			int iw = 0;
			while (iw < length) {
				final int icount = Math.min(length - iw, IB_LENGTH);
				final int bcount = (icount << 2);
				this.ib.clear();
				this.ib.put(array, iw, icount);
				if (!this.bb.hasArray()) {
					this.bb.clear();
					this.bb.get(this.ba, 0, bcount);
				}
				this.out.write(this.ba, 0, bcount);
				iw += icount;
			}
		}
	}
	
	public void writeByteArray(final byte[] array, final int length) throws IOException {
		final ObjectOutput out = this.out;
		out.writeInt(length);
		out.write(array, 0, length);
	}
	
	@SuppressWarnings("deprecation")
	public void writeString(final String s) throws IOException {
		final ObjectOutput out = this.out;
		if (s != null) {
			final int cn = s.length();
			ASCII: if (cn <= BA_LENGTH) {
				for (int ci = 0; ci < cn; ) {
					if ((s.charAt(ci++) & 0xffffff00) != 0) {
						break ASCII;
					}
				}
				if (cn <= 8) {
					out.writeInt(-cn);
					out.writeBytes(s);
					return;
				}
				else {
					out.writeInt(-cn);
					s.getBytes(0, cn, this.ba, 0);
					out.write(this.ba, 0, cn);
					return;
				}
			}
			out.writeInt(cn);
			out.writeChars(s);
			return;
		}
		else {
			out.writeInt(Integer.MIN_VALUE);
			return;
		}
	}
	
	@SuppressWarnings("deprecation")
	public void writeStringArray(final String[] sa, final int length) throws IOException {
		final ObjectOutput out = this.out;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			final String s = sa[i];
			if (s != null) {
				final int cn = s.length();
				ASCII: if (cn <= BA_LENGTH) {
					for (int ci = 0; ci < cn; ) {
						if ((s.charAt(ci++) & 0xffffff00) != 0) {
							break ASCII;
						}
					}
					if (cn <= 8) {
						out.writeInt(-cn);
						out.writeBytes(s);
						continue;
					}
					else {
						out.writeInt(-cn);
						s.getBytes(0, cn, this.ba, 0);
						out.write(this.ba, 0, cn);
						continue;
					}
				}
				out.writeInt(cn);
				out.writeChars(s);
				continue;
			}
			else {
				out.writeInt(Integer.MIN_VALUE);
				continue;
			}
		}
	}
	
	public void writeStringKeyMap(final Map<String, Object> map) throws IOException {
		final ObjectOutput out = this.out;
		if (map == null) {
			out.writeInt(-1);
			return;
		}
		out.writeInt(map.size());
		for (final Entry<String, Object> entry : map.entrySet()) {
			writeString(entry.getKey());
			out.writeObject(entry.getValue());
		}
	}
	
	public void flush() throws IOException {
		this.out.flush();
	}
	
	
	public int[] readIntArray() throws IOException {
		final ObjectInput in = this.in;
		final int length = in.readInt();
		if (length <= 256) {
			switch (length) {
			case 0:
				return EMPTY_INT_ARRAY;
			case 1:
				return new int[] { in.readInt() };
			case 2:
				return new int[] { in.readInt(), in.readInt() };
			case 3:
				return new int[] { in.readInt(), in.readInt(), in.readInt() };
			case 4:
				return new int[] { in.readInt(), in.readInt(), in.readInt(), in.readInt() };
			default:
				final int bn = length << 2;
				in.readFully(this.ba, 0, bn);
				final int[] array = new int[length];
				for (int ib = 0; ib < bn; ib += 4) {
					array[ib >> 2] = (
							((this.ba[ib] & 0xff) << 24) |
							((this.ba[ib+1] & 0xff) << 16) |
							((this.ba[ib+2] & 0xff) << 8) |
							((this.ba[ib+3] & 0xff)) );
				}
				return array;
			}
		}
		else if (length <= IB_LENGTH) {
			final int bn = length << 2;
			in.readFully(this.ba, 0, bn);
			if (!this.bb.hasArray()) {
				this.bb.clear();
				this.bb.put(this.ba, 0, bn);
			}
			final int[] array = new int[length];
			this.ib.clear();
			this.ib.get(array, 0, length);
			return array;
		}
		else {
			final int[] array = new int[length];
			int ir = 0;
			int position = 0;
			final int bToComplete;
			while (true) {
				position += in.read(this.ba, position, BA_LENGTH-position);
				if (position >= BB_PART) {
					final int icount = (position >> 2);
					final int bcount = (icount << 2);
					if (!this.bb.hasArray()) {
						this.bb.clear();
						this.bb.put(this.ba, 0, bcount);
					}
					this.ib.clear();
					this.ib.get(array, ir, icount);
					ir += icount;
					switch (position - bcount) {
					case 0:
						position = 0;
						break;
					case 1:
						this.ba[0] = this.ba[bcount];
						position = 1;
						break;
					case 2:
						this.ba[0] = this.ba[bcount];
						this.ba[1] = this.ba[bcount+1];
						position = 2;
						break;
					case 3:
						array[ir++] = (
								((this.ba[bcount] & 0xff) << 24) |
								((this.ba[bcount+1] & 0xff) << 16) |
								((this.ba[bcount+2] & 0xff) << 8) |
								((in.read() & 0xff)) );
						position = 0;
						break;
					}
					if (length - ir <= IB_LENGTH) {
						bToComplete = ((length - ir) << 2);
						break;
					}
				}
			}
			if (bToComplete > 0) {
				in.readFully(this.ba, position, bToComplete-position);
				if (!this.bb.hasArray()) {
					this.bb.clear();
					this.bb.put(this.ba, 0, bToComplete);
				}
				this.ib.clear();
				this.ib.get(array, ir, bToComplete >> 2);
			}
			return array;
		}
	}
	
	public byte[] readByteArray() throws IOException {
		final ObjectInput in = this.in;
		final byte[] array = new byte[in.readInt()];
		in.readFully(array);
		return array;
	}
	
	private String readString(final int cn, final char[] ca, final ObjectInput in) throws IOException {
		int cr = 0;
		int position = 0;
		final int bToComplete;
		while (true) {
			position += in.read(this.ba, position, BA_LENGTH-position);
			if (position >= BB_PART) {
				final int icount = (position >> 1);
				final int bcount = (icount << 1);
				if (!this.bb.hasArray()) {
					this.bb.clear();
					this.bb.put(this.ba, 0, bcount);
				}
				this.cb.clear();
				this.cb.get(ca, cr, icount);
				cr += icount;
				if (position - bcount != 0) {
					ca[cr++] = (char) (
							((this.ba[bcount] & 0xff) << 8) |
							((in.read() & 0xff)) );
				}
				position = 0;
				if (cn - cr <= CB_LENGTH) {
					bToComplete = (cn - cr) << 1;
					break;
				}
			}
		}
		if (bToComplete > 0) {
			in.readFully(this.ba, position, bToComplete-position);
			if (!this.bb.hasArray()) {
				this.bb.clear();
				this.bb.put(this.ba, 0, bToComplete);
			}
			this.cb.clear();
			this.cb.get(ca, cr, bToComplete >> 1);
		}
		return new String(ca, 0, cn);
	}
	
	@SuppressWarnings("deprecation")
	public String readString() throws IOException {
		final ObjectInput in = this.in;
		final int cn = in.readInt();
		if (cn >= 0) {
			if (cn == 0) {
				return "";
			}
			else if (cn <= 64) {
				for (int ci = 0; ci < cn; ci++) {
					this.ca[ci] = in.readChar();
				}
				return new String(this.ca, 0, cn);
			}
			else if (cn <= CB_LENGTH) {
				final int bn = cn << 1;
				in.readFully(this.ba, 0, bn);
				if (!this.bb.hasArray()) {
					this.bb.clear();
					this.bb.put(this.ba, 0, bn);
				}
				this.cb.clear();
				this.cb.get(this.ca, 0, cn);
				return new String(this.ca, 0, cn);
			}
			else if (cn <= CA_LENGTH) {
				return readString(cn, this.ca, in);
			}
			else {
				return readString(cn, new char[cn], in);
			}
		}
		else {
			if (cn >= -BA_LENGTH) {
				in.readFully(this.ba, 0, -cn);
				return new String(this.ba, 0, 0, -cn);
			}
			else if (cn != Integer.MIN_VALUE) {
				final byte[] bt = new byte[-cn];
				in.readFully(bt, 0, -cn);
				return new String(bt, 0, 0, -cn);
			}
			else { // cn == Integer.MIN_VALUE
				return null;
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public String[] readStringArray() throws IOException {
		final ObjectInput in = this.in;
		final int length = in.readInt();
		final String[] array = new String[length];
		for (int i = 0; i < length; i++) {
			final int cn = in.readInt();
			if (cn >= 0) {
				if (cn == 0) {
					array[i] = "";
					continue;
				}
				else if (cn <= 64) {
					for (int ci = 0; ci < cn; ci++) {
						this.ca[ci] = in.readChar();
					}
					array[i] = new String(this.ca, 0, cn);
					continue;
				}
				else if (cn <= CB_LENGTH) {
					final int bn = cn << 1;
					in.readFully(this.ba, 0, bn);
					if (!this.bb.hasArray()) {
						this.bb.clear();
						this.bb.put(this.ba, 0, bn);
					}
					this.cb.clear();
					this.cb.get(this.ca, 0, cn);
					array[i] = new String(this.ca, 0, cn);
					continue;
				}
				else if (cn <= CA_LENGTH) {
					array[i] = readString(cn, this.ca, in);
					continue;
				}
				else {
					array[i] = readString(cn, new char[cn], in);
					continue;
				}
			}
			else {
				if (cn >= -BA_LENGTH) {
					in.readFully(this.ba, 0, -cn);
					array[i] = new String(this.ba, 0, 0, -cn);
					continue;
				}
				else if (cn != Integer.MIN_VALUE) {
					final byte[] bt = new byte[-cn];
					in.readFully(bt, 0, -cn);
					array[i] = new String(bt, 0, 0, -cn);
					continue;
				}
//				else { // cn == Integer.MIN_VALUE
//					array[i] = null;
//				}
			}
		}
		return array;
	}
	
	public HashMap<String, Object> readStringKeyHashMap() throws IOException {
		final ObjectInput in = this.in;
		final int length = in.readInt();
		if (length < 0) {
			return null;
		}
		try {
			final HashMap<String, Object> map = new HashMap<String, Object>(length);
			for (int i = 0; i < length; i++) {
				map.put(readString(), in.readObject());
			}
			return map;
		}
		catch (final ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}
	
}
