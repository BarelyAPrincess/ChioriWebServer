package com.chiorichan.scheduler;


public interface TaskCreator
{
	/**
	 * Returns a value indicating whether or not this creator is currently enabled
	 * 
	 * @return true if this creator is enabled, otherwise false
	 */
	public boolean isEnabled();
	
	/**
	 * Returns the name of the creator.
	 * <p>
	 * This should return the bare name of the creator and should be used for comparison.
	 * 
	 * @return name of the creator
	 */
	public String getName();
}
