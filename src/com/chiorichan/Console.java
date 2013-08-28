package com.chiorichan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.Terminal;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

import sun.net.dns.ResolverConfiguration.Options;

import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.RemoteConsoleCommandSender;
import com.chiorichan.command.ServerCommand;
import com.google.common.base.Strings;

public class Console
{
	public static Logger log = Logger.getLogger( "ChioriWebServer" );
	
	private final List commandList = Collections.synchronizedList( new ArrayList() );
	public ConsoleCommandSender console;
	public RemoteConsoleCommandSender remoteConsole;
	
	public ConsoleReader reader;
	public Terminal terminal;
	public Map<ChatColor, String> replacements = new EnumMap<ChatColor, String>( ChatColor.class );
	public ChatColor[] colors = ChatColor.values();
	public Boolean isRunning = true;
	private OptionSet options;
	
	public boolean useJline = true;
	public boolean useConsole = true;
	
	private int lineCount = 999;
	
	public void init()
	{
		options = Loader.getOptions();
		
		try
		{
			replacements.put( ChatColor.BLACK, Ansi.ansi().fg( Ansi.Color.BLACK ).boldOff().toString() );
			replacements.put( ChatColor.DARK_BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).boldOff().toString() );
			replacements.put( ChatColor.DARK_GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).boldOff().toString() );
			replacements.put( ChatColor.DARK_AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).boldOff().toString() );
			replacements.put( ChatColor.DARK_RED, Ansi.ansi().fg( Ansi.Color.RED ).boldOff().toString() );
			replacements.put( ChatColor.DARK_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).boldOff().toString() );
			replacements.put( ChatColor.GOLD, Ansi.ansi().fg( Ansi.Color.YELLOW ).boldOff().toString() );
			replacements.put( ChatColor.GRAY, Ansi.ansi().fg( Ansi.Color.WHITE ).boldOff().toString() );
			replacements.put( ChatColor.DARK_GRAY, Ansi.ansi().fg( Ansi.Color.BLACK ).bold().toString() );
			replacements.put( ChatColor.BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).bold().toString() );
			replacements.put( ChatColor.GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).bold().toString() );
			replacements.put( ChatColor.AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).bold().toString() );
			replacements.put( ChatColor.RED, Ansi.ansi().fg( Ansi.Color.RED ).bold().toString() );
			replacements.put( ChatColor.LIGHT_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).bold().toString() );
			replacements.put( ChatColor.YELLOW, Ansi.ansi().fg( Ansi.Color.YELLOW ).bold().toString() );
			replacements.put( ChatColor.WHITE, Ansi.ansi().fg( Ansi.Color.WHITE ).bold().toString() );
			replacements.put( ChatColor.MAGIC, Ansi.ansi().a( Attribute.BLINK_SLOW ).toString() );
			replacements.put( ChatColor.BOLD, Ansi.ansi().a( Attribute.UNDERLINE_DOUBLE ).toString() );
			replacements.put( ChatColor.STRIKETHROUGH, Ansi.ansi().a( Attribute.STRIKETHROUGH_ON ).toString() );
			replacements.put( ChatColor.UNDERLINE, Ansi.ansi().a( Attribute.UNDERLINE ).toString() );
			replacements.put( ChatColor.ITALIC, Ansi.ansi().a( Attribute.ITALIC ).toString() );
			replacements.put( ChatColor.RESET, Ansi.ansi().a( Attribute.RESET ).fg( Ansi.Color.DEFAULT ).toString() );
			
			String jline_UnsupportedTerminal = new String( new char[] { 'j', 'l', 'i', 'n', 'e', '.', 'U', 'n', 's', 'u', 'p', 'p', 'o', 'r', 't', 'e', 'd', 'T', 'e', 'r', 'm', 'i', 'n', 'a', 'l' } );
			String jline_terminal = new String( new char[] { 'j', 'l', 'i', 'n', 'e', '.', 't', 'e', 'r', 'm', 'i', 'n', 'a', 'l' } );
			
			useJline = !( jline_UnsupportedTerminal ).equals( System.getProperty( jline_terminal ) );
			
			if ( options.has( "nojline" ) )
			{
				System.setProperty( "user.language", "en" );
				useJline = false;
			}
			
			if ( !useJline )
			{
				System.setProperty( jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName() );
			}
			
			if ( options.has( "noconsole" ) )
			{
				info( "Console input is disabled due to --noconsole command argument" );
				useConsole = false;
			}
			
			try
			{
				this.reader = new ConsoleReader( System.in, System.out );
				this.reader.setExpandEvents( false ); // Avoid parsing exceptions for uncommonly used event designators
			}
			catch ( Exception e )
			{
				try
				{
					// Try again with jline disabled for Windows users without C++ 2008 Redistributable
					System.setProperty( "jline.terminal", "jline.UnsupportedTerminal" );
					System.setProperty( "user.language", "en" );
					this.reader = new ConsoleReader( System.in, System.out );
					this.reader.setExpandEvents( false );
				}
				catch ( java.io.IOException ex )
				{
					// Logger.getLogger( MinecraftServer.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
			
			reader.setPrompt( "?> " );
			terminal = reader.getTerminal();
			
			ThreadCommandReader threadcommandreader = new ThreadCommandReader( this );
			
			threadcommandreader.setDaemon( true );
			threadcommandreader.start();
			new ConsoleLogManager( "ChioriWebServer" ).init();
			
			System.setOut( new PrintStream( new LoggerOutputStream( log, Level.INFO ), true ) );
			System.setErr( new PrintStream( new LoggerOutputStream( log, Level.SEVERE ), true ) );
			
			sendMessage( ChatColor.RED + "Finsihed initalizing the server console.\n" );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
	
	public Logger getLogger()
	{
		return log;
	}
	
	public void issueCommand( String s, CommandSender commandSender )
	{
		commandList.add( new ServerCommand( s, commandSender ) );
	}
	
	public void issueCommand( String cmd )
	{
		String arr[] = cmd.split( " ", 2 );
		cmd = arr[0].toLowerCase();
		String data = ( arr.length > 1 ) ? arr[1].trim() : null;
		
		if ( cmd.equalsIgnoreCase( "quit" ) || cmd.equalsIgnoreCase( "exit" ) || cmd.equalsIgnoreCase( "stop" ) )
		{
			sendMessage( "&4Server is now Shutting Down!!!" );
			// reader.getTerminal().restore();
			System.exit( 0 );
		}
		else if ( cmd.equals( "ping" ) )
		{
			sendMessage( "&4PONG!" );
		}
		else
		{
			sendMessage( "&4Unknown Command or Keyword, Please Try Again. :D :D :D" );
		}
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
	
	public void debug( String msg )
	{
		log( Level.FINE, msg );
	}
	
	public void info( String msg )
	{
		log( Level.INFO, msg );
	}
	
	public void warning( String msg, Throwable t )
	{
		log( Level.WARNING, msg );
	}
	
	public void warning( String msg )
	{
		log( Level.WARNING, msg );
	}
	
	public void severe( String msg, Throwable t )
	{
		log( Level.SEVERE, msg );
	}
	
	public void severe( String msg )
	{
		log( Level.SEVERE, msg );
	}
	
	public void log( Level l, String client, String msg )
	{
		if ( client.length() < 15 )
		{
			client = client + Strings.repeat( " ", 15 - client.length() );
		}
		
		printHeader();
		
		log( l, "&5" + client + " &a" + msg );
	}
	
	public void log( Level l, String msg, Throwable t )
	{
		log( l, msg );
	}
	
	public void log( Level l, String msg )
	{
		if ( terminal != null && terminal.isAnsiSupported() )
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
	
	public void sendMessage( String message )
	{
		if ( terminal.isAnsiSupported() )
		{
			String result = ChatColor.translateAlternateColorCodes( '&', message );
			for ( ChatColor color : colors )
			{
				if ( replacements.containsKey( color ) )
				{
					result = result.replaceAll( "(?i)" + color.toString(), replacements.get( color ) );
				}
				else
				{
					result = result.replaceAll( "(?i)" + color.toString(), "" );
				}
			}
			log( Level.INFO, result + Ansi.ansi().reset().toString() );
		}
		else
		{
			sendRawMessage( message );
		}
	}
	
	public void sendRawMessage( String message )
	{
		log( Level.ALL, ChatColor.stripColor( message ) );
	}
	
	public void sendMessage( String[] messages )
	{
		for ( String message : messages )
		{
			sendMessage( message );
		}
	}
	
	public void panic( Throwable e )
	{
		e.printStackTrace();
		panic( e.getMessage() );
	}
	
	public void panic( String msg )
	{
		getLogger().severe( msg );
		System.exit( 1 );
	}
	
	public ConsoleReader getReader()
	{
		return reader;
	}
	
	public ConsoleCommandSender getConsoleSender()
	{
		return console;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
}
