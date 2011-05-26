package org.rosuda.JRI;


/**
 * Advanced configuration parameters for the Rengine
 */
public class RConfig {
	
	
	/**
	 * Size of stack of R main thread, Java dependent.
	 * 
	 * @see Thread#Thread(ThreadGroup, Runnable, String, long)
	 * <code>0</code> = java default
	 */
	public long MainCStack_Size = 0;
	
	/**
	 * Flag if stack limit should be set in R.
	 * 
	 * Requires {@link #MainCStack_Size}
	 */
	public boolean MainCStack_SetLimit = true;
	
	
}
