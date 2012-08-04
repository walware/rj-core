package de.walware.rj.eclient;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.walware.rj.services.RService;

import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ts.IToolRunnable;
import de.walware.ecommons.ts.IToolService;


/**
 * Abstract runnable for R tool.
 * 
 * Sub class should implement at least the method {@link #run(IRToolService, IProgressMonitor)},
 * which is called by the R tool and gives access to the {@link IRToolService} API.
 */
public abstract class AbstractRServiceRunnable implements IToolRunnable {
	
	
	private final String fTypeId;
	private final String fLabel;
	
	
	public AbstractRServiceRunnable(final String typeId, final String label) {
		fTypeId = typeId;
		fLabel = label;
	}
	
	
	public String getTypeId() {
		return fTypeId;
	}
	
	public String getLabel() {
		return fLabel;
	}
	
	public boolean isRunnableIn(final ITool tool) {
		return tool.isProvidingFeatureSet("de.walware.rj.services.RService"); //$NON-NLS-1$
	}
	
	public boolean changed(final int event, final ITool tool) {
		return true;
	}
	
	public void run(final IToolService service,
			final IProgressMonitor monitor) throws CoreException {
		run((IRToolService) service, monitor);
	}
	
	/**
	 * 
	 * 
	 * @since 1.2
	 */
	protected void run(final IRToolService r,
			IProgressMonitor monitor) throws CoreException {
		run((RService) r, monitor);
	}
	
	@Deprecated
	protected void run(RService r,
			IProgressMonitor monitor) throws CoreException {
	}
	
}
