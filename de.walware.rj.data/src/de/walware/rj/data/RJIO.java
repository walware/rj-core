/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
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
		io.connect(out);
		return io;
	}
	
	public static RJIO get(final ObjectInput in) {
		final RJIO io = INSTANCES.get();
		io.connect(in);
		return io;
	}
	
	
	private static final int BB_LENGTH = 16384;
	private static final int BA_LENGTH = BB_LENGTH;
	private static final int BB_PART = BB_LENGTH / 4;
	private static final int CB_LENGTH = BB_LENGTH / 2;
	private static final int CA_LENGTH = BB_LENGTH * 4;
	private static final int IB_LENGTH = BB_LENGTH / 4;
	private static final int DB_LENGTH = BB_LENGTH / 8;
	
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	
	private static final byte MODE_BBARRAY = 0;
	private static final byte MODE_IPARRAY = 1;
	
	
	private final ByteBuffer bb;
	private final byte[] ba;
	private final CharBuffer cb;
	private final char[] ca;
	private final IntBuffer ib;
	private final DoubleBuffer db;
	private final byte mode;
	
	private ObjectInput in;
	
	private ObjectOutput out;
	
	private int temp;
	
	public int flags;
	
	private int serialKey;
	
	
	public RJIO() {
		this.bb = ByteBuffer.allocateDirect(BB_LENGTH);
		if (this.bb.hasArray()) {
			this.mode = MODE_BBARRAY;
			this.ba = this.bb.array();
		}
		else {
			this.mode = MODE_IPARRAY;
			this.ba = new byte[BB_LENGTH];
		}
		this.cb = this.bb.asCharBuffer();
		this.ca = new char[CA_LENGTH];
		this.ib = this.bb.asIntBuffer();
		this.db = this.bb.asDoubleBuffer();
		
		this.serialKey = (int) System.currentTimeMillis();
	}
	
	
	public void connect(final ObjectOutput out) {
		this.out = out;
	}
	
	public void connect(final ObjectInput in) {
		this.in = in;
	}
	
	public void disconnect(final ObjectOutput out) throws IOException {
		this.out.flush();
		this.out = null;
	}
	
	public void disconnect(final ObjectInput in) throws IOException {
		this.in = null;
	}
	
	
	private void writeFullyBB(final int bn) throws IOException {
		switch (this.mode) {
		case MODE_BBARRAY:
			this.out.write(this.ba, 0, bn);
			return;
//		case MODE_IPARRAY:
		default:
			this.bb.clear();
			this.bb.get(this.ba, 0, bn);
			this.out.write(this.ba, 0, bn);
			return;
		}
	}
	
	private void readFullyBB(final int bn)  throws IOException {
		switch (this.mode) {
		case MODE_BBARRAY:
			this.in.readFully(this.ba, 0, bn);
			return;
//		case MODE_IPARRAY:
		default:
			this.in.readFully(this.ba, 0, bn);
			this.bb.clear();
			this.bb.put(this.ba, 0, bn);
			return;
		}
	}
	
	private void readFullyBB(final int pos, final int bn) throws IOException {
		switch (this.mode) {
		case MODE_BBARRAY:
			this.in.readFully(this.ba, pos, bn);
			return;
//		case MODE_IPARRAY:
		default:
			this.in.readFully(this.ba, pos, bn);
			this.bb.clear();
			this.bb.put(this.ba, 0, pos+bn);
//			this.bb.position(pos);
//			this.bb.put(this.ba, pos, bn);
			return;
		}
	}
	
	
	public void writeDirectly(final byte[] bytes, final int off, final int n) throws IOException {
		this.out.write(bytes, off, n);
	}
	
	
	public void writeByte(final byte value) throws IOException {
		this.out.writeByte(value);
	}
	
	public void writeByte(final int value) throws IOException {
		this.out.writeByte(value);
	}
	
	public void writeBoolean(final boolean value) throws IOException {
		this.out.writeByte(value ? 0x1 : 0x0);
	}
	
	public void writeInt(final int value) throws IOException {
		this.out.writeInt(value);
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
			this.ib.clear();
			this.ib.put(array, 0, length);
			writeFullyBB((length << 2));
		}
		else {
			int iw = 0;
			while (iw < length) {
				final int icount = Math.min(length - iw, IB_LENGTH);
				this.ib.clear();
				this.ib.put(array, iw, icount);
				writeFullyBB((icount << 2));
				iw += icount;
			}
		}
	}
	
	public void writeIntData(final int[] array, final int length) throws IOException {
		final ObjectOutput out = this.out;
		if (length <= 32) {
			for (int i = 0; i < length; i++) {
				out.writeInt(array[i]);
			}
		}
		else if (length <= IB_LENGTH) {
			this.ib.clear();
			this.ib.put(array, 0, length);
			writeFullyBB((length << 2));
		}
		else {
			int iw = 0;
			while (iw < length) {
				final int icount = Math.min(length - iw, IB_LENGTH);
				this.ib.clear();
				this.ib.put(array, iw, icount);
				writeFullyBB((icount << 2));
				iw += icount;
			}
		}
	}
	
	public void writeLong(final long value) throws IOException {
		this.out.writeLong(value);
	}
	
	public byte getVULongGrade(final long value) {
		if ((value & 0xffffffffffffff00L) == 0) {
			return (byte) 0;
		}
		if ((value & 0xffffffffffff0000L) == 0) {
			return (byte) 1;
		}
		if ((value & 0xffffffffff000000L) == 0) {
			return (byte) 2;
		}
		if ((value & 0xffffffff00000000L) == 0) {
			return (byte) 3;
		}
		if ((value & 0xffffff0000000000L) == 0) {
			return (byte) 4;
		}
		if ((value & 0xffff000000000000L) == 0) {
			return (byte) 5;
		}
		if ((value & 0xff00000000000000L) == 0) {
			return (byte) 6;
		}
		return 7;
	}
	
	public void writeVULong(final byte grade, final long value) throws IOException {
		if (grade == 0) {
			this.out.writeByte((int) value);
			return;
		}
		final byte[] ba = this.ba;
		switch (grade) {
//		case 0:
//			ba[0] = (byte) (value);
//			this.out.write(ba, 0, 1);
//			return;
		case 1:
			ba[0] = (byte) (value >>> 8);
			ba[1] = (byte) (value);
			this.out.write(ba, 0, 2);
			return;
		case 2:
			ba[0] = (byte) (value >>> 16);
			ba[1] = (byte) (value >>> 8);
			ba[2] = (byte) (value);
			this.out.write(ba, 0, 3);
			return;
		case 3:
			ba[0] = (byte) (value >>> 24);
			ba[1] = (byte) (value >>> 16);
			ba[2] = (byte) (value >>> 8);
			ba[3] = (byte) (value);
			this.out.write(ba, 0, 4);
			return;
		case 4:
			ba[0] = (byte) (value >>> 32);
			ba[1] = (byte) (value >>> 24);
			ba[2] = (byte) (value >>> 16);
			ba[3] = (byte) (value >>> 8);
			ba[4] = (byte) (value);
			this.out.write(ba, 0, 5);
			return;
		case 5:
			ba[0] = (byte) (value >>> 40);
			ba[1] = (byte) (value >>> 32);
			ba[2] = (byte) (value >>> 24);
			ba[3] = (byte) (value >>> 16);
			ba[4] = (byte) (value >>> 8);
			ba[5] = (byte) (value);
			this.out.write(ba, 0, 6);
			return;
		case 6:
			ba[0] = (byte) (value >>> 48);
			ba[1] = (byte) (value >>> 40);
			ba[2] = (byte) (value >>> 32);
			ba[3] = (byte) (value >>> 24);
			ba[4] = (byte) (value >>> 16);
			ba[5] = (byte) (value >>> 8);
			ba[6] = (byte) (value);
			this.out.write(ba, 0, 7);
			return;
		case 7:
			ba[0] = (byte) (value >>> 56);
			ba[1] = (byte) (value >>> 48);
			ba[2] = (byte) (value >>> 40);
			ba[3] = (byte) (value >>> 32);
			ba[4] = (byte) (value >>> 24);
			ba[5] = (byte) (value >>> 16);
			ba[6] = (byte) (value >>> 8);
			ba[7] = (byte) (value);
			this.out.write(ba, 0, 8);
			return;
		default:
			throw new IOException("Unsupported data format (c = " + grade + ").");
		}
	}
	
	public void writeDouble(final double value) throws IOException {
		this.out.writeDouble(value);
	}
	
	public void writeDoubleData(final double[] array, final int length) throws IOException {
		final ObjectOutput out = this.out;
		if (length <= 16) {
			for (int i = 0; i < length; i++) {
				out.writeLong(Double.doubleToRawLongBits(array[i]));
			}
		}
		else if (length <= DB_LENGTH) {
			this.db.clear();
			this.db.put(array, 0, length);
			writeFullyBB((length << 3));
		}
		else {
			int dw = 0;
			while (dw < length) {
				final int dcount = Math.min(length - dw, DB_LENGTH);
				this.db.clear();
				this.db.put(array, dw, dcount);
				writeFullyBB((dcount << 3));
				dw += dcount;
			}
		}
	}
	
	public void writeByteData(final byte[] array, final int length) throws IOException {
		final ObjectOutput out = this.out;
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
	public void writeStringData(final String[] sa, final int length) throws IOException {
		final ObjectOutput out = this.out;
		ARRAY: for (int i = 0; i < length; i++) {
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
						continue ARRAY;
					}
					else {
						out.writeInt(-cn);
						s.getBytes(0, cn, this.ba, 0);
						out.write(this.ba, 0, cn);
						continue ARRAY;
					}
				}
				out.writeInt(cn);
				out.writeChars(s);
				continue ARRAY;
			}
			else {
				out.writeInt(Integer.MIN_VALUE);
				continue ARRAY;
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
	
	
	public byte readByte() throws IOException {
		return this.in.readByte();
	}
	
	public boolean readBoolean() throws IOException {
		return (this.in.readByte() == 0x1);
	}
	
	public int readInt() throws IOException {
		return this.in.readInt();
	}
	
	public void readIntData(final int[] array, final int length) throws IOException {
		final ObjectInput in = this.in;
		if (length <= 256) {
			switch (length) {
			case 0:
				return;
			case 1:
				array[0] = in.readInt();
				return;
			case 2:
				array[0] = in.readInt();
				array[1] = in.readInt();
				return;
			case 3:
				array[0] = in.readInt();
				array[1] = in.readInt();
				array[2] = in.readInt();
				return;
			case 4:
				array[0] = in.readInt();
				array[1] = in.readInt();
				array[2] = in.readInt();
				array[3] = in.readInt();
				return;
			default:
				final int bn = length << 2;
				in.readFully(this.ba, 0, bn);
				for (int ib = 0; ib < bn; ib += 4) {
					array[ib >>> 2] = (
							((this.ba[ib] & 0xff) << 24) |
							((this.ba[ib+1] & 0xff) << 16) |
							((this.ba[ib+2] & 0xff) << 8) |
							((this.ba[ib+3] & 0xff)) );
				}
				return;
			}
		}
		else if (length <= IB_LENGTH) {
			readFullyBB((length << 2));
			this.ib.clear();
			this.ib.get(array, 0, length);
			return;
		}
		else {
			int ir = 0;
			int position = 0;
			final int bToComplete;
			while (true) {
				position += in.read(this.ba, position, BA_LENGTH-position);
				if (position >= BB_PART) {
					final int icount = (position >>> 2);
					final int bcount = (icount << 2);
					if (this.mode != MODE_BBARRAY) {
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
				readFullyBB(position, bToComplete-position);
				this.ib.clear();
				this.ib.get(array, ir, bToComplete >>> 2);
			}
			return;
		}
	}
	
	public int[] readIntArray() throws IOException {
		final ObjectInput in = this.in;
		final int length = in.readInt();
		if (length <= 256) {
			switch (length) {
			case -1:
				return null;
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
					array[ib >>> 2] = (
							((this.ba[ib] & 0xff) << 24) |
							((this.ba[ib+1] & 0xff) << 16) |
							((this.ba[ib+2] & 0xff) << 8) |
							((this.ba[ib+3] & 0xff)) );
				}
				return array;
			}
		}
		else if (length <= IB_LENGTH) {
			readFullyBB((length << 2));
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
					final int icount = (position >>> 2);
					final int bcount = (icount << 2);
					if (this.mode != MODE_BBARRAY) {
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
				readFullyBB(position, bToComplete-position);
				this.ib.clear();
				this.ib.get(array, ir, bToComplete >>> 2);
			}
			return array;
		}
	}
	
	public long readLong() throws IOException {
		return this.in.readLong();
	}
	
	public long readVULong(final byte grade) throws IOException {
		if (grade == 0) {
			return this.in.readUnsignedByte();
		}
		final byte[] ba = this.ba;
		switch (grade) {
//		case 0:
//			this.in.read(ba, 0, 1);
//			return (ba[0] & 0xff);
		case 1:
			this.in.read(ba, 0, 2);
			return (((ba[0] & 0xff) << 8) |
					(ba[1] & 0xff) );
		case 2:
			this.in.read(ba, 0, 3);
			return (((ba[0] & 0xff) << 16) |
					((ba[1] & 0xff) << 8) |
					(ba[2] & 0xff) );
		case 3:
			this.in.read(ba, 0, 4);
			return (((long) (ba[0] & 0xff) << 24) |
					((ba[1] & 0xff) << 16) |
					((ba[2] & 0xff) << 8) |
					(ba[3] & 0xff) );
		case 4:
			this.in.read(ba, 0, 5);
			return (((long) (ba[0] & 0xff) << 32) |
					((long) (ba[1] & 0xff) << 24) |
					((ba[2] & 0xff) << 16) |
					((ba[3] & 0xff) << 8) |
					(ba[4] & 0xff) );
		case 5:
			this.in.read(ba, 0, 6);
			return (((long) (ba[0] & 0xff) << 40) |
					((long) (ba[1] & 0xff) << 32) |
					((long) (ba[2] & 0xff) << 24) |
					((ba[3] & 0xff) << 16) |
					((ba[4] & 0xff) << 8) |
					(ba[5] & 0xff) );
		case 6:
			this.in.read(ba, 0, 7);
			return (((long) (ba[0] & 0xff) << 48) |
					((long) (ba[1] & 0xff) << 40) |
					((long) (ba[2] & 0xff) << 32) |
					((long) (ba[3] & 0xff) << 24) |
					((ba[4] & 0xff) << 16) |
					((ba[5] & 0xff) << 8) |
					(ba[6] & 0xff) );
		case 7:
			this.in.read(ba, 0, 8);
			return (((long) (ba[0] & 0xff) << 56) |
					((long) (ba[1] & 0xff) << 48) |
					((long) (ba[2] & 0xff) << 40) |
					((long) (ba[3] & 0xff) << 32) |
					((long) (ba[4] & 0xff) << 24) |
					((ba[5] & 0xff) << 16) |
					((ba[6] & 0xff) << 8) |
					(ba[7] & 0xff) );
		default:
			throw new IOException("Unsupported data format (c = " + grade + ").");
		}
	}
	
	public double readDouble() throws IOException {
		return this.in.readDouble();
	}
	
	public void readDoubleData(final double[] array, final int length) throws IOException {
		final ObjectInput in = this.in;
		if (length <= 32) {
			switch (length) {
			case 0:
				return;
			case 1:
				array[0] = Double.longBitsToDouble(in.readLong());
				return;
			case 2:
				array[0] = Double.longBitsToDouble(in.readLong());
				array[1] = Double.longBitsToDouble(in.readLong());
				return;
			default:
				final int bn = length << 3;
				in.readFully(this.ba, 0, bn);
				for (int db = 0; db < bn; db += 8) {
					array[db >>> 3] = Double.longBitsToDouble(
							((long) (this.ba[db] & 0xff) << 56) |
							((long) (this.ba[db+1] & 0xff) << 48) |
							((long) (this.ba[db+2] & 0xff) << 40) |
							((long) (this.ba[db+3] & 0xff) << 32) |
							((long) (this.ba[db+4] & 0xff) << 24) |
							((this.ba[db+5] & 0xff) << 16) |
							((this.ba[db+6] & 0xff) << 8) |
							((this.ba[db+7] & 0xff)) );
				}
				return;
			}
		}
		else if (length <= DB_LENGTH) {
			readFullyBB((length << 3));
			this.db.clear();
			this.db.get(array, 0, length);
			return;
		}
		else {
			int dr = 0;
			int position = 0;
			final int bToComplete;
			while (true) {
				position += in.read(this.ba, position, BA_LENGTH-position);
				if (position >= BB_PART) {
					final int dcount = (position >>> 3);
					final int bcount = (dcount << 3);
					if (this.mode != MODE_BBARRAY) {
						this.bb.clear();
						this.bb.put(this.ba, 0, bcount);
					}
					this.db.clear();
					this.db.get(array, dr, dcount);
					dr += dcount;
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
						this.ba[0] = this.ba[bcount];
						this.ba[1] = this.ba[bcount+1];
						this.ba[2] = this.ba[bcount+2];
						position = 3;
						break;
					case 4:
						this.ba[0] = this.ba[bcount];
						this.ba[1] = this.ba[bcount+1];
						this.ba[2] = this.ba[bcount+2];
						this.ba[3] = this.ba[bcount+3];
						position = 4;
						break;
					case 5:
						this.ba[0] = this.ba[bcount];
						this.ba[1] = this.ba[bcount+1];
						this.ba[2] = this.ba[bcount+2];
						this.ba[3] = this.ba[bcount+3];
						this.ba[4] = this.ba[bcount+4];
						position = 5;
						break;
					case 6:
						array[dr++] = Double.longBitsToDouble(
								((long) (this.ba[bcount] & 0xff) << 56) |
								((long) (this.ba[bcount+1] & 0xff) << 48) |
								((long) (this.ba[bcount+2] & 0xff) << 40) |
								((long) (this.ba[bcount+3] & 0xff) << 32) |
								((long) (this.ba[bcount+4] & 0xff) << 24) |
								((this.ba[bcount+5] & 0xff) << 16) |
								((in.read() & 0xff) << 8) |
								((in.read() & 0xff)) );
						position = 0;
						break;
					case 7:
						array[dr++] = Double.longBitsToDouble(
								((long) (this.ba[bcount] & 0xff) << 56) |
								((long) (this.ba[bcount+1] & 0xff) << 48) |
								((long) (this.ba[bcount+2] & 0xff) << 40) |
								((long) (this.ba[bcount+3] & 0xff) << 32) |
								((long) (this.ba[bcount+4] & 0xff) << 24) |
								((this.ba[bcount+5] & 0xff) << 16) |
								((this.ba[bcount+6] & 0xff) << 8) |
								((in.read() & 0xff)) );
						position = 0;
						break;
					}
					if (length - dr <= DB_LENGTH) {
						bToComplete = ((length - dr) << 3);
						break;
					}
				}
			}
			if (bToComplete > 0) {
				readFullyBB(position, bToComplete-position);
				this.db.clear();
				this.db.get(array, dr, bToComplete >>> 3);
			}
			return;
		}
	}
	
	public double[] readDoubleArray() throws IOException {
		final double[] array = new double[this.temp = this.in.readInt()];
		readDoubleData(array, array.length);
		return array;
	}
	
	public double[] readDoubleArray2() throws IOException {
		final double[] array = new double[this.temp];
		readDoubleData(array, array.length);
		return array;
	}
	
	public void readByteData(final byte[] array, final int length) throws IOException {
		this.in.readFully(array, 0, length);
		return;
	}
	
	public byte[] readByteArray() throws IOException {
		final byte[] array = new byte[this.temp = this.in.readInt()];
		readByteData(array, array.length);
		return array;
	}
	
	public byte[] readByteArray2() throws IOException {
		final byte[] array = new byte[this.temp];
		readByteData(array, array.length);
		return array;
	}
	
	private String readString(final int cn, final char[] ca, final ObjectInput in) throws IOException {
		int cr = 0;
		int position = 0;
		final int bToComplete;
		while (true) {
			position += in.read(this.ba, position, BA_LENGTH-position);
			if (position >= BB_PART) {
				final int icount = (position >>> 1);
				final int bcount = (icount << 1);
				if (this.mode != MODE_BBARRAY) {
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
			readFullyBB(position, bToComplete-position);
			this.cb.clear();
			this.cb.get(ca, cr, bToComplete >>> 1);
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
				readFullyBB((cn << 1));
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
	public void readStringData(final String[] array, final int length) throws IOException {
		final ObjectInput in = this.in;
		ARRAY: for (int i = 0; i < length; i++) {
			final int cn = in.readInt();
			if (cn >= 0) {
				if (cn == 0) {
					array[i] = "";
					continue ARRAY;
				}
				else if (cn <= 64) {
					for (int ci = 0; ci < cn; ci++) {
						this.ca[ci] = in.readChar();
					}
					array[i] = new String(this.ca, 0, cn);
					continue ARRAY;
				}
				else if (cn <= CB_LENGTH) {
					readFullyBB((cn << 1));
					this.cb.clear();
					this.cb.get(this.ca, 0, cn);
					array[i] = new String(this.ca, 0, cn);
					continue ARRAY;
				}
				else if (cn <= CA_LENGTH) {
					array[i] = readString(cn, this.ca, in);
					continue ARRAY;
				}
				else {
					array[i] = readString(cn, new char[cn], in);
					continue ARRAY;
				}
			}
			else {
				if (cn >= -BA_LENGTH) {
					in.readFully(this.ba, 0, -cn);
					array[i] = new String(this.ba, 0, 0, -cn);
					continue ARRAY;
				}
				else if (cn != Integer.MIN_VALUE) {
					final byte[] bt = new byte[-cn];
					in.readFully(bt, 0, -cn);
					array[i] = new String(bt, 0, 0, -cn);
					continue ARRAY;
				}
				else { // cn == Integer.MIN_VALUE
//					array[i] = null;
					continue ARRAY;
				}
			}
		}
		return;
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
	
	
	public int writeCheck1() throws IOException {
		this.out.writeInt(++this.serialKey);
		return this.serialKey;
	}
	
	public void writeCheck2(final int check) throws IOException {
		this.out.writeInt(check);
	}
	
	public int readCheck1() throws IOException {
		return this.in.readInt();
	}
	
	public void readCheck2(final int check) throws IOException {
		if (check != this.in.readInt()) {
			throw new IOException("Corrupted stream detected.");
		}
	}
	
}
