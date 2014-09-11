/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.scheduler;

import com.chiorichan.plugin.Plugin;

/**
 * Represents a task being executed by the scheduler
 */

public interface IChioriTask
{
	
	/**
	 * Returns the taskId for the task.
	 * 
	 * @return Task id number
	 */
	public int getTaskId();
	
	/**
	 * Returns the Plugin that owns this task.
	 * 
	 * @return The Plugin that owns the task
	 */
	public Plugin getOwner();
	
	/**
	 * Returns true if the Task is a sync task.
	 * 
	 * @return true if the task is run by main thread
	 */
	public boolean isSync();
	
	/**
	 * Will attempt to cancel this task.
	 */
	public void cancel();
}
