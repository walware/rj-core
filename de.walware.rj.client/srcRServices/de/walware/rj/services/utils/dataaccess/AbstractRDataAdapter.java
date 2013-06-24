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

import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RVector;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.RService;


/**
 * Adapter interface to load R data in one- or two-dimensional fragments.
 * 
 * @param <S> type of R object
 * @param <V> type of R object value fragments
 * @since 2.0 (provisional)
 */
public abstract class AbstractRDataAdapter<S extends RObject, V extends RObject> {
	
	
	public static final int ROW_COUNT = 1 << 0;
	public static final int STORE_TYPE = 1 << 2;
	
	
	public abstract S validate(RObject rObject)
			throws UnexpectedRDataException;
	public abstract S validate(RObject rObject, S referenceObject, int flags)
			throws UnexpectedRDataException;
	
	public abstract long getRowCount(S rObject);
	public abstract long getColumnCount(S rObject);
	
	
	public void check(final String expression, final S referenceObject,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject result;
		{	final FunctionCall fcall = r.createFunctionCall("rj:::.checkDataStruct"); //$NON-NLS-1$
			fcall.add("x", expression); //$NON-NLS-1$
			fcall.addChar("xClass1", referenceObject.getRClassName()); //$NON-NLS-1$
			fcall.add("xDim", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					getRowCount(referenceObject),
					getColumnCount(referenceObject),
			}));
			result = fcall.evalData(monitor);
		}
		if (RDataUtil.checkSingleLogiValue(result) == false) {
			throw new UnexpectedRDataException("It seems something changed.");
		}
	}
	
	public V loadData(final String expression, final S referenceObject,
			final LazyRStore.Fragment<V> fragment, final String rowMapping,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall(getLoadDataFName());
			fcall.add("x", expression); //$NON-NLS-1$
			fcall.add("idxs", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					fragment.getRowBeginIdx() + 1,
					fragment.getRowEndIdx(),
					fragment.getColumnBeginIdx() + 1,
					fragment.getColumnEndIdx(),
			}));
			if (rowMapping != null) {
				fcall.addChar("rowMapping", rowMapping); //$NON-NLS-1$
			}
			
			fragmentObject = fcall.evalData(monitor);
		}
		
		return validateData(fragmentObject, referenceObject, fragment);
	}
	
	protected abstract String getLoadDataFName();
	
	protected abstract V validateData(RObject rObject, S referenceObject,
			LazyRStore.Fragment<V> fragment)
			throws UnexpectedRDataException;
	
	public RVector<?> loadRowNames(final String expression, final S referenceObject,
			final LazyRStore.Fragment<RVector<?>> fragment, final String rowMapping,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall(getLoadRowNamesFName());
			fcall.add("x", expression); //$NON-NLS-1$
			fcall.add("idxs", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					fragment.getRowBeginIdx() + 1,
					fragment.getRowEndIdx(),
			}));
			if (rowMapping != null) {
				fcall.addChar("rowMapping", rowMapping); //$NON-NLS-1$
			}
			
			fragmentObject = fcall.evalData(monitor);
		}
		
		return validateRowNames(fragmentObject, referenceObject, fragment);
	}
	
	protected abstract String getLoadRowNamesFName();
	
	protected RVector<?> validateRowNames(final RObject rObject, final S referenceObject,
			final LazyRStore.Fragment<RVector<?>> fragment)
			throws UnexpectedRDataException {
		if (rObject.getRObjectType() == RObject.TYPE_NULL) {
			return null;
		}
		
		final RVector<?> vector = RDataUtil.checkRVector(rObject);
		RDataUtil.checkLengthEqual(vector.getData(), fragment.getRowCount());
		
		return vector;
	}
	
}
