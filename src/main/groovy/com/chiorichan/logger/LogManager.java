/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.logger;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.TaskManager;
import com.google.common.collect.Maps;

/**
 */
public class LogManager implements TaskRegistrar
{
	private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
	private static final ConcurrentMap<String, LogReference> activeLogs = Maps.newConcurrentMap();
	
	public static final LogManager INSTANCE = new LogManager();
	
	private LogManager()
	{
		TaskManager.INSTANCE.runTaskAsynchronously( this, new Runnable()
		{
			@Override
			public void run()
			{
				for ( ;; )
				{
					try
					{
						LogReference ref = ( LogReference ) referenceQueue.remove();
						for ( ;; )
						{
							activeLogs.remove( ref.key );
							ref.record.flush();
							ref = ( LogReference ) referenceQueue.remove();
						}
					}
					catch ( InterruptedException e )
					{
						// Do Nothing
					}
				}
			}
		} );
	}
	
	public static LogEvent logEvent( String id )
	{
		if ( activeLogs.containsKey( id ) )
			return ( LogEvent ) activeLogs.get( id ).get();
		
		LogRecord r = new LogRecord();
		LogEvent e = new LogEvent( id, r );
		activeLogs.put( id, new LogReference( id, r, e ) );
		return e;
	}
	
	public static void close( LogEvent log )
	{
		activeLogs.remove( log.id );
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "LogManager";
	}
	
	static class LogReference extends WeakReference<Object>
	{
		final String key;
		final LogRecord record;
		
		LogReference( String key, LogRecord record, Object garbage )
		{
			super( garbage, referenceQueue );
			this.key = key;
			this.record = record;
		}
	}
}
