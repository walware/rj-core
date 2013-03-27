/*******************************************************************************
 * Copyright (c) 2011-2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.server.gd.Coord;
import de.walware.rj.server.gd.GraOp;


public abstract class AbstractRJComClientGraphicActions implements RClientGraphicActions {
	
	
	public static interface Factory {
		
		
		AbstractRJComClientGraphicActions create(AbstractRJComClient rjs, Object rHandle);
		
	}
	
	
	protected final AbstractRJComClient rjs;
	
	
	protected AbstractRJComClientGraphicActions(final AbstractRJComClient rjs) {
		if (rjs == null) {
			throw new NullPointerException("rjs");
		}
		this.rjs = rjs;
	}
	
	
	public void doResizeGraphic(final int devId,
			final IProgressMonitor monitor) throws CoreException {
		this.rjs.execSyncGraphicOp(devId, GraOp.OP_REQUEST_RESIZE, monitor);
	}
	
	public void doCloseGraphic(final int devId,
			final IProgressMonitor monitor) throws CoreException {
		this.rjs.execSyncGraphicOp(devId, GraOp.OP_CLOSE, monitor);
	}
	
	
	@Override
	public void copy(final int devId, final String toDev, final String toDevFile, final String toDevArgs,
			final IProgressMonitor monitor) throws CoreException {
		final StringBuilder sb = new StringBuilder(64);
		sb.append("rj.gd::.rj.copyGD(");
		sb.append("devNr=").append((devId + 1)).append("L,");
		sb.append("device=").append(toDev).append(",");
		if (toDevFile != null) {
			sb.append("file=\"").append(toDevFile).append("\",");
		}
		if (toDevArgs != null && toDevArgs.length() > 0) {
			sb.append(toDevArgs);
			sb.append(",");
		}
		sb.replace(sb.length()-1, sb.length(), ")");
		this.rjs.evalVoid(sb.toString(), null, monitor);
	}
	
	@Override
	public double[] convertGraphic2User(final int devId, final double[] xy,
			final IProgressMonitor monitor) throws CoreException {
		if (xy == null) {
			throw new NullPointerException("xy");
		}
		if (xy.length != 2) {
			throw new IllegalArgumentException("length of xy");
		}
		final Coord coord = (Coord) this.rjs.execSyncGraphicOp(devId, GraOp.OP_CONVERT_DEV2USER,
				new Coord(xy[0], xy[1]), monitor );
		if (coord != null && !Double.isNaN(coord.getX()) && !Double.isNaN(coord.getY())) {
			return new double[] { coord.getX(), coord.getY() };
		}
		return null;
	}
	
	@Override
	public double[] convertUser2Graphic(final int devId, final double[] xy,
			final IProgressMonitor monitor) throws CoreException {
		if (xy == null) {
			throw new NullPointerException("xy");
		}
		if (xy.length != 2) {
			throw new IllegalArgumentException("length of xy");
		}
		final Coord coord = (Coord) this.rjs.execSyncGraphicOp(devId, GraOp.OP_CONVERT_USER2DEV,
				new Coord(xy[0], xy[1]), monitor );
		if (coord != null && !Double.isNaN(coord.getX()) && !Double.isNaN(coord.getY())) {
			return new double[] { coord.getX(), coord.getY() };
		}
		return null;
	}
	
}
