package com.chiorichan.plugin;

import com.chiorichan.bus.bases.EventException;
import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.Listener;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor
{
	public void execute( Listener listener, Event event ) throws EventException;
}
