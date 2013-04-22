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

import org.rosuda.JRI.Rengine;

import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.gr.Coord;
import de.walware.rj.server.gr.RjsGraphic;
import de.walware.rj.server.gr.RjsGraphicManager;


public final class JRIServerGraphics extends RjsGraphicManager {
	
	
	private final Rengine rEngine;
	private final JRIServerRni rni;
	
	private final long p_xySymbol;
	private final long p_devIdSymbol;
	private final long p_convertUser2DevFun;
	private final long p_convertDev2UserFun;
	
	
	public JRIServerGraphics(final JRIServer server, final Rengine rEngine, final JRIServerRni rni) {
		super(server);;
		this.rEngine = rEngine;
		this.rni = rni;
		
		this.p_xySymbol = this.rEngine.rniInstallSymbol("xy");
		this.p_devIdSymbol = this.rEngine.rniInstallSymbol("devId");
		
		this.p_convertUser2DevFun = this.rni.protect(this.rEngine.rniEval(
					this.rEngine.rniParse("rj:::.gr.convertUser2Dev", 1 ), 0));
		this.p_convertDev2UserFun = this.rni.protect(this.rEngine.rniEval(
				this.rEngine.rniParse("rj:::.gr.convertDev2User", 1 ), 0));
	}
	
	
	protected RjsStatus checkReturnCode(final int code) {
		switch (code) {
		case 0:
			return RjsStatus.OK_STATUS;
		case 11: // java
		case 12: // gd
		case 13: // dd
		case 14: // r code
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
			beginOperation();
			try {
				double[] xy = new double[2];
				xy[0] = coord.getX();
				xy[1] = coord.getY();
				
				final long p = this.rEngine.rniEval(this.rEngine.rniCons(
						this.p_convertDev2UserFun, this.rEngine.rniCons(
								this.rEngine.rniPutDoubleArray(xy), this.rEngine.rniCons(
										this.rEngine.rniPutIntArray(new int[] { devId + 1 }), this.rni.p_NULL,
										this.p_devIdSymbol, false ),
								this.p_xySymbol, false ),
						0, true ), this.rni.p_BaseEnv );
				if (p != 0) {
					xy = this.rEngine.rniGetDoubleArray(p);
					if (xy != null) {
						coord.setX(xy[0]);
						coord.setY(xy[1]);
						return RjsStatus.OK_STATUS;
					}
				}
			}
			finally {
				endOperation();
			}
			return checkReturnCode(14);
		}
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
			beginOperation();
			try {
				double[] xy = new double[2];
				xy[0] = coord.getX();
				xy[1] = coord.getY();
				
				final long p = this.rEngine.rniEval(this.rEngine.rniCons(
						this.p_convertUser2DevFun, this.rEngine.rniCons(
								this.rEngine.rniPutDoubleArray(xy), this.rEngine.rniCons(
										this.rEngine.rniPutIntArray(new int[] { devId + 1 }), this.rni.p_NULL,
										this.p_devIdSymbol, false ),
								this.p_xySymbol, false ),
						0, true ), this.rni.p_BaseEnv );
				if (p != 0) {
					xy = this.rEngine.rniGetDoubleArray(p);
					if (xy != null) {
						coord.setX(xy[0]);
						coord.setY(xy[1]);
						return RjsStatus.OK_STATUS;
					}
				}
			}
			finally {
				endOperation();
			}
			return checkReturnCode(14);
		}
	}
	
}
