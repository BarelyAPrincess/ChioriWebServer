/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.logger;

import java.util.logging.Level;

/**
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class LogEvent implements ILogEvent
{
	final String id;
	final LogRecord record;
	
	LogEvent( String id, LogRecord record )
	{
		this.id = id;
		this.record = record;
	}
	
	@Override
	public void log( Level level, String msg, Object... objs )
	{
		record.log( level, msg, objs );
	}
	
	public void flushAndClose()
	{
		flush();
		close();
	}
	
	@Override
	public void flush()
	{
		record.flush();
	}
	
	public void close()
	{
		LogManager.close( this );
	}
}
