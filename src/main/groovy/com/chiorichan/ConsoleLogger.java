/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.EvalException;
import com.google.common.base.Strings;

public class ConsoleLogger
{
	private int lineCount = 999;
	private Logger logger = null;
	private String id;
	
	/**
	 * Attempts to find a logger based on the id provided.
	 * If you would like to use your own Logger, be sure to create it
	 * with the same id prior to using any of the builtin getLogger() methods
	 * or you will need to use the replaceLogger() method.
	 * 
	 * @param loggerId
	 */
	protected ConsoleLogger( String loggerId )
	{
		logger = LogManager.getLogManager().getLogger( loggerId );
		
		if ( logger == null )
			logger = new ConsoleSubLogger( loggerId );
		
		id = loggerId;
		
		logger.setParent( Loader.getLogManager().getParent() );
		logger.setLevel( Level.ALL );
	}
	
	public String getId()
	{
		return id;
	}
	
	public void highlight( String msg )
	{
		log( Level.INFO, ConsoleColor.GOLD + "" + ConsoleColor.NEGATIVE + msg );
	}
	
	public void info( String s )
	{
		log( Level.INFO, ConsoleColor.WHITE + s );
	}
	
	public void warning( String s )
	{
		log( Level.WARNING, ConsoleColor.GOLD + s );
	}
	
	public void warning( String s, Object... aobject )
	{
		logger.log( Level.WARNING, ConsoleColor.GOLD + s, aobject );
	}
	
	public void warning( String s, Throwable throwable )
	{
		log( Level.WARNING, ConsoleColor.GOLD + s, throwable );
	}
	
	public void severe( String s )
	{
		log( Level.SEVERE, ConsoleColor.RED + s );
	}
	
	public void severe( Throwable t )
	{
		log( Level.SEVERE, ConsoleColor.RED + t.getMessage(), t );
	}
	
	public void severe( String s, Throwable t )
	{
		log( Level.SEVERE, ConsoleColor.RED + s, t );
	}
	
	public void panic( Throwable e )
	{
		severe( e );
		Loader.stop( "The server is stopping due to a severe error!" );
	}
	
	public void panic( String var1 )
	{
		severe( var1 );
		Loader.stop( var1 );
	}
	
	public void fine( String var1 )
	{
		logger.log( Level.FINE, var1 );
	}
	
	public void finer( String var1 )
	{
		logger.log( Level.FINER, var1 );
	}
	
	public void finest( String var1 )
	{
		logger.log( Level.FINEST, var1 );
	}
	
	private void printHeader()
	{
		if ( lineCount > 40 )
		{
			lineCount = 0;
			log( Level.FINE, ConsoleColor.GOLD + "<CLIENT ID>     <MESSAGE>" );
		}
		
		lineCount++;
	}
	
	public void log( Level l, String client, String msg )
	{
		if ( client.length() < 15 )
		{
			client = client + Strings.repeat( " ", 15 - client.length() );
		}
		
		printHeader();
		
		log( l, ConsoleColor.LIGHT_PURPLE + client + " " + ConsoleColor.AQUA + msg );
	}
	
	public void log( Level l, String msg, Throwable t )
	{
		logger.log( l, msg, t );
	}
	
	public void log( Level l, String msg )
	{
		logger.log( l, msg );
	}
	
	public String[] multilineColorRepeater( String var1 )
	{
		return multilineColorRepeater( var1.split( "\\n" ) );
	}
	
	public String[] multilineColorRepeater( String[] var1 )
	{
		String color = ConsoleColor.getLastColors( var1[0] );
		
		for ( int l = 0; l < var1.length; l++ )
		{
			var1[l] = color + var1[l];
		}
		
		return var1;
	}
	
	public void debug( String... var1 )
	{
		if ( !Loader.getConfig().getBoolean( "console.developerMode", true ) )
			return;
		
		for ( String var2 : var1 )
			info( ConsoleColor.NEGATIVE + "" + ConsoleColor.YELLOW + " >>>>   " + var2 + "   <<<< " );
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	public void exceptions( EvalException... exceptions )
	{
		for ( EvalException e : exceptions )
			if ( e.errorLevel().isEnabledLevel() )
				if ( e.isScriptingException() )
				{
					ScriptTraceElement element = e.getScriptTrace()[0];
					severe( String.format( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s:%s, message '%s'", e.getClass().getName(), element.getMetaData().fileName, element.getLineNumber(), ( element.getColumnNumber() > 0 ) ? element.getColumnNumber() : 0, e.getMessage() ) );
				}
				else
					severe( String.format( ConsoleColor.NEGATIVE + "" + ConsoleColor.RED + "Exception %s thrown in file '%s' at line %s, message '%s'", e.getClass().getName(), e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(), e.getMessage() ) );
	}
}
