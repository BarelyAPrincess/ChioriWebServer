/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.logger;

import java.lang.ref.WeakReference;
import java.util.List;

import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventCreator;
import com.chiorichan.event.Listener;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.scheduler.ScheduleManager;
import com.chiorichan.scheduler.TaskCreator;
import com.google.common.collect.Lists;

/**
 * Provides the direct log output for the server.
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class Out implements Listener, EventCreator, TaskCreator, Runnable
{
	private static final List<WeakReference<LogEvent>> logEvents = Lists.newCopyOnWriteArrayList();
	
	static
	{
		new Out();
	}
	
	Out()
	{
		ScheduleManager.INSTANCE.scheduleAsyncRepeatingTask( this, this, 5000L, 5000L );
		EventBus.INSTANCE.registerEvents( this, this );
	}
	
	public static LogEvent logEvent( String id )
	{
		LogEvent e = new LogEvent();
		logEvents.add( new WeakReference<LogEvent>( e ) );
		return e;
	}
	
	@Override
	public void run()
	{
		
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "OutLog";
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	
}
