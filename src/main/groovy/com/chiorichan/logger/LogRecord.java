/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.logger;

import java.util.List;
import java.util.logging.Level;

import com.chiorichan.Loader;
import com.google.common.collect.Lists;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
class LogRecord implements ILogEvent
{
	static class LogElement
	{
		Level level;
		String msg;
		
		LogElement( Level level, String msg )
		{
			this.level = level;
			this.msg = msg;
		}
	}
	
	final List<LogElement> elements = Lists.newLinkedList();
	
	public LogRecord()
	{
		Loader.getLogger().debug( "Start LogRecord" );
	}
	
	@Override
	public void log( Level level, String msg, Object... objs )
	{
		elements.add( new LogElement( level, String.format( msg, objs ) ) );
	}
	
	@Override
	public void flush()
	{
		// [id: 0x1e21deab, /173.245.53.148:61025 => /66.45.240.230:8080] WRITE: 4B
		
		StringBuilder sb = new StringBuilder();
		
		for ( LogElement e : elements )
			sb.append( "\n|-->" + e.msg );
		
		Loader.getLogger().log( Level.INFO, sb.toString() );
		elements.clear();
	}
}
