package com.chiorichan;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
	 * @param parent
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
		log( Level.INFO, ChatColor.GOLD + "" + ChatColor.NEGATIVE + msg );
	}
	
	public void info( String s )
	{
		log( Level.INFO, ChatColor.WHITE + s );
	}
	
	public void warning( String s )
	{
		log( Level.WARNING, ChatColor.GOLD + s );
	}
	
	public void warning( String s, Object... aobject )
	{
		logger.log( Level.WARNING, ChatColor.GOLD + s, aobject );
	}
	
	public void warning( String s, Throwable throwable )
	{
		log( Level.WARNING, ChatColor.GOLD + s, throwable );
	}
	
	public void severe( String s )
	{
		log( Level.SEVERE, ChatColor.RED + s );
	}
	
	public void severe( String s, Throwable throwable )
	{
		log( Level.SEVERE, ChatColor.RED + s, throwable );
	}
	
	public void panic( Throwable e )
	{
		severe( e.getMessage(), e );
		System.exit( 1 );
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
			log( Level.FINE, ChatColor.GOLD + "<CLIENT ID>     <MESSAGE>" );
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
		
		log( l, ChatColor.LIGHT_PURPLE + client + " " + ChatColor.AQUA + msg );
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
		String color = ChatColor.getLastColors( var1[0] );
		
		for ( int l = 0; l < var1.length; l++ )
		{
			var1[l] = color + var1[l];
		}
		
		return var1;
	}
	
	public void debug( String... var1 )
	{
		for ( String var2 : var1 )
			info( ChatColor.NEGATIVE + "" + ChatColor.YELLOW + " >>>>   " + var2 + "   <<<< " );
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
}
