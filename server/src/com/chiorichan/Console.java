package com.chiorichan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import jline.Terminal;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.RemoteConsoleCommandSender;
import com.chiorichan.command.ServerCommand;
import com.chiorichan.conversations.Conversation;
import com.chiorichan.conversations.ConversationAbandonedEvent;
import com.chiorichan.event.server.ServerCommandEvent;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionAttachment;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;

public class Console implements ConsoleCommandSender, Runnable
{
	private ConsoleLogManager logManager;
	
	private final List<ServerCommand> commandList = Collections.synchronizedList( new ArrayList<ServerCommand>() );
	public RemoteConsoleCommandSender remoteConsole;
	
	public ConsoleReader reader;
	public Terminal terminal;
	public Boolean isRunning = true;
	private OptionSet options;
	
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
		
		Loader.getPluginManager().subscribeToPermission( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE, this );
		Loader.getPluginManager().subscribeToPermission( Loader.BROADCAST_CHANNEL_USERS, this );
		
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
		
		ThreadCommandReader threadcommandreader = new ThreadCommandReader( this );
		
		threadcommandreader.setDaemon( true );
		threadcommandreader.setName( "Console Reader Thread" );
		threadcommandreader.start();
		logManager = new ConsoleLogManager( "" );
		logManager.init();
		
		// TODO: Add alt color handling for System OUT and ERR.
		System.setOut( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.INFO ), true ) );
		System.setErr( new PrintStream( new LoggerOutputStream( getLogger().getLogger(), Level.SEVERE ), true ) );
		
		getLogger().info( "Finished initalizing the server console." );
		
		Runtime.getRuntime().addShutdownHook( new ServerShutdownThread( loader ) );
		
		primaryThread = new Thread( this, "Server thread" );
	}
	
	@Override
	public void run()
	{
		try
		{
			long i = System.currentTimeMillis();
			
			Boolean g = false;
			long Q = 0;
			
			for ( long j = 0L; isRunning; g = true )
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
		catch ( Throwable throwable )
		{
			throwable.printStackTrace();
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
		PersistenceManager.mainThreadHeartbeat( tick );
	}
	
	public ConsoleLogManager getLogger()
	{
		return logManager;
	}
	
	public void issueCommand( String s, CommandSender commandSender )
	{
		commandList.add( new ServerCommand( s, commandSender ) );
	}
	
	public void handleCommands()
	{
		while ( !commandList.isEmpty() )
		{
			ServerCommand servercommand = (ServerCommand) commandList.remove( 0 );
			
			ServerCommandEvent event = new ServerCommandEvent( this, servercommand.command );
			Loader.getPluginManager().callEvent( event );
			servercommand = new ServerCommand( event.getCommand(), servercommand.sender );
			
			Loader.getInstance().dispatchServerCommand( this, servercommand );
		}
	}
	
	public void sendMessage( String message )
	{
		getLogger().info( message );
	}
	
	public void sendRawMessage( String message )
	{
		getLogger().log( Level.INFO, ChatColor.stripColor( message ) );
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
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public boolean isPrimaryThread()
	{
		return Thread.currentThread().equals( primaryThread );
	}
	
	@Override
	public boolean isOp()
	{
		return Loader.getConfig().getBoolean( "framework.users.operators." + getName(), false );
	}
	
	@Override
	public void setOp( boolean value )
	{
		Loader.getConfig().set( "framework.users.operators." + getName(), value );
	}
	
	@Override
	public String getName()
	{
		return "[console]";
	}
	
	@Override
	public boolean isPermissionSet( String name )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isPermissionSet( Permission perm )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean hasPermission( String name )
	{
		getLogger().info( "Console was checked for permission: " + name );
		
		return true;
	}
	
	@Override
	public boolean hasPermission( Permission perm )
	{
		return true;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void removeAttachment( PermissionAttachment attachment )
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void recalculatePermissions()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isConversing()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void acceptConversationInput( String input )
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean beginConversation( Conversation conversation )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void abandonConversation( Conversation conversation )
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void abandonConversation( Conversation conversation, ConversationAbandonedEvent details )
	{
		// TODO Auto-generated method stub
		
	}
	
	public boolean AnsiSupported()
	{
		return ( terminal != null && terminal.isAnsiSupported() );
	}
}
