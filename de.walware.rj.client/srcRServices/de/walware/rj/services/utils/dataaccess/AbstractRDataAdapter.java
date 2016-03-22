/*=============================================================================#
 # Copyright (c) 2013-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services.utils.dataaccess;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RLanguage;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RVector;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.data.defaultImpl.RObjectFactoryImpl;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.IFQRObjectRef;
import de.walware.rj.services.RService;


/**
 * Adapter interface to load R data in one- or two-dimensional fragments.
 * 
 * @param <TRObject> type of R object
 * @param <TFragmentObject> type of R object value fragments
 * @since 2.0 (provisional)
 */
public abstract class AbstractRDataAdapter<TRObject extends RObject, TFragmentObject extends RObject> {
	
	
	public static final int ROW_COUNT = 1 << 0;
	public static final int STORE_TYPE = 1 << 2;
	
	
	protected static void addXRef(final FunctionCall fcall, final IFQRObjectRef ref) {
		fcall.add("x.env", ref.getEnv()); //$NON-NLS-1$
		fcall.add("x.expr", RObjectFactoryImpl.INSTANCE.createExpression( //$NON-NLS-1$
				"x.env$" + ((RLanguage) ref.getName()).getSource() )); //$NON-NLS-1$
	}
	
	
	public abstract TRObject validate(RObject rObject)
			throws UnexpectedRDataException;
	public abstract TRObject validate(RObject rObject, TRObject referenceObject, int flags)
			throws UnexpectedRDataException;
	
	public abstract long getRowCount(TRObject rObject);
	public abstract long getColumnCount(TRObject rObject);
	
	
	public void check(final IFQRObjectRef ref, final TRObject referenceObject,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject result;
		{	final FunctionCall fcall = r.createFunctionCall("rj:::.checkDataStruct"); //$NON-NLS-1$
			addXRef(fcall, ref);
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
	
	public TFragmentObject loadData(final IFQRObjectRef ref, final TRObject referenceObject,
			final LazyRStore.Fragment<TFragmentObject> fragment, final String rowMapping,
			final RService r,
			final IProgressMonitor monitor) throws CoreException, UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall(getLoadDataFName());
			addXRef(fcall, ref);
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
	
	protected abstract TFragmentObject validateData(RObject rObject, TRObject referenceObject,
			LazyRStore.Fragment<TFragmentObject> fragment)
			throws UnexpectedRDataException;
	
	public void setData(final IFQRObjectRef ref, final TRObject referenceObject,
			final RDataAssignment assignment, final String rowMapping,
			final RService r,
			final IProgressMonitor monitor) throws CoreException, UnexpectedRDataException {
		{	final FunctionCall fcall = r.createFunctionCall(getSetDataFName());
			addXRef(fcall, ref);
			fcall.add("idxs", RObjectFactoryImpl.INSTANCE.createNumVector(new double[] { //$NON-NLS-1$
					assignment.getRowBeginIdx() + 1,
					assignment.getRowEndIdx(),
					assignment.getColumnBeginIdx() + 1,
					assignment.getColumnEndIdx(),
			}));
			if (rowMapping != null) {
				fcall.addChar("rowMapping", rowMapping); //$NON-NLS-1$
			}
			fcall.add("values", RObjectFactoryImpl.INSTANCE.createVector( //$NON-NLS-1$
					assignment.getData() ));
			
			fcall.evalVoid(monitor);
		}
	}
	
	protected abstract String getSetDataFName();
	
	public RVector<?> loadRowNames(final IFQRObjectRef ref, final TRObject referenceObject,
			final LazyRStore.Fragment<RVector<?>> fragment, final String rowMapping,
			final RService r, final IProgressMonitor monitor) throws CoreException,
			UnexpectedRDataException {
		final RObject fragmentObject;
		{	final FunctionCall fcall = r.createFunctionCall(getLoadRowNamesFName());
			addXRef(fcall, ref);
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
	
	protected RVector<?> validateRowNames(final RObject rObject, final TRObject referenceObject,
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
