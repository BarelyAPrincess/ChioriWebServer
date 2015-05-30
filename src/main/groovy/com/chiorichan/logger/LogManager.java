/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.logger;

import com.chiorichan.util.GarbageCollectingMap;

/**
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class LogManager
{
	static class LogRecord
	{
		
	}
	
	/**
	 * This map allows our manager to know when the object is done being used by detecting when the LogEvent is GC'ed
	 */
	private static final GarbageCollectingMap<String, LogRecord> logs = new GarbageCollectingMap<String, LogRecord>();
	
	public static LogEvent logEvent( String id )
	{
		LogRecord r = new LogRecord();
		LogEvent e = new LogEvent( r );
		logs.put( id, r, e );
		return e;
	}
}
