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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.defaultImpl.RLanguageImpl;
import de.walware.rj.data.defaultImpl.RNull;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;


public class FunctionCallImpl implements FunctionCall {
	
	
	private final String name;
	
	private final List<String> argNames = new ArrayList<String>();
	private final List<RObject> argValues = new ArrayList<RObject>();
	
	private final AbstractRJComClient rjs;
	private final RObjectFactory rObjectFactory;
	
	
	public FunctionCallImpl(final AbstractRJComClient client, final String name,
			final RObjectFactory rObjectFactory) {
		this.rjs = client;
		this.rObjectFactory = rObjectFactory;
		this.name = name;
	}
	
	
	@Override
	public FunctionCall add(final String arg, final String expression) {
		if (expression == null) {
			throw new NullPointerException();
		}
		this.argNames.add(arg);
		final RObject data = new RLanguageImpl((byte) 0, expression, null);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall add(final String expression) {
		return this.add(null, expression);
	}
	
	@Override
	public FunctionCall add(final String arg, final RObject data) {
		if (data == null) {
			throw new NullPointerException();
		}
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall add(final RObject data) {
		return this.add(null, data);
	}
	
	@Override
	public FunctionCall addLogi(final String arg, final boolean logical) {
		final RObject data = this.rObjectFactory.createVector(
				this.rObjectFactory.createLogiData(new boolean[] { logical }) );
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall addLogi(final boolean logical) {
		return addLogi(null, logical);
	}
	
	@Override
	public FunctionCall addInt(final String arg, final int integer) {
		final RObject data = this.rObjectFactory.createVector(
				this.rObjectFactory.createIntData(new int[] { integer }) );
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall addInt(final int integer) {
		return addInt(null, integer);
	}
	
	@Override
	public FunctionCall addNum(final String arg, final double numeric) {
		final RObject data = this.rObjectFactory.createVector(
				this.rObjectFactory.createNumData(new double[] { numeric }) );
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall addNum(final double numeric) {
		return this.addNum(null, numeric);
	}
	
	@Override
	public FunctionCall addChar(final String arg, final String character) {
		final RObject data = this.rObjectFactory.createVector(
				this.rObjectFactory.createCharData(new String[] { character }));
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall addChar(final String character) {
		return this.addChar(null, character);
	}
	
	@Override
	public FunctionCall addCplx(final String arg, final double real, final double imaginary) {
		final RObject data = this.rObjectFactory.createVector(
				this.rObjectFactory.createCplxData(new double[] { real }, new double[] {imaginary }) );
		this.argNames.add(arg);
		this.argValues.add(data);
		return this;
	}
	
	@Override
	public FunctionCall addCplx(final double real, final double imaginary) {
		return addCplx(null, real, imaginary);
	}
	
	@Override
	public FunctionCall addNull(final String arg) {
		this.argNames.add(arg);
		this.argValues.add(RNull.INSTANCE);
		return this;
	}
	
	@Override
	public FunctionCall addNull() {
		return this.addNull(null);
	}
	
	
	private RList prepareArgs(final IProgressMonitor monitor) throws CoreException {
		// TODO step by step upload for large objects
		final String[] names = this.argNames.toArray(new String[this.argNames.size()]);
		final RObject[] values = this.argValues.toArray(new RObject[this.argValues.size()]);
		assert (names.length == values.length);
		return this.rObjectFactory.createList(values, names);
	}
	
	@Override
	public void evalVoid(final IProgressMonitor monitor) throws CoreException {
		final RList args = prepareArgs(monitor);
		this.rjs.evalVoid(this.name, args, null, monitor);
	}
	
	@Override
	public RObject evalData(final IProgressMonitor monitor) throws CoreException {
		final RList args = prepareArgs(monitor);
		return this.rjs.evalData(this.name, args, null, null, 0, RService.DEPTH_INFINITE, monitor);
	}
	
	@Override
	public RObject evalData(final String factoryId, final int options, final int depth, final IProgressMonitor monitor) throws CoreException {
		final RList args = prepareArgs(monitor);
		return this.rjs.evalData(this.name, args, null, factoryId, options, depth, monitor);
	}
	
	@Override
	public String toString() {
		final StringBuilder call = new StringBuilder();
		call.append(this.name);
		call.append('(');
		if (this.argNames.size() > 0) {
			for (int i = 0; i < this.argNames.size(); i++) {
				final String argName = this.argNames.get(i);
				if (argName != null) {
					call.append('\n');
					call.append(argName);
					call.append(" = ");
				}
				final Object value = this.argValues.get(i);
				if (value instanceof String) {
					call.append((String) value);
				}
				else if (value instanceof RObject) {
					call.append("\n<DATA>\n");
					call.append(value.toString());
					call.append("\n</DATA>");
				}
			}
			call.append("\n");
		}
		call.append(')');
		return call.toString();
	}
	
}
