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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.google.common.base.Strings;

public class ConsoleLogFormatter extends Formatter
{
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat timeFormat;
	private boolean formatConfigLoaded = false;
	private final boolean useColor;
	
	public static boolean debugMode = false;
	public static int debugModeHowDeep = 1;
	
	public ConsoleLogFormatter( ConsoleBus console )
	{
		this( console, true );
	}
	
	public ConsoleLogFormatter( ConsoleBus console, boolean useColor )
	{
		dateFormat = new SimpleDateFormat( "MM-dd" );
		timeFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );
		this.useColor = useColor;
	}
	
	@Override
	public String format( LogRecord record )
	{
		if ( Loader.getConfig() != null && !formatConfigLoaded )
		{
			dateFormat = new SimpleDateFormat( Loader.getConfig().getString( "console.dateFormat", "MM-dd" ) );
			timeFormat = new SimpleDateFormat( Loader.getConfig().getString( "console.timeFormat", "HH:mm:ss.SSS" ) );
			formatConfigLoaded = true;
		}
		
		String style = ( Loader.getConfig() == null ) ? "&r&7%dt %tm [%lv&7]&f" : Loader.getConfig().getString( "console.style", "&r&7[&d%ct&7] %dt %tm [%lv&7]&f" );
		
		Throwable ex = record.getThrown();
		
		if ( style.contains( "%ct" ) )
		{
			String threadName = Thread.currentThread().getName();
			
			if ( threadName.length() > 10 )
				threadName = threadName.substring( 0, 2 ) + ".." + threadName.substring( threadName.length() - 6 );
			else if ( threadName.length() < 10 )
				threadName = threadName + Strings.repeat( " ", 10 - threadName.length() );
			
			style = style.replaceAll( "%ct", threadName );
		}
		
		style = style.replaceAll( "%dt", dateFormat.format( record.getMillis() ) );
		style = style.replaceAll( "%tm", timeFormat.format( record.getMillis() ) );
		
		int howDeep = debugModeHowDeep;
		
		if ( debugMode )
		{
			StackTraceElement[] var1 = Thread.currentThread().getStackTrace();
			
			for ( StackTraceElement var2 : var1 )
			{
				if ( !var2.getClassName().toLowerCase().contains( "java" ) && !var2.getClassName().toLowerCase().contains( "sun" ) && !var2.getClassName().toLowerCase().contains( "log" ) && !var2.getMethodName().equals( "sendMessage" ) && !var2.getMethodName().equals( "sendRawMessage" ) )
				{
					howDeep--;
					
					if ( howDeep <= 0 )
					{
						style += " " + var2.getClassName() + "$" + var2.getMethodName() + ":" + var2.getLineNumber();
						break;
					}
				}
			}
		}
		
		if ( style.contains( "%lv" ) )
			style = style.replaceAll( "%lv", ConsoleColor.fromLevel( record.getLevel() ) + record.getLevel().getLocalizedName().toUpperCase() );
		
		style += " " + formatMessage( record );
		
		if ( !formatMessage( record ).endsWith( "\r" ) )
			style += "\n";
		
		if ( ex != null )
		{
			StringWriter writer = new StringWriter();
			ex.printStackTrace( new PrintWriter( writer ) );
			style += writer;
		}
		
		if ( !Loader.getConsole().useColors || !useColor )
			return ConsoleColor.removeAltColors( style );
		else
			return ConsoleColor.transAltColors( style );
	}
}
