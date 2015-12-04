/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event;

import com.chiorichan.SourceContext;

public class RegisteredListener
{
	private final Listener listener;
	private final EventPriority priority;
	private final SourceContext context;
	private final EventExecutor executor;
	private final boolean ignoreCancelled;
	
	public RegisteredListener( final Listener listener, final EventExecutor executor, final EventPriority priority, final SourceContext context, final boolean ignoreCancelled )
	{
		this.listener = listener;
		this.priority = priority;
		this.context = context;
		this.executor = executor;
		this.ignoreCancelled = ignoreCancelled;
	}
	
	/**
	 * Calls the event executor
	 * 
	 * @param event
	 *            The event
	 * @throws EventException
	 *             If an event handler throws an exception.
	 */
	public void callEvent( final AbstractEvent event ) throws EventException
	{
		if ( event instanceof Cancellable )
			if ( ( ( Cancellable ) event ).isCancelled() && isIgnoringCancelled() )
				return;
		
		if ( event instanceof Conditional )
			if ( priority != EventPriority.MONITOR && ! ( ( Conditional ) event ).conditional( this ) )
				return;
		
		executor.execute( listener, event );
	}
	
	/**
	 * Gets the plugin for this registration
	 * 
	 * @return Registered Plugin
	 */
	public SourceContext getContext()
	{
		return context;
	}
	
	/**
	 * Gets the listener for this registration
	 * 
	 * @return Registered Listener
	 */
	public Listener getListener()
	{
		return listener;
	}
	
	/**
	 * Gets the priority for this registration
	 * 
	 * @return Registered Priority
	 */
	public EventPriority getPriority()
	{
		return priority;
	}
	
	/**
	 * Whether this listener accepts cancelled events
	 * 
	 * @return True when ignoring cancelled events
	 */
	public boolean isIgnoringCancelled()
	{
		return ignoreCancelled;
	}
}
