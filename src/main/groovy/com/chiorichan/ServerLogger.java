/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.EvalException;
import com.chiorichan.util.Versioning;
import com.google.common.base.Strings;

public class ServerLogger
{
	private int lineCount = 999;
	private final Logger logger;
	private final String id;
	
	/**
	 * Attempts to find a logger based on the id provided.
	 * If you would like to use your own Logger, be sure to create it
	 * with the same id prior to using any of the builtin getLogger() methods
	 * or you will need to use the replaceLogger() method.
	 * 
	 * @param loggerId
	 */
	protected ServerLogger( String id )
	{
		Logger logger = LogManager.getLogManager().getLogger( id );
		
		if ( logger == null )
			logger = new ServerSubLogger( id );
		
		logger.setParent( Loader.getLogManager().getParent() );
		logger.setLevel( Level.ALL );
		
		this.id = id;
		this.logger = logger;
	}
	
	public void debug( Object... var1 )
	{
		if ( !Versioning.isDevelopment() )
			return;
		
		for ( Object var2 : var1 )
			info( LogColor.NEGATIVE + "" + LogColor.YELLOW + " >>>>   " + var2.toString() + "   <<<< " );
	}
	
	public void exceptions( EvalException... exceptions )
	{
		for ( EvalException e : exceptions )
			if ( e.errorLevel().isEnabledLevel() )
				if ( e.isScriptingException() )
				{
					ScriptTraceElement element = e.getScriptTrace()[0];
					severe( String.format( LogColor.NEGATIVE + "" + LogColor.RED + "Exception %s thrown in file '%s' at line %s:%s, message '%s'", e.getClass().getName(), element.context().filename(), element.getLineNumber(), ( element.getColumnNumber() > 0 ) ? element.getColumnNumber() : 0, e.getMessage() ) );
				}
				else
					severe( String.format( LogColor.NEGATIVE + "" + LogColor.RED + "Exception %s thrown in file '%s' at line %s, message '%s'", e.getClass().getName(), e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(), e.getMessage() ) );
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
	
	public String getId()
	{
		return id;
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	public void highlight( String msg )
	{
		log( Level.INFO, LogColor.GOLD + "" + LogColor.NEGATIVE + msg );
	}
	
	public void info( String s )
	{
		log( Level.INFO, LogColor.WHITE + s );
	}
	
	public void log( Level l, String msg )
	{
		logger.log( l, msg );
	}
	
	public void log( Level l, String client, String msg )
	{
		if ( client.length() < 15 )
			client = client + Strings.repeat( " ", 15 - client.length() );
		
		printHeader();
		
		log( l, LogColor.LIGHT_PURPLE + client + " " + LogColor.AQUA + msg );
	}
	
	public void log( Level l, String msg, Throwable t )
	{
		logger.log( l, msg, t );
	}
	
	public String[] multilineColorRepeater( String var1 )
	{
		return multilineColorRepeater( var1.split( "\\n" ) );
	}
	
	public String[] multilineColorRepeater( String[] var1 )
	{
		String color = LogColor.getLastColors( var1[0] );
		
		for ( int l = 0; l < var1.length; l++ )
			var1[l] = color + var1[l];
		
		return var1;
	}
	
	public void panic( String var1 )
	{
		severe( var1 );
		Loader.stop( var1 );
	}
	
	public void panic( Throwable e )
	{
		severe( e );
		Loader.stop( "The server is stopping due to a severe error!" );
	}
	
	private void printHeader()
	{
		if ( lineCount > 40 )
		{
			lineCount = 0;
			log( Level.FINE, LogColor.GOLD + "<CLIENT ID>     <MESSAGE>" );
		}
		
		lineCount++;
	}
	
	public void severe( String s )
	{
		log( Level.SEVERE, LogColor.RED + s );
	}
	
	public void severe( String s, Throwable t )
	{
		log( Level.SEVERE, LogColor.RED + s, t );
	}
	
	public void severe( Throwable t )
	{
		log( Level.SEVERE, LogColor.RED + t.getMessage(), t );
	}
	
	public void warning( String s )
	{
		log( Level.WARNING, LogColor.GOLD + s );
	}
	
	public void warning( String s, Object... aobject )
	{
		logger.log( Level.WARNING, LogColor.GOLD + s, aobject );
	}
	
	public void warning( String s, Throwable throwable )
	{
		log( Level.WARNING, LogColor.GOLD + s, throwable );
	}
}