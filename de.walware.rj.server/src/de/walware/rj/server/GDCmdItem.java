/*******************************************************************************
 * Copyright (c) 2009-2011 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server;

import java.io.IOException;
import java.util.Arrays;

import de.walware.rj.data.RJIO;


/**
 * Command item for GD.
 */
public abstract class GDCmdItem extends MainCmdItem {
	
	
	public static final byte SET_CLIP = 1;
	public static final byte SET_COLOR = 2;
	public static final byte SET_FILL = 3;
	public static final byte SET_LINE = 4;
	public static final byte SET_FONT = 5;
	
	public static final byte DRAW_LINE = 6;
	public static final byte DRAW_RECTANGLE = 7;
	public static final byte DRAW_POLYLINE = 8;
	public static final byte DRAW_POLYGON = 9;
	public static final byte DRAW_CIRCLE = 10;
	public static final byte DRAW_TEXT = 11;
	
	public static final byte C_NEW_PAGE = 12;
	public static final byte C_CLOSE_DEVICE = 13;
	public static final byte C_GET_SIZE = 14;
	public static final byte C_SET_ACTIVE_OFF = 15;
	public static final byte C_SET_ACTIVE_ON = 16;
	public static final byte C_SET_MODE = 17;
	public static final byte C_GET_FONTMETRIC = 18;
	public static final byte C_GET_STRINGWIDTH = 19;
	
	public static final byte U_LOCATOR =                    0x20;
	
	
	private static final double[] NO_DATA = new double[0];
	
	
	public static final class Answer extends GDCmdItem {
		
		
		private final double[] data;
		
		
		public Answer(final byte requestId, final int devId, final double[] data) {
			this.options = OM_WAITFORCLIENT;
			this.devId = devId;
			this.data = (data != null) ? data : NO_DATA;
			this.requestId = requestId;
		}
		
		public Answer(final byte requestId, final int devId, final RjsStatus status) {
			this.options = OM_WAITFORCLIENT | (status.getSeverity() << OS_STATUS);
			this.devId = devId;
			this.data = NO_DATA;
			this.requestId = requestId;
		}
		
		/**
		 * Constructor for deserialization
		 */
		public Answer(final RJIO io) throws IOException, ClassNotFoundException {
			this.options = io.readInt();
			this.devId = io.readInt();
			this.requestId = io.readByte();
			final int length = io.readInt();
			this.data = new double[length];
			for (int i = 0; i < length; i++) {
				this.data[i] = io.readDouble();
			}
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(this.requestId);
			final int length = this.data.length;
			io.writeInt(length);
			for (int i = 0; i < length; i++) {
				io.writeDouble(this.data[i]);
			}
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			return this.data;
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer(100 + this.data.length*5);
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=ANSWER");
			sb.append(")");
			sb.append("\n<GD-DATA>\n");
			sb.append(Arrays.toString(this.data));
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class CInit extends GDCmdItem {
		
		
		private final double w;
		private final double h;
		private final boolean active;
		
		
		public CInit(final int devId, final double w, final double h, final boolean activate, final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.w = w;
			this.h = h;
			this.active = activate;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_NEW_PAGE);
			io.writeDouble(this.w);
			io.writeDouble(this.h);
			io.writeBoolean(this.active);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_NEW_PAGE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class CCloseDevice extends GDCmdItem {
		
		
		public CCloseDevice(final int devId, final byte slot) {
			this.options = 0;
			this.devId = devId;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_CLOSE_DEVICE);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_CLOSE_DEVICE);
			sb.append(")\n<GD-DATA />");
			return sb.toString();
		}
		
	}
	
	public static final class CGetSize extends GDCmdItem {
		
		
		public CGetSize(final int devId, final byte slot) {
			this.options = OV_WAITFORCLIENT;
			this.devId = devId;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_GET_SIZE);
			io.writeByte(this.requestId);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_GET_SIZE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class CSetActiveOff extends GDCmdItem {
		
		
		public CSetActiveOff(final int devId, final byte slot) {
			this.options = 0;
			this.devId = devId;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_SET_ACTIVE_OFF);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_SET_ACTIVE_OFF);
			sb.append(")\n<GD-DATA />");
			return sb.toString();
		}
		
	}
	
	public static final class CSetActiveOn extends GDCmdItem {
		
		
		public CSetActiveOn(final int devId, final byte slot) {
			this.options = 0;
			this.devId = devId;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_SET_ACTIVE_ON);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_SET_ACTIVE_ON);
			sb.append(")\n<GD-DATA />");
			return sb.toString();
		}
		
	}
	
	public static final class CSetMode extends GDCmdItem {
		
		
		private final byte mode;
		
		
		public CSetMode(final int devId, final int mode, final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.mode = (byte) mode;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_SET_MODE);
			io.writeByte(this.mode);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_SET_MODE);
			sb.append(")<GD-DATA>\nmode = ");
			sb.append(this.mode);
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class CGetFontMetric extends GDCmdItem {
		
		
		private final int c;
		
		
		public CGetFontMetric(final int devId, final int c, final byte slot) {
			this.options = OV_WAITFORCLIENT;
			this.devId = devId;
			this.c = c;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_GET_FONTMETRIC);
			io.writeByte(this.requestId);
			io.writeInt(this.c);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_GET_FONTMETRIC);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class CGetStrWidth extends GDCmdItem {
		
		
		private final String text;
		
		
		public CGetStrWidth(final int devId, final String text, final byte slot) {
			this.options = OV_WAITFORCLIENT;
			this.devId = devId;
			this.text = text;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(C_GET_STRINGWIDTH);
			io.writeByte(this.requestId);
			io.writeString(this.text);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			this.options = (this.options & OM_CLEARFORANSWER) | (status.getSeverity() << OS_STATUS);
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(C_GET_STRINGWIDTH);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class SetClip extends GDCmdItem {
		
		
		private final double x0;
		private final double y0;
		private final double x1;
		private final double y1;
		
		
		public SetClip(final int devId, final double x0, final double y0, final double x1, final double y1,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(SET_CLIP);
			io.writeDouble(this.x0);
			io.writeDouble(this.y0);
			io.writeDouble(this.x1);
			io.writeDouble(this.y1);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(SET_CLIP);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class SetColor extends GDCmdItem {
		
		
		private final int cc;
		
		
		public SetColor(final int devId, final int cc, final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.cc = cc;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(SET_COLOR);
			io.writeInt(this.cc);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(SET_COLOR);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class SetFill extends GDCmdItem {
		
		
		private final int cc;
		
		
		public SetFill(final int devId, final int cc, final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.cc = cc;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(SET_FILL);
			io.writeInt(this.cc);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(SET_FILL);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class SetLine extends GDCmdItem {
		
		
		private final int type;
		private final double width;
		
		
		public SetLine(final int devId, final int type, final double width,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.type = type;
			this.width = width;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(SET_LINE);
			io.writeInt(this.type);
			io.writeDouble(this.width);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(SET_LINE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class SetFont extends GDCmdItem {
		
		
		private final String family;
		private final int face;
		private final double pointSize;
		private final double cex;
		private final double lineHeight;
		
		
		public SetFont(final int devId, final String family, final int face,
				final double pointSize, final double cex, final double lineHeight,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.family = family;
			this.face = face;
			this.pointSize = pointSize;
			this.cex = cex;
			this.lineHeight = lineHeight;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(SET_FONT);
			io.writeString(this.family);
			io.writeInt(this.face);
			io.writeDouble(this.pointSize);
			io.writeDouble(this.cex);
			io.writeDouble(this.lineHeight);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(SET_FONT);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawLine extends GDCmdItem {
		
		
		private final double x0;
		private final double y0;
		private final double x1;
		private final double y1;
		
		
		public DrawLine(final int devId, final double x0, final double y0, final double x1, final double y1,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_LINE);
			io.writeDouble(this.x0);
			io.writeDouble(this.y0);
			io.writeDouble(this.x1);
			io.writeDouble(this.y1);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_LINE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawRect extends GDCmdItem {
		
		
		private final double x0;
		private final double y0;
		private final double x1;
		private final double y1;
		
		
		public DrawRect(final int devId, final double x0, final double y0, final double x1, final double y1,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_RECTANGLE);
			io.writeDouble(this.x0);
			io.writeDouble(this.y0);
			io.writeDouble(this.x1);
			io.writeDouble(this.y1);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_RECTANGLE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawPolyline extends GDCmdItem {
		
		
		private final double x[];
		private final double y[];
		
		
		public DrawPolyline(final int devId, final double x[], final double y[],
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x = x;
			this.y = y;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_POLYLINE);
			final int n = this.x.length;
			io.writeInt(n);
			for (int i = 0; i < n; i++) {
				io.writeDouble(this.x[i]);
			}
			for (int i = 0; i < n; i++) {
				io.writeDouble(this.y[i]);
			}
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_POLYLINE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawPolygon extends GDCmdItem {
		
		
		private final double x[];
		private final double y[];
		
		
		public DrawPolygon(final int devId, final double x[], final double y[],
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x = x;
			this.y = y;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_POLYGON);
			final int n = this.x.length;
			io.writeInt(n);
			for (int i = 0; i < n; i++) {
				io.writeDouble(this.x[i]);
			}
			for (int i = 0; i < n; i++) {
				io.writeDouble(this.y[i]);
			}
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_POLYGON);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawCircle extends GDCmdItem {
		
		
		private final double x;
		private final double y;
		private final double r;
		
		
		public DrawCircle(final int devId, final double x, final double y, final double r,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x = x;
			this.y = y;
			this.r = r;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_CIRCLE);
			io.writeDouble(this.x);
			io.writeDouble(this.y);
			io.writeDouble(this.r);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_CIRCLE);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class DrawText extends GDCmdItem {
		
		
		private final double x;
		private final double y;
		private final double hAdj;
		private final double rDeg;
		private final String text;
		
		
		public DrawText(final int devId, final double x, final double y,
				final double hAdj, final double rDeg, final String text,
				final byte slot) {
			this.options = 0;
			this.devId = devId;
			this.x = x;
			this.y = y;
			this.hAdj = hAdj;
			this.rDeg = rDeg;
			this.text = text;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(DRAW_TEXT);
			io.writeDouble(this.x);
			io.writeDouble(this.y);
			io.writeDouble(this.hAdj);
			io.writeDouble(this.rDeg);
			io.writeString(this.text);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(DRAW_TEXT);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	public static final class Locator extends GDCmdItem {
		
		
		public Locator(final int devId,
				final byte slot) {
			this.options = OV_WAITFORCLIENT;
			this.devId = devId;
			
			this.slot = slot;
		}
		
		@Override
		public void writeExternal(final RJIO io) throws IOException {
			io.writeInt(this.options);
			io.writeInt(this.devId);
			io.writeByte(U_LOCATOR);
			io.writeByte(this.requestId);
		}
		
		
		@Override
		public void setAnswer(final RjsStatus status) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public double[] getData() {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append("GDCmdItem (options=0x");
			sb.append(Integer.toHexString(this.options));
			sb.append(", device=");
			sb.append(this.devId);
			sb.append(", commandId=");
			sb.append(U_LOCATOR);
			sb.append(")\n<GD-DATA>\n");
			sb.append("\n</GD-DATA>");
			return sb.toString();
		}
		
	}
	
	
	protected int devId;
	
	
	/**
	 * Constructor for automatic deserialization
	 */
	protected GDCmdItem() {
	}
	
	
	@Override
	public final byte getCmdType() {
		return T_GRAPH_ITEM;
	}
	
	@Override
	public byte getOp() {
		return 0;
	}
	
	@Override
	public final boolean isOK() {
		return ((this.options & OM_STATUS) == RjsStatus.OK);
	}
	
	@Override
	public final RjsStatus getStatus() {
		return null;
	}
	
	public final int getDeviceId() {
		return this.devId;
	}
	
	public abstract double[] getData();
	
	@Override
	public final String getDataText() {
		return null;
	}
	
	
	@Override
	public boolean testEquals(final MainCmdItem other) {
		if (!(other instanceof GDCmdItem)) {
			return false;
		}
		final GDCmdItem otherItem = (GDCmdItem) other;
		if (this.options != otherItem.options) {
			return false;
		}
		if (getDeviceId() != otherItem.getDeviceId()) {
			return false;
		}
		return true;
	}
	
}
