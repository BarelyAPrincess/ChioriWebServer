/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.scheduler;


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
	int getTaskId();
	
	/**
	 * Returns the TaskCreator that owns this task.
	 * 
	 * @return The TaskCreator that owns the task
	 */
	TaskCreator getOwner();
	
	/**
	 * Returns true if the Task is a sync task.
	 * 
	 * @return true if the task is run by main thread
	 */
	boolean isSync();
	
	/**
	 * Will attempt to cancel this task.
	 */
	void cancel();
}
