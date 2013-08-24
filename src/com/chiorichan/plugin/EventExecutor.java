package com.chiorichan.plugin;

import com.chiorichan.event.Event;
import com.chiorichan.event.EventException;
import com.chiorichan.event.Listener;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor
{
	public void execute( Listener listener, Event event ) throws EventException;
}
