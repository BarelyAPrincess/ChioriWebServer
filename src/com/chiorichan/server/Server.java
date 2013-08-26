package com.chiorichan.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jline.Terminal;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;

import com.caucho.resin.BeanEmbed;
import com.caucho.resin.FilterMappingEmbed;
import com.caucho.resin.HttpEmbed;
import com.caucho.resin.ResinEmbed;
import com.caucho.resin.WebAppEmbed;
import com.chiorichan.ChatColor;
import com.chiorichan.ConsoleLogManager;
import com.chiorichan.Main;
import com.chiorichan.ThreadCommandReader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.RemoteConsoleCommandSender;
import com.chiorichan.command.ServerCommand;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.user.UserList;
import com.chiorichan.util.ServerShutdownThread;
import com.chiorichan.util.Versioning;

public class Server
{
	private static Logger log = Logger.getLogger( "ChioriWebServer" );
	
	private final List commandList = Collections.synchronizedList( new ArrayList() );
	public ConsoleReader reader;
	public Terminal terminal;
	public ChatColor[] colors = ChatColor.values();
	public Boolean isRunning = false;
	public UserList userList;
	
	public String serverIp = "";
	public OptionSet options;
	public Main server;
	public ConsoleCommandSender console;
	public RemoteConsoleCommandSender remoteConsole;
	public static int currentTick;
	public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
	public int autosavePeriod;
	
	public ConsoleLogManager consoleLogManager = new ConsoleLogManager( "ChioriWebServer" );
	
	private final static ResinEmbed primaryResin = new ResinEmbed();
	
	public Thread primaryThread = Thread.currentThread();
	public ServerThread serverThread = new ServerThread();
	
	public Server()
	{
		
	}
	
	public void init( YamlConfiguration configuration )
	{
		try
		{
			long startTime = System.currentTimeMillis();
			
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
			
			//System.setOut( new PrintStream( new LoggerOutputStream( log, Level.INFO ), true ) );
			//System.setErr( new PrintStream( new LoggerOutputStream( log, Level.SEVERE ), true ) );
			
			Runtime.getRuntime().addShutdownHook( new ServerShutdownThread( this ) );
			
			getLogger().info( "Starting Chiori Web Server " + Versioning.getVersion() );
			
			if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
			{
				getLogger().warning( "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar chiori_server.jar\"" );
			}
			
			getLogger().info( "Loading server configuration" );
			
			serverIp = configuration.getString( "server.ip", "" );
			
			if ( serverIp.length() > 0 )
			{
				setIp( serverIp );
			}
			
			setPort( configuration.getInt( "server.port", 8080 ) );
			
			//ServiceBean service = new ServiceBean();
			
			//primaryResin.addBean( new BeanEmbed( service, "framework" ) );
			
			primaryResin.setServerHeader( "Chiori Web Server Version " + Main.getVersion() );
			primaryResin.setServerId( Main.getConfig().getString( "server.id", "applebloom" ) );
			
			File root = new File( configuration.getString( "settings.webroot", "webroot" ) );
			
			if ( !root.exists() )
				root.mkdirs();
			
			// TODO: Make it so the fw folder is an internal protected folder
			WebAppEmbed webapp = new WebAppEmbed( "/", root.getAbsolutePath() );
			
			webapp.addFilterMapping( new FilterMappingEmbed( "DefaultFilter", "/*", "com.chiorichan.server.DefaultFilter" ) );
			
			primaryResin.addWebApp( webapp );
			
			primaryResin.setDevelopmentMode( true );
			
			this.getLogger().info( "Starting Server on " + ( this.getServerIp().length() == 0 ? "*" : this.getServerIp() ) + ":" + configuration.getInt( "server.port", 8080 ) );
			
			try
			{
				primaryResin.start();
			}
			catch ( Throwable e )
			{
				// e.printStackTrace();
				getLogger().warning( "There was a problem starting the Web Server, Trying to start on the alternate port of 8080!", e );
				
				try
				{
					setPort( 8080 );
					primaryResin.start();
				}
				catch ( Exception ee )
				{
					getLogger().severe( "There was even a problem starting the alternate port of 8080, Goodbye!", ee );
					ee.printStackTrace();
					System.exit( 1 );
				}
			}
			
			userList = new UserList( this );
			
			if ( configuration.getBoolean( "server.enable-query", false ) )
			{
				this.getLogger().info( "Starting GS4 status listener" );
				// this.remoteStatusListener = new RemoteStatusListener( this );
			}
			
			if ( configuration.getBoolean( "server.enable-rcon", false ) )
			{
				getLogger().info( "Starting remote control listener" );
				// remoteControlListener = new RemoteControlListener( this );
				// remoteConsole = new ChioriRemoteConsoleCommandSender();
			}
			
			this.getLogger().info( "Done (" + ( System.currentTimeMillis() - startTime ) + "ms)! For help, type \"help\" or \"?\"" );
			
			isRunning = true;
			serverThread.start();
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
		}
	}
	
	public void setIp( String ip )
	{
		// TODO: Bind server to listening IP Address. Is this possible?
	}
	
	public void setPort( int port )
	{
		if ( !isRunning )
		{
			HttpEmbed http = new HttpEmbed( port );
			HttpEmbed[] http2 = new HttpEmbed[1];
			http2[0] = http;
			primaryResin.setPorts( http2 );
		}
	}
	
	public static ResinEmbed getResinServer()
	{
		return primaryResin;
	}
	
	/*
	 * public void sendMessage( String message ) { if ( terminal.isAnsiSupported() ) { String result =
	 * ChatColor.translateAlternateColorCodes( '&', message ); for ( ChatColor color : colors ) { if (
	 * replacements.containsKey( color ) ) { result = result.replaceAll( "(?i)" + color.toString(), replacements.get(
	 * color ) ); } else { result = result.replaceAll( "(?i)" + color.toString(), "" ); } } System.out.print( result +
	 * Ansi.ansi().reset().toString() ); // log( Level.INFO, result + Ansi.ansi().reset().toString() ); } else {
	 * sendRawMessage( message ); } }
	 * 
	 * public void sendRawMessage( String message ) { System.out.print( ChatColor.stripColor( message ) ); // log(
	 * Level.ALL, ChatColor.stripColor( message ) ); }
	 * 
	 * public void sendMessage( String[] messages ) { for ( String message : messages ) { sendMessage( message ); } }
	 */
	
	public void issueCommand( String s, CommandSender commandSender )
	{
		commandList.add( new ServerCommand( s, commandSender ) );
	}
	
	public ConsoleLogManager getLogger()
	{
		return consoleLogManager;
	}
	
	public UserList getUserList()
	{
		return userList;
	}
	
	public String getServerIp()
	{
		return this.serverIp;
	}
	
	public void sendMessage( String msg )
	{
		
	}
	
	public Map<String, String[]> getCommandAliases()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public void safeShutdown()
	{
		// TODO Auto-generated method stub
		
	}
	
	public void shutdown()
	{
		// TODO Auto-generated method stub
		
	}

	public void setOptions( OptionSet options )
	{
		this.options = options;
	}

	public void registerBean( Class bean, String name )
	{
		primaryResin.addBean( new BeanEmbed( bean, name ) );
	}
}
