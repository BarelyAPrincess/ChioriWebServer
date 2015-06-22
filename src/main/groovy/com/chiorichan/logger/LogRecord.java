/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.logger;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.EvalException;
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
		long time = System.currentTimeMillis();
		
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
		if ( objs.length < 1 )
			elements.add( new LogElement( level, msg, ConsoleColor.fromLevel( level ) ) );
		else
			elements.add( new LogElement( level, String.format( msg, objs ), ConsoleColor.fromLevel( level ) ) );
	}
	
	@Override
	public void flush()
	{
		StringBuilder sb = new StringBuilder();
		
		if ( header != null )
			sb.append( ConsoleColor.GOLD + header );
		
		for ( LogElement e : elements )
			sb.append( ConsoleColor.RESET + "" + ConsoleColor.GRAY + "\n  |-> " + new SimpleDateFormat( "ss.SSS" ).format( e.time ) + " " + e.color + e.msg );
		
		Loader.getLogger().log( Level.INFO, "\r" + sb.toString() );
		
		elements.clear();
	}
	
	@Override
	public void exceptions( EvalException... exceptions )
	{
		for ( EvalException e : exceptions )
			if ( e.errorLevel().isEnabledLevel() )
				if ( e.isScriptingException() )
				{
					ScriptTraceElement element = e.getScriptTrace()[0];
					Throwable t = e.getCause();
					log( Level.SEVERE, ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s:%s, message '%s'", t.getClass().getName(), element.context().filename(), element.getLineNumber(), ( element.getColumnNumber() > 0 ) ? element.getColumnNumber() : 0, e.getMessage() );
				}
				else
				{
					Throwable t = e.getCause();
					log( Level.SEVERE, ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s, message '%s'", t.getClass().getName(), t.getStackTrace()[0].getFileName(), t.getStackTrace()[0].getLineNumber(), t.getMessage() );
				}
	}
}
