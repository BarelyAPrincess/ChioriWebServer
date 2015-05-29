/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.tasks;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

interface ITaskManager
{
	/**
	 * Schedules a once off task to occur after a delay. This task will be executed by the main server thread.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @param delay
	 *            Delay in server ticks before executing task
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleSyncDelayedTask( TaskCreator creator, Runnable task, long delay );
	
	/**
	 * Schedules a once off task to occur as soon as possible. This task will be executed by the main server thread.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleSyncDelayedTask( TaskCreator creator, Runnable task );
	
	/**
	 * Schedules a repeating task. This task will be executed by the main server thread.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @param delay
	 *            Delay in server ticks before executing first repeat
	 * @param period
	 *            Period in server ticks of the task
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleSyncRepeatingTask( TaskCreator creator, Runnable task, long delay, long period );
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules a once off task to occur after a delay. This task will be executed by a thread managed by the scheduler.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @param delay
	 *            Delay in server ticks before executing task
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleAsyncDelayedTask( TaskCreator creator, Runnable task, long delay );
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules a once off task to occur as soon as possible. This task will be executed by a thread managed by the
	 * scheduler.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleAsyncDelayedTask( TaskCreator creator, Runnable task );
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules a repeating task. This task will be executed by a thread managed by the scheduler.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @param delay
	 *            Delay in server ticks before executing first repeat
	 * @param period
	 *            Period in server ticks of the task
	 * @return Task id number (-1 if scheduling failed)
	 */
	int scheduleAsyncRepeatingTask( TaskCreator creator, Runnable task, long delay, long period );
	
	/**
	 * Calls a method on the main thread and returns a Future object This task will be executed by the main server
	 * thread.
	 * <p>
	 * Note: The Future.get() methods must NOT be called from the main thread. Note2: There is at least an average of 10ms latency until the isDone() method returns true.
	 * 
	 * @param <T>
	 *            The callable's return type
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @return Future Future object related to the task
	 */
	<T> Future<T> callSyncMethod( TaskCreator creator, Callable<T> task );
	
	/**
	 * Removes task from scheduler.
	 * 
	 * @param taskId
	 *            Id number of task to be removed
	 */
	void cancelTask( int taskId );
	
	/**
	 * Removes all tasks associated with a particular creator from the scheduler.
	 * 
	 * @param creator
	 *            Owner of tasks to be removed
	 */
	void cancelTasks( TaskCreator creator );
	
	/**
	 * Removes all tasks from the scheduler.
	 */
	void cancelAllTasks();
	
	/**
	 * Check if the task currently running.
	 * <p>
	 * A repeating task might not be running currently, but will be running in the future. A task that has finished, and does not repeat, will not be running ever again.
	 * <p>
	 * Explicitly, a task is running if there exists a thread for it, and that thread is alive.
	 * 
	 * @param taskId
	 *            The task to check.
	 *            <p>
	 * @return If the task is currently running.
	 */
	boolean isCurrentlyRunning( int taskId );
	
	/**
	 * Check if the task queued to be run later.
	 * <p>
	 * If a repeating task is currently running, it might not be queued now but could be in the future. A task that is not queued, and not running, will not be queued again.
	 * 
	 * @param taskId
	 *            The task to check.
	 *            <p>
	 * @return If the task is queued to be run.
	 */
	boolean isQueued( int taskId );
	
	/**
	 * Returns a list of all active workers.
	 * <p>
	 * This list contains asynch tasks that are being executed by separate threads.
	 * 
	 * @return Active workers
	 */
	List<Worker> getActiveWorkers();
	
	/**
	 * Returns a list of all pending tasks. The ordering of the tasks is not related to their order of execution.
	 * 
	 * @return Active workers
	 */
	List<Task> getPendingTasks();
	
	/**
	 * Returns a task that will run on the next server tick.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTask( TaskCreator creator, Runnable task ) throws IllegalArgumentException;
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Returns a task that will run asynchronously.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTaskAsynchronously( TaskCreator creator, Runnable task ) throws IllegalArgumentException;
	
	/**
	 * Returns a task that will run after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @param delay
	 *            the ticks to wait before running the task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTaskLater( TaskCreator creator, Runnable task, long delay ) throws IllegalArgumentException;
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Returns a task that will run asynchronously after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @param delay
	 *            the ticks to wait before running the task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTaskLaterAsynchronously( TaskCreator creator, Runnable task, long delay ) throws IllegalArgumentException;
	
	/**
	 * Returns a task that will repeatedly run until cancelled, starting after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @param delay
	 *            the ticks to wait before running the task
	 * @param period
	 *            the ticks to wait between runs
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTaskTimer( TaskCreator creator, Runnable task, long delay, long period ) throws IllegalArgumentException;
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Returns a task that will repeatedly run asynchronously until cancelled, starting after the specified number of
	 * server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param task
	 *            the task to be run
	 * @param delay
	 *            the ticks to wait before running the task for the first time
	 * @param period
	 *            the ticks to wait between runs
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	ITask runTaskTimerAsynchronously( TaskCreator creator, Runnable task, long delay, long period ) throws IllegalArgumentException;
}