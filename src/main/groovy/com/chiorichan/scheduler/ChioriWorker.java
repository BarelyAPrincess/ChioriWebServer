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
 * Represents a worker thread for the scheduler. This gives information about the Thread object for the task, owner of
 * the task and the taskId. </p> Workers are used to execute async tasks.
 */

public interface ChioriWorker
{
	
	/**
	 * Returns the taskId for the task being executed by this worker.
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
	 * Returns the thread for the worker.
	 * 
	 * @return The Thread object for the worker
	 */
	public Thread getThread();
	
}
