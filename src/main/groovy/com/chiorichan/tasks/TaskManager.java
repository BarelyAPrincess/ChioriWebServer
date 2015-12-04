/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.google.common.collect.Maps;

/**
 * Manages task scheduled in the main thread
 */
public class TaskManager implements ServerManager
{
	public static final TaskManager INSTANCE = new TaskManager();
	private static boolean isInitialized = false;
	
	private static final int RECENT_TICKS;
	static
	{
		RECENT_TICKS = 20;
	}
	/**
	 * Counter for IDs. Order doesn't matter, only uniqueness.
	 */
	private final AtomicInteger ids = new AtomicInteger( 1 );
	/**
	 * Current head of linked-list. This reference is always stale, {@link Task#next} is the live reference.
	 */
	private volatile Task head = new Task();
	/**
	 * Holds tasks that are awaiting for there owners to be enabled
	 */
	private final Map<Long, Task> backlogTasks = Maps.newConcurrentMap();
	/**
	 * Tail of a linked-list. AtomicReference only matters when adding to queue
	 */
	private final AtomicReference<Task> tail = new AtomicReference<Task>( head );
	/**
	 * Main thread logic only
	 */
	private final PriorityQueue<Task> pending = new PriorityQueue<Task>( 10, new Comparator<Task>()
	{
		@Override
		public int compare( final Task o1, final Task o2 )
		{
			return ( int ) ( o1.getNextRun() - o2.getNextRun() );
		}
	} );
	/**
	 * Main thread logic only
	 */
	private final List<Task> temp = new ArrayList<Task>();
	/**
	 * These are tasks that are currently active. It's provided for 'viewing' the current state.
	 */
	private final ConcurrentHashMap<Integer, Task> runners = new ConcurrentHashMap<Integer, Task>();
	private volatile int currentTick = -1;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private AsyncTaskDebugger debugHead = new AsyncTaskDebugger( -1, null, null )
	{
		@Override
		StringBuilder debugTo( StringBuilder string )
		{
			return string;
		}
	};
	
	private AsyncTaskDebugger debugTail = debugHead;
	
	private TaskManager()
	{
		
	}
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Schedule Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
		
	}
	
	/**
	 * Checks in the provided creator and task are valid
	 * 
	 * @param creator
	 *            The object owning this task
	 * @param task
	 *            The task to validate
	 */
	private static void validate( final TaskRegistrar creator, final Object task )
	{
		Validate.notNull( creator, "TaskCreator cannot be null" );
		Validate.notNull( task, "Task cannot be null" );
		
		if ( !creator.isEnabled() )
		{
			// Task Creator can now register while disabled but will not be called until enabled.
			// throw new IllegalTaskCreatorAccessException( "TaskCreator attempted to register task while disabled" );
		}
	}
	
	private void addTask( final Task task )
	{
		final AtomicReference<Task> tail = this.tail;
		Task tailTask = tail.get();
		while ( !tail.compareAndSet( tailTask, task ) )
			tailTask = tail.get();
		tailTask.setNext( task );
	}
	
	private Task backlog( Task task, long delay )
	{
		backlogTasks.put( delay, task );
		return task;
	}
	
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
	public <T> Future<T> callSyncMethod( final TaskRegistrar creator, final Callable<T> task )
	{
		validate( creator, task );
		if ( !creator.isEnabled() )
			throw new IllegalTaskCreatorAccessException( "TaskCreator attempted to register task while disabled" );
		
		final FutureTask<T> future = new FutureTask<T>( task, creator, nextId() );
		handle( future, 0L );
		return future;
	}
	
	/**
	 * Removes all tasks from the manager.
	 */
	public void cancelAllTasks()
	{
		final Task task = new Task( new Runnable()
		{
			@Override
			public void run()
			{
				Iterator<Task> it = runners.values().iterator();
				while ( it.hasNext() )
				{
					Task task = it.next();
					task.cancel0();
					if ( task.isSync() )
						it.remove();
				}
				pending.clear();
				temp.clear();
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
				break;
			taskPending.cancel0();
		}
		for ( Task runner : runners.values() )
			runner.cancel0();
	}
	
	/**
	 * Removes task from scheduler.
	 * 
	 * @param taskId
	 *            Id number of task to be removed
	 */
	public void cancelTask( final int taskId )
	{
		if ( taskId <= 0 )
			return;
		Task task = runners.get( taskId );
		if ( task != null )
			task.cancel0();
		task = new Task( new Runnable()
		{
			private boolean check( final Iterable<Task> collection )
			{
				final Iterator<Task> tasks = collection.iterator();
				while ( tasks.hasNext() )
				{
					final Task task = tasks.next();
					if ( task.getTaskId() == taskId )
					{
						task.cancel0();
						tasks.remove();
						if ( task.isSync() )
							runners.remove( taskId );
						return true;
					}
				}
				return false;
			}
			
			@Override
			public void run()
			{
				if ( !check( temp ) )
					check( pending );
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
				return;
			if ( taskPending.getTaskId() == taskId )
				taskPending.cancel0();
		}
	}
	
	/**
	 * Removes all tasks associated with a particular creator from the scheduler.
	 * 
	 * @param creator
	 *            Owner of tasks to be removed
	 */
	public void cancelTasks( final TaskRegistrar creator )
	{
		Validate.notNull( creator, "Cannot cancel tasks of null creator" );
		final Task task = new Task( new Runnable()
		{
			void check( final Iterable<Task> collection )
			{
				final Iterator<Task> tasks = collection.iterator();
				while ( tasks.hasNext() )
				{
					final Task task = tasks.next();
					if ( task.getOwner().equals( creator ) )
					{
						task.cancel0();
						tasks.remove();
						if ( task.isSync() )
							runners.remove( task.getTaskId() );
					}
				}
			}
			
			@Override
			public void run()
			{
				check( pending );
				check( temp );
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
				return;
			if ( taskPending.getTaskId() != -1 && taskPending.getOwner().equals( creator ) )
				taskPending.cancel0();
		}
		for ( Task runner : runners.values() )
			if ( runner.getOwner().equals( creator ) )
				runner.cancel0();
	}
	
	/**
	 * Returns a list of all active workers.
	 * <p>
	 * This list contains asynch tasks that are being executed by separate threads.
	 * 
	 * @return Active workers
	 */
	public List<Worker> getActiveWorkers()
	{
		final ArrayList<Worker> workers = new ArrayList<Worker>();
		for ( final Task taskObj : runners.values() )
		{
			// Iterator will be a best-effort (may fail to grab very new values) if called from an async thread
			if ( taskObj.isSync() )
				continue;
			final AsyncTask task = ( AsyncTask ) taskObj;
			synchronized ( task.getWorkers() )
			{
				// This will never have an issue with stale threads; it's state-safe
				workers.addAll( task.getWorkers() );
			}
		}
		return workers;
	}
	
	/**
	 * Returns a list of all pending tasks. The ordering of the tasks is not related to their order of execution.
	 * 
	 * @return Active workers
	 */
	public List<Task> getPendingTasks()
	{
		final ArrayList<Task> truePending = new ArrayList<Task>();
		for ( Task task = head.getNext(); task != null; task = task.getNext() )
			if ( task.getTaskId() != -1 )
				// -1 is special code
				truePending.add( task );
		
		final ArrayList<Task> pending = new ArrayList<Task>();
		for ( Task task : runners.values() )
			if ( task.getPeriod() >= -1L )
				pending.add( task );
		
		for ( final Task task : truePending )
			if ( task.getPeriod() >= -1L && !pending.contains( task ) )
				pending.add( task );
		return pending;
	}
	
	private Task handle( final Task task, final long delay )
	{
		task.setNextRun( currentTick + delay );
		addTask( task );
		return task;
	}
	
	/**
	 * This method is designed to never block or wait for locks; an immediate execution of all current tasks.
	 */
	public void heartbeat( final int currentTick )
	{
		if ( Thread.currentThread() != Loader.getServerBus().primaryThread )
			throw new IllegalStateException( "We detected that the heartbeat method was called on a thread other than the ConsoleBus thread. This is a really bad thing and could cause concurrency issues if left unchecked." );
		
		this.currentTick = currentTick;
		final List<Task> temp = this.temp;
		parsePending();
		while ( isReady( currentTick ) )
		{
			final Task task = pending.remove();
			if ( task.getPeriod() < -1L )
			{
				if ( task.isSync() )
					runners.remove( task.getTaskId(), task );
				parsePending();
				continue;
			}
			if ( task.isSync() )
			{
				try
				{
					task.run();
				}
				catch ( final Throwable throwable )
				{
					Loader.getLogger().log( Level.WARNING, String.format( "Task #%s for %s generated an exception", task.getTaskId(), task.getOwner().getName() ), throwable );
				}
				parsePending();
			}
			else
			{
				debugTail = debugTail.setNext( new AsyncTaskDebugger( currentTick + RECENT_TICKS, task.getOwner(), task.getTaskClass() ) );
				executor.execute( task );
				// We don't need to parse pending
				// (async tasks must live with race-conditions if they attempt to cancel between these few lines of code)
			}
			final long period = task.getPeriod(); // State consistency
			if ( period > 0 )
			{
				task.setNextRun( currentTick + period );
				temp.add( task );
			}
			else if ( task.isSync() )
				runners.remove( task.getTaskId() );
		}
		
		// Scans the backlog map for unscheduled tasks awaiting for their owner to become enabled
		if ( !backlogTasks.isEmpty() )
			for ( Entry<Long, Task> e : backlogTasks.entrySet() )
				if ( e.getValue() == null || e.getValue().getOwner() == null )
					backlogTasks.remove( e.getKey() );
				else if ( e.getValue().getOwner().isEnabled() )
				{
					handle( e.getValue(), e.getKey() );
					backlogTasks.remove( e.getKey() );
				}
		
		pending.addAll( temp );
		temp.clear();
		debugHead = debugHead.getNextHead( currentTick );
	}
	
	private void init0()
	{
		
	}
	
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
	public boolean isCurrentlyRunning( final int taskId )
	{
		final Task task = runners.get( taskId );
		if ( task == null || task.isSync() )
			return false;
		final AsyncTask asyncTask = ( AsyncTask ) task;
		synchronized ( asyncTask.getWorkers() )
		{
			return asyncTask.getWorkers().isEmpty();
		}
	}
	
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
	public boolean isQueued( final int taskId )
	{
		if ( taskId <= 0 )
			return false;
		for ( Task task = head.getNext(); task != null; task = task.getNext() )
			if ( task.getTaskId() == taskId )
				return task.getPeriod() >= -1L; // The task will run
		Task task = runners.get( taskId );
		return task != null && task.getPeriod() >= -1L;
	}
	
	private boolean isReady( final int currentTick )
	{
		return !pending.isEmpty() && pending.peek().getNextRun() <= currentTick;
	}
	
	private int nextId()
	{
		return ids.incrementAndGet();
	}
	
	private void parsePending()
	{
		Task head = this.head;
		Task task = head.getNext();
		Task lastTask = head;
		for ( ; task != null; task = ( lastTask = task ).getNext() )
			if ( task.getTaskId() == -1 )
				task.run();
			else if ( task.getPeriod() >= -1L )
			{
				pending.add( task );
				runners.put( task.getTaskId(), task );
			}
		// We split this because of the way things are ordered for all of the async calls in ChioriScheduler
		// (it prevents race-conditions)
		for ( task = head; task != lastTask; task = head )
		{
			head = task.getNext();
			task.setNext( null );
		}
		this.head = lastTask;
	}
	
	/**
	 * Returns a task that will run on the next server tick.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param runnable
	 *            the task to be run
	 * @return a {@link Task} that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	public Task runTask( TaskRegistrar creator, Runnable runnable )
	{
		return runTaskLater( creator, 0L, runnable );
	}
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Returns a task that will run asynchronously.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param runnable
	 *            the task to be run
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	public Task runTaskAsynchronously( TaskRegistrar creator, Runnable runnable )
	{
		return runTaskLaterAsynchronously( creator, 0L, runnable );
	}
	
	/**
	 * Returns a task that will run after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param delay
	 *            the ticks to wait before running the task
	 * @param runnable
	 *            the task to be run
	 * @return a {@link Task} that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	public Task runTaskLater( TaskRegistrar creator, long delay, Runnable runnable )
	{
		return runTaskTimer( creator, delay, -1L, runnable );
	}
	
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
	public Task runTaskLaterAsynchronously( TaskRegistrar creator, long delay, Runnable runnable )
	{
		return runTaskTimerAsynchronously( creator, delay, -1L, runnable );
	}
	
	/**
	 * Returns a task that will repeatedly run until cancelled, starting after the specified number of server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param delay
	 *            the ticks to wait before running the task
	 * @param period
	 *            the ticks to wait between runs
	 * @param task
	 *            the task to be run
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	public Task runTaskTimer( TaskRegistrar creator, long delay, long period, Runnable runnable )
	{
		validate( creator, runnable );
		
		if ( delay < 0L )
			delay = 0;
		if ( period == 0L )
			period = 1L;
		else if ( period < -1L )
			period = -1L;
		
		Task task = new Task( creator, runnable, nextId(), period );
		
		if ( creator.isEnabled() )
			return handle( task, delay );
		else
			return backlog( task, delay );
	}
	
	/**
	 * <b>Asynchronous tasks should never access any API in Main. Great care should be taken to assure the thread-safety
	 * of asynchronous tasks.</b> <br>
	 * <br>
	 * Returns a task that will repeatedly run asynchronously until cancelled, starting after the specified number of
	 * server ticks.
	 * 
	 * @param creator
	 *            the reference to the creator scheduling task
	 * @param delay
	 *            the ticks to wait before running the task for the first time
	 * @param period
	 *            the ticks to wait between runs
	 * @param task
	 *            the task to be run
	 * @return a ChioriTask that contains the id number
	 * @throws IllegalArgumentException
	 *             if creator is null
	 * @throws IllegalArgumentException
	 *             if task is null
	 */
	public Task runTaskTimerAsynchronously( TaskRegistrar creator, long delay, long period, Runnable runnable )
	{
		validate( creator, runnable );
		
		if ( delay < 0L )
			delay = 0;
		if ( period == 0L )
			period = 1L;
		else if ( period < -1L )
			period = -1L;
		
		Task task = new AsyncTask( runners, creator, runnable, nextId(), period );
		
		if ( !creator.isEnabled() )
			return handle( task, delay );
		else
			return backlog( task, delay );
	}
	
	public void runTaskWithTimeout( final TaskRegistrar creator, final long timeout, final Runnable runnable )
	{
		final Task task = runTaskAsynchronously( creator, runnable );
		runTaskLaterAsynchronously( creator, timeout, new Runnable()
		{
			@Override
			public void run()
			{
				task.cancel();
			}
		} );
		
		int cnt = 0;
		
		for ( ;; )
		{
			cnt++;
			
			if ( !isQueued( task.getTaskId() ) && !isCurrentlyRunning( task.getTaskId() ) )
				return;
			
			if ( cnt > timeout + Ticks.SECOND_15 )
				throw new IllegalStateException( "The task exceeded timeout of " + timeout + " and failed to terminate." );
			
			try
			{
				Thread.sleep( 50 );
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
	}
	
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
	public int scheduleAsyncDelayedTask( final TaskRegistrar creator, final long delay, final Runnable task )
	{
		return scheduleAsyncRepeatingTask( creator, delay, -1L, task );
	}
	
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
	public int scheduleAsyncDelayedTask( final TaskRegistrar creator, final Runnable task )
	{
		return this.scheduleAsyncDelayedTask( creator, 0L, task );
	}
	
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
	 * @return Task id number (-1 if scheduling failed), calling {@link #cancelTask(int)} will cancel it
	 */
	public int scheduleAsyncRepeatingTask( final TaskRegistrar creator, long delay, long period, final Runnable runnable )
	{
		return runTaskTimerAsynchronously( creator, delay, period, runnable ).getTaskId();
	}
	
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
	public int scheduleSyncDelayedTask( final TaskRegistrar creator, final long delay, final Runnable task )
	{
		return scheduleSyncRepeatingTask( creator, delay, -1L, task );
	}
	
	/**
	 * Schedules a once off task to occur as soon as possible. This task will be executed by the main server thread.
	 * 
	 * @param creator
	 *            TaskCreator that owns the task
	 * @param task
	 *            Task to be executed
	 * @return Task id number (-1 if scheduling failed)
	 */
	public int scheduleSyncDelayedTask( final TaskRegistrar creator, final Runnable task )
	{
		return this.scheduleSyncDelayedTask( creator, 0L, task );
	}
	
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
	public int scheduleSyncRepeatingTask( final TaskRegistrar creator, long delay, long period, final Runnable runnable )
	{
		return runTaskTimer( creator, delay, period, runnable ).getTaskId();
	}
	
	public void shutdown()
	{
		executor.shutdown();
	}
	
	@Override
	public String toString()
	{
		int debugTick = currentTick;
		StringBuilder string = new StringBuilder( "Recent tasks from " ).append( debugTick - RECENT_TICKS ).append( '-' ).append( debugTick ).append( '{' );
		debugHead.debugTo( string );
		return string.append( '}' ).toString();
	}
}
