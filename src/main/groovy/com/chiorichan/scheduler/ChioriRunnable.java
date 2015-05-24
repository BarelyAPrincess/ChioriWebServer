/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.scheduler;


/**
 * This class is provided as an easy way to handle scheduling tasks.
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public abstract class ChioriRunnable implements Runnable
{
	private int taskId = -1;
	
	/**
	 * Attempts to cancel this task.
	 * 
	 * @throws IllegalStateException
	 *             if task was not scheduled yet
	 */
	public synchronized void cancel() throws IllegalStateException
	{
		ScheduleManager.INSTANCE.cancelTask( getTaskId() );
	}
	
	/**
	 * Schedules this in the Chiori scheduler to run on next tick.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTask(TaskCreator, Runnable)
	 */
	public synchronized IChioriTask runTask( TaskCreator creator ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTask( creator, this ) );
	}
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules this in the Chiori scheduler to run asynchronously.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTaskAsynchronously(TaskCreator, Runnable)
	 */
	public synchronized IChioriTask runTaskAsynchronously( TaskCreator creator ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTaskAsynchronously( creator, this ) );
	}
	
	/**
	 * Schedules this to run after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @param delay
	 *            the ticks to wait before running the task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTaskLater(TaskCreator, Runnable, long)
	 */
	public synchronized IChioriTask runTaskLater( TaskCreator creator, long delay ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTaskLater( creator, this, delay ) );
	}
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules this to run asynchronously after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @param delay
	 *            the ticks to wait before running the task
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTaskLaterAsynchronously(TaskCreator, Runnable, long)
	 */
	public synchronized IChioriTask runTaskLaterAsynchronously( TaskCreator creator, long delay ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTaskLaterAsynchronously( creator, this, delay ) );
	}
	
	/**
	 * Schedules this to repeatedly run until cancelled, starting after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @param delay
	 *            the ticks to wait before running the task
	 * @param period
	 *            the ticks to wait between runs
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTaskTimer(TaskCreator, Runnable, long, long)
	 */
	public synchronized IChioriTask runTaskTimer( TaskCreator creator, long delay, long period ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTaskTimer( creator, this, delay, period ) );
	}
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Schedules this to repeatedly run asynchronously until cancelled, starting after the specified number of server
	 * ticks.
	 * 
	 * @param creator
	 *            the reference to the plugin scheduling task
	 * @param delay
	 *            the ticks to wait before running the task for the first time
	 * @param period
	 *            the ticks to wait between runs
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if plugin is null
	 * @throws IllegalStateException
	 *             if this was already scheduled
	 * @see IChioriScheduler#runTaskTimerAsynchronously(TaskCreator, Runnable, long, long)
	 */
	public synchronized IChioriTask runTaskTimerAsynchronously( TaskCreator creator, long delay, long period ) throws IllegalArgumentException, IllegalStateException
	{
		checkState();
		return setupId( ScheduleManager.INSTANCE.runTaskTimerAsynchronously( creator, this, delay, period ) );
	}
	
	/**
	 * Gets the task id for this runnable.
	 * 
	 * @return the task id that this runnable was scheduled as
	 * @throws IllegalStateException
	 *             if task was not scheduled yet
	 */
	public synchronized int getTaskId() throws IllegalStateException
	{
		final int id = taskId;
		if ( id == -1 )
		{
			throw new IllegalStateException( "Not scheduled yet" );
		}
		return id;
	}
	
	private void checkState()
	{
		if ( taskId != -1 )
		{
			throw new IllegalStateException( "Already scheduled as " + taskId );
		}
	}
	
	private IChioriTask setupId( final IChioriTask task )
	{
		this.taskId = task.getTaskId();
		return task;
	}
}
