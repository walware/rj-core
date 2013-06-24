/*******************************************************************************
 * Copyright (c) 2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.services.utils.dataaccess;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.data.RArray;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RVector;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;
import de.walware.rj.services.utils.dataaccess.LazyRStore.Fragment;


/**
 * Data adapter for {@link RArray} objects of any dimension, loaded as one-dimensional vectors.
 * 
 * @since 2.0 (provisional)
 */
public class RArrayAsVectorDataAdapter extends AbstractRDataAdapter<RArray<?>, RVector<?>> {
	
	
	@Override
	public RArray<?> validate(final RObject rObject) throws UnexpectedRDataException {
		return RDataUtil.checkRArray(rObject);
	}
	
	@Override
	public RArray<?> validate(final RObject rObject, final RArray<?> referenceObject,
			final int flags) throws UnexpectedRDataException {
		final RArray<?> array = RDataUtil.checkRArray(rObject, referenceObject.getDim().getLength());
		// check dim ?
		if ((flags & ROW_COUNT) != 0) {
			RDataUtil.checkLengthEqual(array, referenceObject.getLength());
		}
		if ((flags & STORE_TYPE) != 0) {
			RDataUtil.checkData(array.getData(), referenceObject.getData().getStoreType());
		}
		return array;
	}
	
	@Override
	public long getRowCount(final RArray<?> rObject) {
		return rObject.getLength();
	}
	
	@Override
	public long getColumnCount(final RArray<?> rObject) {
		return 1;
	}
	
	
	@Override
	protected String getLoadDataFName() {
		return "rj:::.getDataVectorValues"; //$NON-NLS-1$
	}
	
	@Override
	protected RVector<?> validateData(final RObject rObject, final RArray<?> referenceObject,
			final Fragment<RVector<?>> fragment) throws UnexpectedRDataException {
		final RVector<?> vector = RDataUtil.checkRVector(rObject);
		RDataUtil.checkLengthEqual(vector, fragment.getRowCount());
		
		RDataUtil.checkData(rObject.getData(), referenceObject.getData().getStoreType());
		
		return vector;
	}
	
	@Override
	protected String getLoadRowNamesFName() {
		throw new UnsupportedOperationException();
	}
	
	public RVector<?> loadDimNames(final String expression, final RArray<?> referenceObject,
			final LazyRStore.Fragment<RVector<?>> fragment,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall("rj:::.getDataArrayDimNames"); //$NON-NLS-1$
			fcall.add("x", expression); //$NON-NLS-1$
			fcall.add("idxs", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					fragment.getRowBeginIdx() + 1,
					fragment.getRowEndIdx(),
			}));
			
			fragmentObject = fcall.evalData(monitor);
		}
		
		return validateRowNames(fragmentObject, referenceObject, fragment);
	}
	
	public RVector<?> loadDimItemNames(final String expression, final RArray<?> referenceObject,
			final int dim, final LazyRStore.Fragment<RVector<?>> fragment,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall("rj:::.getDataArrayDimItemNames"); //$NON-NLS-1$
			fcall.add("x", expression); //$NON-NLS-1$
			fcall.addInt("dimIdx", dim + 1); //$NON-NLS-1$
			fcall.add("idxs", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					fragment.getRowBeginIdx() + 1,
					fragment.getRowEndIdx(),
		}));
		
		fragmentObject = fcall.evalData(monitor);
		}
		
		return validateRowNames(fragmentObject, referenceObject, fragment);
	}
	
}
