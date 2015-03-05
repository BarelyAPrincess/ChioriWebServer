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
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

import com.google.common.base.Strings;

public class ConsoleLogFormatter extends Formatter
{
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat timeFormat;
	private boolean formatConfigLoaded = false;
	private boolean useColor;
	
	private static Map<ConsoleColor, String> replacements = new EnumMap<ConsoleColor, String>( ConsoleColor.class );
	private static ConsoleColor[] colors = ConsoleColor.values();
	
	public static boolean debugMode = false;
	public static int debugModeHowDeep = 1;
	
	static
	{
		replacements.put( ConsoleColor.BLACK, Ansi.ansi().fg( Ansi.Color.BLACK ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_RED, Ansi.ansi().fg( Ansi.Color.RED ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).boldOff().toString() );
		replacements.put( ConsoleColor.GOLD, Ansi.ansi().fg( Ansi.Color.YELLOW ).boldOff().toString() );
		replacements.put( ConsoleColor.GRAY, Ansi.ansi().fg( Ansi.Color.WHITE ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_GRAY, Ansi.ansi().fg( Ansi.Color.BLACK ).bold().toString() );
		replacements.put( ConsoleColor.BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).bold().toString() );
		replacements.put( ConsoleColor.GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).bold().toString() );
		replacements.put( ConsoleColor.AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).bold().toString() );
		replacements.put( ConsoleColor.RED, Ansi.ansi().fg( Ansi.Color.RED ).bold().toString() );
		replacements.put( ConsoleColor.LIGHT_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).bold().toString() );
		replacements.put( ConsoleColor.YELLOW, Ansi.ansi().fg( Ansi.Color.YELLOW ).bold().toString() );
		replacements.put( ConsoleColor.WHITE, Ansi.ansi().fg( Ansi.Color.WHITE ).bold().toString() );
		replacements.put( ConsoleColor.MAGIC, Ansi.ansi().a( Attribute.BLINK_SLOW ).toString() );
		replacements.put( ConsoleColor.BOLD, Ansi.ansi().a( Attribute.INTENSITY_BOLD ).toString() );
		replacements.put( ConsoleColor.STRIKETHROUGH, Ansi.ansi().a( Attribute.STRIKETHROUGH_ON ).toString() );
		replacements.put( ConsoleColor.UNDERLINE, Ansi.ansi().a( Attribute.UNDERLINE ).toString() );
		replacements.put( ConsoleColor.ITALIC, Ansi.ansi().a( Attribute.ITALIC ).toString() );
		replacements.put( ConsoleColor.FAINT, Ansi.ansi().a( Attribute.INTENSITY_FAINT ).toString() );
		replacements.put( ConsoleColor.NEGATIVE, Ansi.ansi().a( Attribute.NEGATIVE_ON ).toString() );
		replacements.put( ConsoleColor.RESET, Ansi.ansi().a( Attribute.RESET ).fg( Ansi.Color.DEFAULT ).toString() );
	}
	
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
	
	public ConsoleColor getLevelColor( Level var1 )
	{
		if ( var1 == Level.FINEST || var1 == Level.FINER || var1 == Level.FINE )
			return ConsoleColor.WHITE;
		else if ( var1 == Level.INFO )
			return ConsoleColor.AQUA;
		else if ( var1 == Level.WARNING )
			return ConsoleColor.GOLD;
		else if ( var1 == Level.SEVERE )
			return ConsoleColor.RED;
		else if ( var1 == Level.CONFIG )
			return ConsoleColor.WHITE;
		else
			return ConsoleColor.WHITE;
	}
	
	public String handleAltColors( String var1 )
	{
		if ( !Loader.getConsole().useColors || !useColor )
		{
			var1 = var1.replaceAll( "&.", "" );
			var1 = var1.replaceAll( "§.", "" );
		}
		else
		{
			var1 = ConsoleColor.translateAlternateColorCodes( '&', var1 ) + ConsoleColor.RESET;
			
			for ( ConsoleColor color : colors )
			{
				if ( replacements.containsKey( color ) )
					var1 = var1.replaceAll( "(?i)" + color.toString(), replacements.get( color ) );
				else
					var1 = var1.replaceAll( "(?i)" + color.toString(), "" );
			}
		}
		
		return var1;
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
			style = style.replaceAll( "%lv", getLevelColor( record.getLevel() ) + record.getLevel().getLocalizedName().toUpperCase() );
		
		style += " " + formatMessage( record );
		
		if ( !formatMessage( record ).endsWith( "\r" ) )
			style += "\n";
		
		if ( ex != null )
		{
			StringWriter writer = new StringWriter();
			ex.printStackTrace( new PrintWriter( writer ) );
			style += writer;
		}
		
		return handleAltColors( style );
	}
}
