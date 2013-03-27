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

package de.walware.rj.server.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.graphic.RGraphic;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RGraphicCreator;
import de.walware.rj.services.RService;


/**
 * Default implementation for {@link RGraphicCreator}.
 * <p>
 * Requires R package <code>rj</code>.</p>
 */
public class RGraphicCreatorImpl implements RGraphicCreator {
	
	
	private final RService service;
	private final AbstractRJComClient rjs;
	
	private int options;
	
	private double width = -1;
	private double height = -1;
	
	
	public RGraphicCreatorImpl(final RService service, final AbstractRJComClient rjs, final int options) {
		this.service = service;
		this.rjs = rjs;
	}
	
	
	@Override
	public void setSize(final double width, final double height) {
		if (width == -1 && height == -1) {
			this.width = -1;
			this.height = -1;
			return;
		}
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException();
		}
		this.width = width;
		this.height = height;
	}
	
	@Override
	public RGraphic create(final String expression, final IProgressMonitor monitor) throws CoreException {
		return create(expression, null, monitor);
	}
	
	@Override
	public RGraphic create(final FunctionCall fcall, final IProgressMonitor monitor) throws CoreException {
		return create(null, fcall, monitor);
	}
	
	private RGraphic create(final String expression, final FunctionCall fcall, final IProgressMonitor monitor) throws CoreException {
		final int savedOptions = this.rjs.getGraphicOptions();
		int graphicOptions = this.options;
		if ((this.options & RClientGraphicFactory.MANAGED_ON) == 0) {
			graphicOptions |= RClientGraphicFactory.MANAGED_OFF;
		}
		if ((this.options & RClientGraphicFactory.R_CLOSE_ON) == 0) {
			graphicOptions |= RClientGraphicFactory.R_CLOSE_OFF;
		}
		this.rjs.setGraphicOptions(graphicOptions);
		final RClientGraphic graphic;
		try {
			if (this.width < 0) {
				this.service.evalVoid("rj.gd::rj.GD()", monitor);
			}
			else {
				this.service.evalVoid("rj.gd::rj.GD(" +
						"width= " + this.width + ", height= "+this.height+", size.unit= \"px\"" +
						")", monitor );
			}
			if (expression != null) {
				this.service.evalData(expression, monitor);
			}
			else {
				fcall.evalVoid(monitor);
			}
			graphic = this.rjs.getLastGraphic();
		}
		finally {
			this.rjs.setGraphicOptions(savedOptions);
		}
		return ((graphic instanceof RGraphic) ? ((RGraphic) graphic) : null);
	}
	
}
