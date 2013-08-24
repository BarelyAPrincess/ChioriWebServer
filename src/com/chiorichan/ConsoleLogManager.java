package com.chiorichan;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chiorichan.server.Server;
import com.google.common.base.Strings;

public class ConsoleLogManager
{
	public final Logger a;
	public static Logger global = Logger.getLogger( "" );
	
	public ConsoleLogManager(String s)
	{
		a = Logger.getLogger( s );
	}
	
	public void init()
	{
		ConsoleLogFormatter consolelogformatter = new ConsoleLogFormatter( false );
		
		Server server = Main.getServer();
		ConsoleHandler consolehandler = new TerminalConsoleHandler( server.reader );
		
		consolehandler.setFormatter( consolelogformatter );
		a.addHandler( consolehandler );
		
		for ( java.util.logging.Handler handler : global.getHandlers() )
		{
			global.removeHandler( handler );
		}
		
		consolehandler.setFormatter( new ShortConsoleLogFormatter( server ) );
		global.addHandler( consolehandler );
		
		try
		{
			String pattern = (String) server.options.valueOf( "log-pattern" );
			
			String tmpDir = System.getProperty( "java.io.tmpdir" );
			String homeDir = System.getProperty( "user.home" );
			if ( tmpDir == null )
			{
				tmpDir = homeDir;
			}
			
			File parent = new File( pattern ).getParentFile();
			StringBuilder fixedPattern = new StringBuilder();
			String parentPath = "";
			if ( parent != null )
			{
				parentPath = parent.getPath();
			}
			
			int i = 0;
			while ( i < parentPath.length() )
			{
				char ch = parentPath.charAt( i );
				char ch2 = 0;
				if ( i + 1 < parentPath.length() )
				{
					ch2 = Character.toLowerCase( pattern.charAt( i + 1 ) );
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
			
			// Try to create needed parent directories
			parent = new File( fixedPattern.toString() );
			if ( parent != null )
			{
				parent.mkdirs();
			}
			
			int limit = 0;// (Integer) server.options.valueOf( "log-limit" );
			int count = 0;// (Integer) server.options.valueOf( "log-count" );
			boolean append = true;// (Boolean) server.options.valueOf( "log-append" );
			FileHandler filehandler = new FileHandler( pattern, limit, count, append );
			
			// filehandler.setFormatter( consolelogformatter );
			a.addHandler( filehandler );
			global.addHandler( filehandler );
		}
		catch ( Exception exception )
		{
			a.log( Level.WARNING, "Failed to log to server.log", exception );
		}
	}
	
	public Logger getLogger()
	{
		return a;
	}
	
	public void info( String s )
	{
		a.log( Level.INFO, s );
	}
	
	public void warning( String s )
	{
		a.log( Level.WARNING, s );
	}
	
	public void warning( String s, Object... aobject )
	{
		a.log( Level.WARNING, s, aobject );
	}
	
	public void warning( String s, Throwable throwable )
	{
		a.log( Level.WARNING, s, throwable );
	}
	
	public void severe( String s )
	{
		a.log( Level.SEVERE, s );
	}
	
	public void severe( String s, Throwable throwable )
	{
		a.log( Level.SEVERE, s, throwable );
	}

	public void log( Level severe, String string, IOException ex )
	{
		a.log( severe, string, ex );
	}
	
	/*
	public void log( Level l, String client, String msg )
	{
		if ( client.length() < 15 )
		{
			client = client + Strings.repeat( " ", 15 - client.length() );
		}
		
		//printHeader();
		
		log( l, "&5" + client + " &a" + msg );
	}
	
	public void log( Level l, String msg )
	{
		if ( terminal.isAnsiSupported() )
		{
			msg = ChatColor.translateAlternateColorCodes( '&', msg ) + ChatColor.RESET;
			
			String result = ChatColor.translateAlternateColorCodes( '&', msg );
			for ( ChatColor color : colors )
			{
				if ( replacements.containsKey( color ) )
				{
					msg = msg.replaceAll( "(?i)" + color.toString(), replacements.get( color ) );
				}
				else
				{
					msg = msg.replaceAll( "(?i)" + color.toString(), "" );
				}
			}
		}
		
		log.log( l, msg );
	}
	*/
}