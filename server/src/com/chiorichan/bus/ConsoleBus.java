package com.chiorichan.bus;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import jline.Terminal;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;

import com.chiorichan.ChatColor;
import com.chiorichan.ConsoleLogManager;
import com.chiorichan.Loader;
import com.chiorichan.LoggerOutputStream;
import com.chiorichan.ServerShutdownThread;
import com.chiorichan.ThreadCommandReader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.bus.events.server.CommandIssuedEvent;
import com.chiorichan.command.CommandRef;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;

public class ConsoleBus implements Runnable
{
	private ConsoleLogManager logManager;
	
	private final List<CommandRef> commandList = Collections.synchronizedList( new ArrayList<CommandRef>() );
	
	public ConsoleReader reader;
	public Terminal terminal;
	private OptionSet options;
	private ThreadCommandReader consoleReader;
	
	public static int lastFiveTick = -1;
	public static int currentTick = (int) ( System.currentTimeMillis() / 50 );
	public Thread primaryThread;
	
	public boolean useJline = true;
	public boolean useConsole = true;
	public boolean useColors = true;
	
	public Loader loader;
	
	public void init( Loader _loader, OptionSet _options )
	{
		loader = _loader;
		options = _options;
		
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
			System.out.println( "Console input is disabled due to --noconsole command argument" );
			useConsole = false;
		}
		
		if ( options.has( "nocolor" ) )
		{
			useColors = false;
		}
		
		try
		{
			reader = new ConsoleReader( System.in, System.out );
			reader.setExpandEvents( false ); // Avoid parsing exceptions for uncommonly used event designators
		}
		catch ( Exception e )
		{
			try
			{
				// Try again with jline disabled for Windows users without C++ 2008 Redistributable
				System.setProperty( "jline.terminal", "jline.UnsupportedTerminal" );
				System.setProperty( "user.language", "en" );
				reader = new ConsoleReader( System.in, System.out );
				reader.setExpandEvents( false );
			}
			catch ( Exception ex )
			{
				ex.printStackTrace();
			}
		}
		
		reader.setPrompt( "?> " );
		terminal = reader.getTerminal();
		
		logManager = new ConsoleLogManager( "" );
		logManager.init();
		
		// TODO: Add alt color handling for System OUT and ERR.
		System.setOut( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.INFO ), true ) );
		System.setErr( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.SEVERE ), true ) );
		
		Runtime.getRuntime().addShutdownHook( new ServerShutdownThread( loader ) );
		
		primaryThread = new Thread( this, "Server Thread" );
	}
	
	@Override
	public void run()
	{
		try
		{
			long i = System.currentTimeMillis();
			
			Boolean g = false;
			long Q = 0;
			
			for ( long j = 0L; Loader.isRunning(); g = true )
			{
				long k = System.currentTimeMillis();
				long l = k - i;
				
				if ( l > 2000L && i - Q >= 15000L )
				{
					if ( loader.getWarnOnOverload() )
						getLogger().warning( "Can\'t keep up! Did the system time change, or is the server overloaded?" );
					l = 2000L;
					Q = i;
				}
				
				if ( l < 0L )
				{
					getLogger().warning( "Time ran backwards! Did the system time change?" );
					l = 0L;
				}
				
				j += l;
				i = k;
				while ( j > 50L )
				{
					currentTick = (int) ( System.currentTimeMillis() / 50 );
					j -= 50L;
					loopTick( currentTick );
				}
				
				Thread.sleep( 1L );
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			// Crash report generate here
		}
		finally
		{
			try
			{
				Loader.shutdown();
			}
			catch ( Throwable throwable1 )
			{
				throwable1.printStackTrace();
			}
			finally
			{
				try
				{
					reader.getTerminal().reset();
					;
				}
				catch ( Exception e )
				{}
			}
		}
	}
	
	private void loopTick( int tick )
	{
		handleCommands();
		
		Loader.getScheduler().mainThreadHeartbeat( tick );
		
		// Execute every five minutes - ex: clean sessions and checking for updates.
		int fiveMinuteTick = new DateTime().getMinuteOfHour();
		if ( fiveMinuteTick % 5 == 0 && lastFiveTick != fiveMinuteTick )
		{
			lastFiveTick = fiveMinuteTick;
			
			if ( fiveMinuteTick % Loader.getConfig().getInt( "sessions.cleanupInterval", 5 ) == 0 )
				PersistenceManager.mainThreadHeartbeat( tick );
			
			if ( fiveMinuteTick % Loader.getConfig().getInt( "auto-updater.check-interval", 30 ) == 0 )
				Loader.getAutoUpdater().check();
		}
	}
	
	public ConsoleLogManager getLogger()
	{
		return logManager;
	}
	
	public void issueCommand( SentientHandler sender, String command )
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( command, "CommandLine cannot be null" );
		
		commandList.add( new CommandRef( sender, command ) );
	}
	
	public void handleCommands()
	{
		while ( !commandList.isEmpty() )
		{
			CommandRef command = (CommandRef) commandList.remove( 0 );
			
			try
			{
				CommandIssuedEvent event = new CommandIssuedEvent( command.command, command.sender );
				Loader.getEventBus().callEvent( event );
				
				if ( PluginManager.getCommandMap().dispatch( command.sender, event.getCommand() ) )
					return;
				
				command.sender.sendMessage( "The command was unrecognized. Type \"help\" for help." );
			}
			catch ( Exception ex )
			{
				getLogger().log( Level.WARNING, "Unexpected exception while parsing console command \"" + command.command + '"', ex );
			}
		}
	}
	
	public ConsoleReader getReader()
	{
		return reader;
	}
	
	public boolean isPrimaryThread()
	{
		return Thread.currentThread().equals( primaryThread );
	}
	
	public boolean AnsiSupported()
	{
		return ( terminal != null && terminal.isAnsiSupported() );
	}
	
	public void startConsolePrompt()
	{
		consoleReader = new ThreadCommandReader( this );
		
		consoleReader.setDaemon( true );
		consoleReader.setName( "Console Reader Thread" );
		consoleReader.start();
		
		// Allow users to be attached to console at startup without password too.
		// reader.attachAccount( SystemAccounts.NO_LOGIN );
	}
	
	public void showBanner()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
				
				String[] banner = new String( FileUtil.inputStream2Bytes( is ) ).split( "\\n" );
				
				for ( String l : banner )
				{
					getLogger().info( ChatColor.GOLD + l );
				}
				
				getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Starting " + Versioning.getProduct() + " Version " + Versioning.getVersion() );
				getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + Versioning.getCopyright() );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{	
			
		}
	}
	
	public ThreadCommandReader getConsoleReader()
	{
		return consoleReader;
	}
}
