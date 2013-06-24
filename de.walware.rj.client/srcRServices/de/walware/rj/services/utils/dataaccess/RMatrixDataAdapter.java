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

import de.walware.rj.data.RArray;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RObject;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.services.utils.dataaccess.LazyRStore.Fragment;


/**
 * Data adapter for two-dimensional {@link RArray} objects.
 * 
 * @since 2.0 (provisional)
 */
public class RMatrixDataAdapter extends AbstractRDataAdapter<RArray<?>, RArray<?>> {
	
	
	@Override
	public RArray<?> validate(final RObject rObject) throws UnexpectedRDataException {
		return RDataUtil.checkRArray(rObject, 2);
	}
	
	@Override
	public RArray<?> validate(final RObject rObject, final RArray<?> referenceObject,
			final int flags) throws UnexpectedRDataException {
		final RArray<?> array = RDataUtil.checkRArray(rObject, 2);
		RDataUtil.checkColumnCountEqual(array, RDataUtil.getColumnCount(referenceObject));
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
		return rObject.getDim().getInt(0);
	}
	
	@Override
	public long getColumnCount(final RArray<?> rObject) {
		return rObject.getDim().getInt(1);
	}
	
	
	@Override
	protected String getLoadDataFName() {
		return "rj:::.getDataMatrixValues"; //$NON-NLS-1$
	}
	
	@Override
	protected RArray<?> validateData(final RObject rObject, final RArray<?> referenceObject,
			final Fragment<RArray<?>> fragment)
			throws UnexpectedRDataException {
		final RArray<?> array = RDataUtil.checkRArray(rObject, 2);
		RDataUtil.checkColumnCountEqual(array, fragment.getColumnCount());
		RDataUtil.checkRowCountEqual(array, fragment.getRowCount());
		
		RDataUtil.checkData(rObject.getData(), referenceObject.getData().getStoreType());
		
		return array;
	}
	
	@Override
	protected String getLoadRowNamesFName() {
		return "rj:::.getDataMatrixRowNames"; //$NON-NLS-1$
	}
	
}
