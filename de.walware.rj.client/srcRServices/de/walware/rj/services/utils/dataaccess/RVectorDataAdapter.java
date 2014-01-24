/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.services.utils.dataaccess;

import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RVector;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.services.utils.dataaccess.LazyRStore.Fragment;


/**
 * Data adapter for {@link RVector} objects.
 * 
 * @since 2.0 (provisional)
 */
public class RVectorDataAdapter extends AbstractRDataAdapter<RVector<?>, RVector<?>> {
	
	
	@Override
	public RVector<?> validate(final RObject rObject) throws UnexpectedRDataException {
		return RDataUtil.checkRVector(rObject);
	}
	
	@Override
	public RVector<?> validate(final RObject rObject, final RVector<?> referenceObject,
			final int flags) throws UnexpectedRDataException {
		final RVector<?> vector = RDataUtil.checkRVector(rObject);
		if ((flags & ROW_COUNT) != 0) {
			RDataUtil.checkLengthEqual(vector, referenceObject.getLength());
		}
		if ((flags & STORE_TYPE) != 0) {
			RDataUtil.checkData(vector.getData(), referenceObject.getData().getStoreType());
		}
		return vector;
	}
	
	@Override
	public long getRowCount(final RVector<?> rObject) {
		return rObject.getLength();
	}
	
	@Override
	public long getColumnCount(final RVector<?> rObject) {
		return 1;
	}
	
	
	@Override
	protected String getLoadDataFName() {
		return "rj:::.getDataVectorValues"; //$NON-NLS-1$
	}
	
	@Override
	protected RVector<?> validateData(final RObject rObject, final RVector<?> referenceObject,
			final Fragment<RVector<?>> fragment) throws UnexpectedRDataException {
		final RVector<?> vector = RDataUtil.checkRVector(rObject);
		RDataUtil.checkLengthEqual(vector, fragment.getRowCount());
		
		RDataUtil.checkData(rObject.getData(), referenceObject.getData().getStoreType());
		
		return vector;
	}
	
	@Override
	protected String getLoadRowNamesFName() {
		return "rj:::.getDataVectorRowNames"; //$NON-NLS-1$
	}
	
}
