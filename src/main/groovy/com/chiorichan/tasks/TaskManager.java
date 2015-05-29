/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
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
import java.util.concurrent.Executor;
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
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class TaskManager implements ITaskManager, ServerManager
{
	public static final TaskManager INSTANCE = new TaskManager();
	private static boolean isInitialized = false;
	
	/**
	 * Time delay constants for scheduling with the Manager<br>
	 * Just multiply the unit by the needed number and TADA!
	 */
	public static final long DELAY_SECOND = 50;
	public static final long DELAY_MINUTE = 3000;
	public static final long DELAY_HOUR = 180000;
	public static final long DELAY_DAY = 4320000;
	
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
	private final Executor executor = Executors.newCachedThreadPool();
	private AsyncTaskDebugger debugHead = new AsyncTaskDebugger( -1, null, null )
	{
		@Override
		StringBuilder debugTo( StringBuilder string )
		{
			return string;
		}
	};
	private AsyncTaskDebugger debugTail = debugHead;
	private static final int RECENT_TICKS;
	
	static
	{
		RECENT_TICKS = 20;
	}
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Schedule Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
		
	}
	
	private void init0()
	{
		
	}
	
	private TaskManager()
	{
		
	}
	
	public int scheduleSyncDelayedTask( final TaskCreator creator, final Runnable task )
	{
		return this.scheduleSyncDelayedTask( creator, task, 0L );
	}
	
	public Task runTask( TaskCreator creator, Runnable runnable )
	{
		return runTaskLater( creator, runnable, 0L );
	}
	
	public int scheduleAsyncDelayedTask( final TaskCreator creator, final Runnable task )
	{
		return this.scheduleAsyncDelayedTask( creator, task, 0L );
	}
	
	public Task runTaskAsynchronously( TaskCreator creator, Runnable runnable )
	{
		return runTaskLaterAsynchronously( creator, runnable, 0L );
	}
	
	public int scheduleSyncDelayedTask( final TaskCreator creator, final Runnable task, final long delay )
	{
		return this.scheduleSyncRepeatingTask( creator, task, delay, -1L );
	}
	
	public Task runTaskLater( TaskCreator creator, Runnable runnable, long delay )
	{
		return runTaskTimer( creator, runnable, delay, -1L );
	}
	
	public int scheduleAsyncDelayedTask( final TaskCreator creator, final Runnable task, final long delay )
	{
		return this.scheduleAsyncRepeatingTask( creator, task, delay, -1L );
	}
	
	public Task runTaskLaterAsynchronously( TaskCreator creator, Runnable runnable, long delay )
	{
		return runTaskTimerAsynchronously( creator, runnable, delay, -1L );
	}
	
	public int scheduleSyncRepeatingTask( final TaskCreator creator, final Runnable runnable, long delay, long period )
	{
		return runTaskTimer( creator, runnable, delay, period ).getTaskId();
	}
	
	public Task runTaskTimer( TaskCreator creator, Runnable runnable, long delay, long period )
	{
		validate( creator, runnable );
		
		if ( delay < 0L )
		{
			delay = 0;
		}
		if ( period == 0L )
		{
			period = 1L;
		}
		else if ( period < -1L )
		{
			period = -1L;
		}
		
		Task task = new Task( creator, runnable, nextId(), period );
		
		if ( creator.isEnabled() )
			return handle( task, delay );
		else
			return backlog( task, delay );
	}
	
	/**
	 * Deprecated for a new preferred argument order
	 * See {@link #scheduleAsyncRepeatingTask(TaskCreator, long, long, Runnable)}
	 */
	@Deprecated
	public int scheduleAsyncRepeatingTask( final TaskCreator creator, final Runnable runnable, long delay, long period )
	{
		return this.scheduleAsyncRepeatingTask( creator, runnable, delay, period );
	}
	
	/**
	 * Schedules a repeating runnable task
	 * 
	 * @param creator
	 *            The {@link TaskCreator} responsible
	 * @param delay
	 *            The required delay before first execute
	 * @param period
	 *            The required delay in between executes
	 * @param runnable
	 *            The runnable to execute on each delayed tick
	 * @return Integer referencing the scheduled task, calling {@link #cancelTask(int)} will cancel it
	 */
	public int scheduleAsyncRepeatingTask( final TaskCreator creator, long delay, long period, final Runnable runnable )
	{
		return runTaskTimerAsynchronously( creator, runnable, delay, period ).getTaskId();
	}
	
	public Task runTaskTimerAsynchronously( TaskCreator creator, Runnable runnable, long delay, long period )
	{
		validate( creator, runnable );
		
		if ( delay < 0L )
		{
			delay = 0;
		}
		if ( period == 0L )
		{
			period = 1L;
		}
		else if ( period < -1L )
		{
			period = -1L;
		}
		
		Task task = new AsyncTask( runners, creator, runnable, nextId(), period );
		
		if ( !creator.isEnabled() )
			return handle( task, delay );
		else
			return backlog( task, delay );
	}
	
	public <T> Future<T> callSyncMethod( final TaskCreator creator, final Callable<T> task )
	{
		validate( creator, task );
		if ( !creator.isEnabled() )
			throw new IllegalTaskCreatorAccessException( "TaskCreator attempted to register task while disabled" );
		
		final FutureTask<T> future = new FutureTask<T>( task, creator, nextId() );
		handle( future, 0L );
		return future;
	}
	
	public void cancelTask( final int taskId )
	{
		if ( taskId <= 0 )
		{
			return;
		}
		Task task = runners.get( taskId );
		if ( task != null )
		{
			task.cancel0();
		}
		task = new Task( new Runnable()
		{
			public void run()
			{
				if ( !check( TaskManager.this.temp ) )
				{
					check( TaskManager.this.pending );
				}
			}
			
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
						{
							runners.remove( taskId );
						}
						return true;
					}
				}
				return false;
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
			{
				return;
			}
			if ( taskPending.getTaskId() == taskId )
			{
				taskPending.cancel0();
			}
		}
	}
	
	public void cancelTasks( final TaskCreator creator )
	{
		Validate.notNull( creator, "Cannot cancel tasks of null creator" );
		final Task task = new Task( new Runnable()
		{
			public void run()
			{
				check( TaskManager.this.pending );
				check( TaskManager.this.temp );
			}
			
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
						{
							runners.remove( task.getTaskId() );
						}
					}
				}
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
			{
				return;
			}
			if ( taskPending.getTaskId() != -1 && taskPending.getOwner().equals( creator ) )
			{
				taskPending.cancel0();
			}
		}
		for ( Task runner : runners.values() )
		{
			if ( runner.getOwner().equals( creator ) )
			{
				runner.cancel0();
			}
		}
	}
	
	public void cancelAllTasks()
	{
		final Task task = new Task( new Runnable()
		{
			public void run()
			{
				Iterator<Task> it = TaskManager.this.runners.values().iterator();
				while ( it.hasNext() )
				{
					Task task = it.next();
					task.cancel0();
					if ( task.isSync() )
					{
						it.remove();
					}
				}
				TaskManager.this.pending.clear();
				TaskManager.this.temp.clear();
			}
		} );
		handle( task, 0L );
		for ( Task taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext() )
		{
			if ( taskPending == task )
			{
				break;
			}
			taskPending.cancel0();
		}
		for ( Task runner : runners.values() )
		{
			runner.cancel0();
		}
	}
	
	public boolean isCurrentlyRunning( final int taskId )
	{
		final Task task = runners.get( taskId );
		if ( task == null || task.isSync() )
		{
			return false;
		}
		final AsyncTask asyncTask = ( AsyncTask ) task;
		synchronized ( asyncTask.getWorkers() )
		{
			return asyncTask.getWorkers().isEmpty();
		}
	}
	
	public boolean isQueued( final int taskId )
	{
		if ( taskId <= 0 )
		{
			return false;
		}
		for ( Task task = head.getNext(); task != null; task = task.getNext() )
		{
			if ( task.getTaskId() == taskId )
			{
				return task.getPeriod() >= -1L; // The task will run
			}
		}
		Task task = runners.get( taskId );
		return task != null && task.getPeriod() >= -1L;
	}
	
	public List<Worker> getActiveWorkers()
	{
		final ArrayList<Worker> workers = new ArrayList<Worker>();
		for ( final Task taskObj : runners.values() )
		{
			// Iterator will be a best-effort (may fail to grab very new values) if called from an async thread
			if ( taskObj.isSync() )
			{
				continue;
			}
			final AsyncTask task = ( AsyncTask ) taskObj;
			synchronized ( task.getWorkers() )
			{
				// This will never have an issue with stale threads; it's state-safe
				workers.addAll( task.getWorkers() );
			}
		}
		return workers;
	}
	
	public List<Task> getPendingTasks()
	{
		final ArrayList<Task> truePending = new ArrayList<Task>();
		for ( Task task = head.getNext(); task != null; task = task.getNext() )
		{
			if ( task.getTaskId() != -1 )
			{
				// -1 is special code
				truePending.add( task );
			}
		}
		
		final ArrayList<Task> pending = new ArrayList<Task>();
		for ( Task task : runners.values() )
		{
			if ( task.getPeriod() >= -1L )
			{
				pending.add( task );
			}
		}
		
		for ( final Task task : truePending )
		{
			if ( task.getPeriod() >= -1L && !pending.contains( task ) )
			{
				pending.add( task );
			}
		}
		return pending;
	}
	
	/**
	 * This method is designed to never block or wait for locks; an immediate execution of all current tasks.
	 */
	public void heartbeat( final int currentTick )
	{
		if ( Thread.currentThread() != Loader.getConsole().primaryThread )
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
				{
					runners.remove( task.getTaskId(), task );
				}
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
			{
				runners.remove( task.getTaskId() );
			}
		}
		
		// Scans the backlog map for unscheduled tasks awaiting for their owner to become enabled
		if ( !backlogTasks.isEmpty() )
			for ( Entry<Long, Task> e : backlogTasks.entrySet() )
			{
				if ( e.getValue() == null || e.getValue().getOwner() == null )
				{
					backlogTasks.remove( e.getKey() );
				}
				else if ( e.getValue().getOwner().isEnabled() )
				{
					handle( e.getValue(), e.getKey() );
					backlogTasks.remove( e.getKey() );
				}
			}
		
		pending.addAll( temp );
		temp.clear();
		debugHead = debugHead.getNextHead( currentTick );
	}
	
	private void addTask( final Task task )
	{
		final AtomicReference<Task> tail = this.tail;
		Task tailTask = tail.get();
		while ( !tail.compareAndSet( tailTask, task ) )
		{
			tailTask = tail.get();
		}
		tailTask.setNext( task );
	}
	
	private Task backlog( Task task, long delay )
	{
		backlogTasks.put( delay, task );
		return task;
	}
	
	private Task handle( final Task task, final long delay )
	{
		task.setNextRun( currentTick + delay );
		addTask( task );
		return task;
	}
	
	/**
	 * Checks in the provided creator and task are valid
	 * 
	 * @param creator
	 *            The object owning this task
	 * @param task
	 *            The task to validate
	 */
	private static void validate( final TaskCreator creator, final Object task )
	{
		Validate.notNull( creator, "TaskCreator cannot be null" );
		Validate.notNull( task, "Task cannot be null" );
		
		if ( !creator.isEnabled() )
		{
			// Task Creator can now register while disabled but will not be called until enabled.
			// throw new IllegalTaskCreatorAccessException( "TaskCreator attempted to register task while disabled" );
		}
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
		{
			if ( task.getTaskId() == -1 )
			{
				task.run();
			}
			else if ( task.getPeriod() >= -1L )
			{
				pending.add( task );
				runners.put( task.getTaskId(), task );
			}
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
	
	private boolean isReady( final int currentTick )
	{
		return !pending.isEmpty() && pending.peek().getNextRun() <= currentTick;
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
