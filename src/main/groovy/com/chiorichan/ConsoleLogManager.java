/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.io.File;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.google.common.collect.Sets;

public class ConsoleLogManager
{
	private static Set<ConsoleLogger> loggers = Sets.newHashSet();
	private static Logger logger = Logger.getLogger( "" );
	
	public void init()
	{
		for ( Handler h : logger.getHandlers() )
			logger.removeHandler( h );
		
		try
		{
			String filename = (String) Loader.getOptions().valueOf( "log-pattern" );
			
			String tmpDir = System.getProperty( "java.io.tmpdir" );
			String homeDir = System.getProperty( "user.home" );
			if ( tmpDir == null )
			{
				tmpDir = homeDir;
			}
			
			File parent = new File( filename ).getParentFile();
			StringBuilder fixedPattern = new StringBuilder();
			String parentPath = "";
			if ( parent != null )
			{
				parentPath = parent.getPath();
			}
			
			int i = 0;
			while( i < parentPath.length() )
			{
				char ch = parentPath.charAt( i );
				char ch2 = 0;
				if ( i + 1 < parentPath.length() )
				{
					ch2 = Character.toLowerCase( filename.charAt( i + 1 ) );
				}
				
				if ( ch == '%' )
				{
					if ( ch2 == 'h' )
					{
						i += 2;
						fixedPattern.append( homeDir );
						continue;
					}
					else if ( ch2 == 't' )
					{
						i += 2;
						fixedPattern.append( tmpDir );
						continue;
					}
					else if ( ch2 == '%' )
					{
						// Even though we don't care about this we have to skip it to avoid matching %%t
						i += 2;
						fixedPattern.append( "%%" );
						continue;
					}
					else if ( ch2 != 0 )
					{
						throw new java.io.IOException( "log-pattern can only use %t and %h for directories, got %" + ch2 );
					}
				}
				
				fixedPattern.append( ch );
				i++;
			}
			
			parent = new File( fixedPattern.toString() );
			if ( parent != null )
				parent.mkdirs();
			
			int limit = (Integer) Loader.getOptions().valueOf( "log-limit" );
			int count = (Integer) Loader.getOptions().valueOf( "log-count" );
			boolean append = (Boolean) Loader.getOptions().valueOf( "log-append" );
			FileHandler fileHandler = new FileHandler( filename, limit, count, append );
			fileHandler.setFormatter( new ConsoleLogFormatter( Loader.getConsole() ) );
			
			logger.addHandler( fileHandler );
		}
		catch( Exception exception )
		{
			getLogger().warning( "Failed to log to server.log", exception );
		}
		
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter( new ConsoleLogFormatter( Loader.getConsole() ) );
		
		logger.addHandler( consoleHandler );
	}
	
	public ConsoleLogger getLogger()
	{
		return getLogger( "Core" );
	}
	
	public ConsoleLogger getLogger( String loggerId )
	{
		for ( ConsoleLogger log : loggers )
			if ( log.getId().equals( loggerId ) )
				return log;
		
		ConsoleLogger log = new ConsoleLogger( loggerId );
		loggers.add( log );
		return log;
	}
	
	public Logger getParent()
	{
		return logger;
	}
}
