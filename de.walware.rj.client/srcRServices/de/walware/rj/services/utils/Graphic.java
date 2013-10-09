/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.services.utils;

import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;


public abstract class Graphic {
	
	
	public static final String UNIT_PX = "px";
	public static final String UNIT_IN = "in";
	public static final String UNIT_CM = "cm";
	public static final String UNIT_MM = "mm";
	
	
	String sizeUnit;
	double sizeWidth;
	double sizeHeight;
	
	int resolution = -1;
	
	
	protected Graphic() {
	}
	
	
	/**
	 * Sets the size of the graphic.
	 * 
	 * The unit can be one of the constants with prefix UNIT_ .
	 * The default is pixel for raster graphic images (png) and inch for vector images (pdf).
	 * 
	 * @param width the width in the given unit
	 * @param height the height in the given unit 
	 * @param unit the unit of width and height arguments
	 */
	public void setSize(final double width, final double height, final String unit) {
		this.sizeWidth = width;
		this.sizeHeight = height;
		this.sizeUnit = unit;
	}
	
	/**
	 * Sets the nominal resolution in dpi of the graphic.
	 * 
	 * @param resolution the resolution in dpi
	 */
	public void setResolution(final int resolution) {
		this.resolution = resolution;
	}
	
	
	public byte[] create(final FunctionCall plot, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".plot";
		prepare(filename, service, monitor);
		plot.evalVoid(monitor);
		service.evalVoid("dev.off()", monitor);
		return service.downloadFile(filename, 0, monitor);
	}
	
	public void create(final FunctionCall plot, final OutputStream out, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".plot";
		prepare(filename, service, monitor);
		plot.evalVoid(monitor);
		service.evalVoid("dev.off()", monitor);
		service.downloadFile(out, filename, 0, monitor);
	}
	
	public byte[] create(final String plotCommand, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".plot";
		prepare(filename, service, monitor);
		service.evalVoid(plotCommand, monitor);
		service.evalVoid("dev.off()", monitor);
		return service.downloadFile(filename, 0, monitor);
	}
	
	public void create(final String plotCommand, final OutputStream out, final RService service, final IProgressMonitor monitor) throws CoreException {
		final String filename = "plot-"+System.nanoTime()+".plot";
		prepare(filename, service, monitor);
		service.evalVoid(plotCommand, monitor);
		service.evalVoid("dev.off()", monitor);
		service.downloadFile(out, filename, 0, monitor);
	}
	
	protected abstract void prepare(String filename, RService service, IProgressMonitor monitor) throws CoreException;
	
}
