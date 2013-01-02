/*******************************************************************************
 * Copyright (c) 2008-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rosuda.JRI.Rengine;

import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.gd.Coord;
import de.walware.rj.server.srvext.RjsGraphic;


public final class JRIServerGraphics {
	
	
	private static final Comparator<RjsGraphic> GRAPHIC_COMPARATOR = new Comparator<RjsGraphic>() {
		
		public int compare(final RjsGraphic o1, final RjsGraphic o2) {
			return o1.getDevId() - o2.getDevId();
		}
		
	};
	
	
	private final JRIServer server;
	private final Rengine rEngine;
	
	private final List<RjsGraphic> graphicList = new ArrayList<RjsGraphic>();
	
	
	public JRIServerGraphics(final JRIServer server, final Rengine rEngine) {
		this.server = server;
		this.rEngine = rEngine;
	}
	
	
	public void addGraphic(final RjsGraphic graphic) {
		final int idx = Collections.binarySearch(this.graphicList, graphic, GRAPHIC_COMPARATOR);
		if (idx >= 0) {
			this.graphicList.set(idx, graphic);
		}
		else {
			this.graphicList.add(-(idx+1), graphic);
		}
	}
	
	public void removeGraphic(final RjsGraphic graphic) {
		this.graphicList.remove(graphic);
	}
	
	public RjsGraphic getGraphic(final int devId) {
		for (int i = 0; i < this.graphicList.size(); i++) {
			final RjsGraphic graphic = this.graphicList.get(i);
			if (graphic.getDevId() < devId) {
				continue;
			}
			else if (graphic.getDevId() > devId) {
				break;
			}
			else {
				return graphic;
			}
		}
		return null;
	}
	
	
	protected RjsStatus checkReturnCode(final int code) {
		switch (code) {
		case 0:
			return RjsStatus.OK_STATUS;
		case 11: // java
		case 12: // gd
		case 13: // dd
			return new RjsStatus(RjsStatus.ERROR, code, "Graphic is not ready");
		default:
			return new RjsStatus(RjsStatus.ERROR, code);
		}
	}
	public RjsStatus closeGraphic(final int devId) {
		final RjsGraphic graphic = getGraphic(devId);
		if (graphic == null) {
			return RjsStatus.OK_STATUS; // ignore
		}
		this.rEngine.rniGDClose(devId);
		return RjsStatus.OK_STATUS;
	}
	
	public RjsStatus resizeGraphic(final int devId) {
		final RjsGraphic graphic = getGraphic(devId);
		if (graphic == null) {
			return checkReturnCode(11);
		}
		final int code = this.rEngine.rniGDResize(devId);
		return checkReturnCode(code);
	}
	
	public RjsStatus convertDev2User(final int devId, final Coord coord) {
		if (coord == null) {
			throw new NullPointerException("coord");
		}
		final RjsGraphic graphic = getGraphic(devId);
		if (graphic == null || graphic.getState() < RjsGraphic.STATE_OPENED) {
			return checkReturnCode(11);
		}
		if (graphic.getState() < RjsGraphic.STATE_PAGED) {
			coord.setX(Double.NaN);
			coord.setY(Double.NaN);
			return RjsStatus.OK_STATUS;
		}
		else {
			final double[] xy = new double[2];
			xy[0] = coord.getX();
			xy[1] = coord.getY();
			final int code = this.rEngine.rniGDConvertDevToUser(devId, xy);
			if (code == 0) {
				coord.setX(xy[0]);
				coord.setY(xy[1]);
				return RjsStatus.OK_STATUS;
			}
		}
		return new RjsStatus(RjsStatus.ERROR, 0);
	}
	
	public RjsStatus convertUser2Dev(final int devId, final Coord coord) {
		if (coord == null) {
			throw new NullPointerException("coord");
		}
		final RjsGraphic graphic = getGraphic(devId);
		if (graphic == null || graphic.getState() < RjsGraphic.STATE_OPENED) {
			return checkReturnCode(11);
		}
		if (graphic.getState() < RjsGraphic.STATE_PAGED) {
			coord.setX(Double.NaN);
			coord.setY(Double.NaN);
			return RjsStatus.OK_STATUS;
		}
		else {
			final double[] xy = new double[2];
			xy[0] = coord.getX();
			xy[1] = coord.getY();
			final int code = this.rEngine.rniGDConvertUserToDev(devId, xy);
			if (code == 0) {
				coord.setX(xy[0]);
				coord.setY(xy[1]);
				return RjsStatus.OK_STATUS;
			}
			return checkReturnCode(code);
		}
	}
	
}
