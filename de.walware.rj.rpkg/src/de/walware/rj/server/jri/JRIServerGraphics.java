/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import org.rosuda.JRI.Rengine;

import de.walware.rj.server.RJ;
import de.walware.rj.server.RjsStatus;
import de.walware.rj.server.gr.Coord;
import de.walware.rj.server.gr.RjsGraphic;
import de.walware.rj.server.gr.RjsGraphicManager;


public final class JRIServerGraphics extends RjsGraphicManager {
	
	
	private final Rengine rEngine;
	private final JRIServerRni rni;
	
	private final long xySymP;
	private final long devIdSymP;
	private final long convertUser2DevFunP;
	private final long convertDev2UserFunP;
	
	
	public JRIServerGraphics(final RJ server, final Rengine rEngine, final JRIServerRni rni) {
		super(server);
		this.rEngine = rEngine;
		this.rni = rni;
		
		this.xySymP= this.rEngine.rniInstallSymbol("xy");
		this.devIdSymP= this.rEngine.rniInstallSymbol("devId");
		
		this.convertUser2DevFunP= this.rni.protect(this.rEngine.rniEval(
				this.rEngine.rniParse("rj:::.gr.convertUser2Dev", 1 ),
				this.rni.rniSafeBaseExecEnvP ));
		this.convertDev2UserFunP= this.rni.protect(this.rEngine.rniEval(
				this.rEngine.rniParse("rj:::.gr.convertDev2User", 1 ),
				this.rni.rniSafeBaseExecEnvP ));
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
				
				final long p= this.rEngine.rniEval(this.rEngine.rniCons(
								this.convertDev2UserFunP, this.rEngine.rniCons(
										this.rEngine.rniPutDoubleArray(xy), this.rEngine.rniCons(
												this.rEngine.rniPutIntArray(new int[] { devId + 1 }), this.rni.NULL_P,
												this.devIdSymP, false ),
										this.xySymP, false ),
								0, true ),
						this.rni.rniSafeBaseExecEnvP );
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
								this.convertUser2DevFunP, this.rEngine.rniCons(
										this.rEngine.rniPutDoubleArray(xy), this.rEngine.rniCons(
												this.rEngine.rniPutIntArray(new int[] { devId + 1 }), this.rni.NULL_P,
												this.devIdSymP, false ),
										this.xySymP, false ),
								0, true ),
						this.rni.rniSafeBaseExecEnvP );
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
