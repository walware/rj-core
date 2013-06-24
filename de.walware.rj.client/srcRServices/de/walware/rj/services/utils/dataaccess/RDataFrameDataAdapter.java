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

import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RDataUtil;
import de.walware.rj.data.RObject;
import de.walware.rj.data.UnexpectedRDataException;
import de.walware.rj.services.utils.dataaccess.LazyRStore.Fragment;


/**
 * Data adapter for {@link RDataFrame} objects.
 * 
 * @since 2.0 (provisional)
 */
public class RDataFrameDataAdapter extends AbstractRDataAdapter<RDataFrame, RDataFrame> {
	
	
	@Override
	public RDataFrame validate(final RObject rObject) throws UnexpectedRDataException {
		return RDataUtil.checkRDataFrame(rObject);
	}
	
	@Override
	public RDataFrame validate(final RObject rObject, final RDataFrame referenceObject,
			final int flags) throws UnexpectedRDataException {
		final RDataFrame dataframe = RDataUtil.checkRDataFrame(rObject, referenceObject.getColumnCount());
		if ((flags & ROW_COUNT) != 0) {
			RDataUtil.checkRowCountEqual(dataframe, referenceObject.getRowCount());
		}
		if ((flags & STORE_TYPE) != 0) {
			for (int i = 0; i < dataframe.getColumnCount(); i++) {
				RDataUtil.checkData(dataframe.getColumn(i), referenceObject.getColumn(i).getStoreType());
			}
		}
		return dataframe;
	}
	
	@Override
	public long getRowCount(final RDataFrame rObject) {
		return rObject.getRowCount();
	}
	
	@Override
	public long getColumnCount(final RDataFrame rObject) {
		return rObject.getColumnCount();
	}
	
	
	@Override
	protected String getLoadDataFName() {
		return "rj:::.getDataFrameValues"; //$NON-NLS-1$
	}
	
	@Override
	protected RDataFrame validateData(final RObject rObject, final RDataFrame referenceObject,
			final Fragment<RDataFrame> fragment) throws UnexpectedRDataException {
		final RDataFrame dataframe = RDataUtil.checkRDataFrame(rObject, fragment.getColumnCount());
		RDataUtil.checkRowCountEqual(dataframe, fragment.getRowCount());
		
		for (int i = 0; i < fragment.getColumnCount(); i++) {
			RDataUtil.checkData(dataframe.getColumn(i),
					referenceObject.getColumn(fragment.getColumnBeginIdx() + i).getStoreType() );
		}
		
		return dataframe;
	}
	
	@Override
	protected String getLoadRowNamesFName() {
		return "rj:::.getDataFrameRowNames"; //$NON-NLS-1$
	}
	
}
