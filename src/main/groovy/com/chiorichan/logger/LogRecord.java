/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.logger;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Lists;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
class LogRecord implements ILogEvent
{
	String header = null;
	
	static class LogElement
	{
		Level level;
		String msg;
		ConsoleColor color;
		long time = Timings.epoch();
		
		LogElement( Level level, String msg, ConsoleColor color )
		{
			this.level = level;
			this.msg = msg;
			this.color = color;
		}
	}
	
	final List<LogElement> elements = Lists.newLinkedList();
	
	public LogRecord()
	{
		
	}
	
	@Override
	public void header( String msg, Object... objs )
	{
		header = String.format( msg, objs );
	}
	
	@Override
	public void log( Level level, String msg, Object... objs )
	{
		elements.add( new LogElement( level, String.format( msg, objs ), ConsoleColor.fromLevel( level ) ) );
	}
	
	@Override
	public void flush()
	{
		StringBuilder sb = new StringBuilder();
		
		if ( header != null )
			sb.append( ConsoleColor.GOLD + header );
		
		for ( LogElement e : elements )
			sb.append( ConsoleColor.GRAY + "\n  |-> " + new SimpleDateFormat( "HH:mm:ss.SSS" ).format( e.time ) + " " + e.color + e.msg );
		
		Loader.getLogger().log( Level.INFO, "\r" + sb.toString() );
		
		elements.clear();
	}
}
