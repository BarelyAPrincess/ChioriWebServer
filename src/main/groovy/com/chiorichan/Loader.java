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
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.io.FileUtils;

import com.chiorichan.Warning.WarningState;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.ConsoleBus;
import com.chiorichan.bus.EventBus;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.SiteManager;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.http.session.SessionManager;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.permissions.PermissionsManager;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginLoadOrder;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.ServicesManager;
import com.chiorichan.plugin.messaging.Messenger;
import com.chiorichan.plugin.messaging.StandardMessenger;
import com.chiorichan.scheduler.ChioriScheduler;
import com.chiorichan.scheduler.ChioriWorker;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.ChioriDLUpdaterService;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;

public class Loader
{
	public static final String BROADCAST_CHANNEL_ADMINISTRATIVE = "chiori.broadcast.admin";
	public static final String BROADCAST_CHANNEL_USERS = "chiori.broadcast.user";
	public static final String PATH_SEPERATOR = File.separator;
	
	private static AutoUpdater updater = null;
	private static YamlConfiguration configuration;
	private static Loader instance;
	private static OptionSet options;
	private static long startTime = System.currentTimeMillis();
	
	public static File tmpFileDirectory;
	public static String webroot = "";
	private WarningState warningState = WarningState.DEFAULT;
	
	protected final static ConsoleBus console = new ConsoleBus();
	
	protected static final PluginManager pluginManager = new PluginManager();
	protected static final EventBus events = new EventBus();
	protected static final AccountManager accounts = new AccountManager();
	protected static final SessionManager sessionManager = new SessionManager();
	protected static final SiteManager sites = new SiteManager();
	protected static final PermissionsManager permissions = new PermissionsManager();
	
	private final static StandardMessenger messenger = new StandardMessenger();
	
	private final static ServicesManager servicesManager = new ServicesManager();
	private final static ChioriScheduler scheduler = new ChioriScheduler();
	public static String clientId;
	private static boolean isRunning = true;
	private static String stopReason = null;
	
	protected static DatabaseEngine fwDatabase = null;
	
	public static void main( String... args ) throws Exception
	{
		System.setProperty( "file.encoding", "utf-8" );
		OptionSet options = null;
		
		try
		{
			OptionParser parser = getOptionParser();
			
			try
			{
				options = parser.parse( args );
			}
			catch ( joptsimple.OptionException ex )
			{
				Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, ex.getLocalizedMessage() );
			}
			
			if ( ( options == null ) || ( options.has( "?" ) ) )
				try
				{
					parser.printHelpOn( System.out );
				}
				catch ( IOException ex )
				{
					Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, null, ex );
				}
			else if ( options.has( "v" ) )
				System.out.println( Versioning.getVersion() );
			else
			{
				isRunning = new Loader( options ).start();
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			
			if ( getLogger() != null )
				getLogger().severe( ChatColor.RED + "" + ChatColor.NEGATIVE + "SEVERE ERROR (" + ( System.currentTimeMillis() - startTime ) + "ms)! Press 'Ctrl-c' to quit!'" );
			else
				System.err.println( "SEVERE ERROR (" + ( System.currentTimeMillis() - startTime ) + "ms)! Press 'Ctrl-c' to quit!'" );
			// TODO Make it so this exception (and possibly other critical exceptions) are reported to us without user interaction. Should also find a way that the log can be sent along with it.
			try
			{
				NetworkManager.cleanup();
				isRunning = false;
				
				if ( configuration != null && configuration.getBoolean( "server.haltOnSevereError" ) )
				{
					Scanner keyboard = new Scanner( System.in );
					keyboard.nextLine();
					keyboard.close();
				}
			}
			catch ( Exception e )
			{}
		}
	}
	
	public static OptionParser getOptionParser()
	{
		OptionParser parser = new OptionParser()
		{
			{
				acceptsAll( Arrays.asList( "?", "help" ), "Show the help" );
				
				acceptsAll( Arrays.asList( "c", "config", "b", "settings" ), "File for chiori settings" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "server.yaml" ) ).describedAs( "Yml file" );
				
				acceptsAll( Arrays.asList( "P", "plugins" ), "Plugin directory to use" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "plugins" ) ).describedAs( "Plugin directory" );
				
				acceptsAll( Arrays.asList( "h", "web-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
				
				acceptsAll( Arrays.asList( "p", "web-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
				
				acceptsAll( Arrays.asList( "h", "tcp-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
				
				acceptsAll( Arrays.asList( "p", "tcp-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
				
				acceptsAll( Arrays.asList( "p", "web-disable" ), "Disable the internal Web Server" );
				
				acceptsAll( Arrays.asList( "p", "tcp-disable" ), "Disable the internal TCP Server" );
				
				acceptsAll( Arrays.asList( "s", "size", "max-users" ), "Maximum amount of users" ).withRequiredArg().ofType( Integer.class ).describedAs( "Server size" );
				
				acceptsAll( Arrays.asList( "d", "date-format" ), "Format of the date to display in the console (for log entries)" ).withRequiredArg().ofType( SimpleDateFormat.class ).describedAs( "Log date format" );
				
				acceptsAll( Arrays.asList( "log-pattern" ), "Specfies the log filename pattern" ).withRequiredArg().ofType( String.class ).defaultsTo( "server.log" ).describedAs( "Log filename" );
				
				acceptsAll( Arrays.asList( "log-limit" ), "Limits the maximum size of the log file (0 = unlimited)" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 0 ).describedAs( "Max log size" );
				
				acceptsAll( Arrays.asList( "log-count" ), "Specified how many log files to cycle through" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 1 ).describedAs( "Log count" );
				
				acceptsAll( Arrays.asList( "log-append" ), "Whether to append to the log file" ).withRequiredArg().ofType( Boolean.class ).defaultsTo( true ).describedAs( "Log append" );
				
				acceptsAll( Arrays.asList( "log-strip-color" ), "Strips color codes from log file" );
				
				acceptsAll( Arrays.asList( "nojline" ), "Disables jline and emulates the vanilla console" );
				
				acceptsAll( Arrays.asList( "noconsole" ), "Disables the console" );
				
				acceptsAll( Arrays.asList( "nobanner" ), "Disables the banner" );
				
				acceptsAll( Arrays.asList( "nocolor" ), "Disables the console color formatting" );
				
				acceptsAll( Arrays.asList( "v", "version" ), "Show the Version" );
			}
		};
		
		return parser;
	}
	
	public Loader(OptionSet options0) throws StartupException
	{
		instance = this;
		options = options0;
		boolean firstRun = false;
		
		String internalConfigFile = "com/chiorichan/config.yaml";
		
		console.init( this, options );
		
		if ( !options0.has( "nobanner" ) )
			console.showBanner();
		
		if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
			getLogger().warning( "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar server.jar\"" );
		
		if ( getConfigFile() == null )
			throw new StartupException( "We had problems loading the configuration file! Did you define the --config argument?" );
		
		try
		{
			File contentTypes = new File( "ContentTypes.properties" );
			
			if ( !contentTypes.exists() )
				FileUtils.writeStringToFile( contentTypes, "# Chiori-chan's Web Server Content-Types File which overrides the default internal ones.\n# Syntax: 'ext: mime/type'" );
		}
		catch ( IOException e )
		{
			getLogger().warning( "There was an exception thrown trying to create the 'ContentTypes.properties' file.", e );
		}
		
		try
		{
			File shellOverrides = new File( "InterpreterOverrides.properties" );
			
			if ( !shellOverrides.exists() )
				FileUtils.writeStringToFile( shellOverrides, "# Chiori-chan's Web Server Interpreter Overrides File which overrides the default internal ones.\n# You don't have to add a string if the key and value are the same, hence Convension!\n# Syntax: 'fileExt: shellHandler'" );
		}
		catch ( IOException e )
		{
			getLogger().warning( "There was an exception thrown trying to create the 'InterpreterOverrides.properties' file.", e );
		}
		
		if ( !getConfigFile().exists() )
		{
			try
			{
				FileUtil.putResource( internalConfigFile, getConfigFile() );
				firstRun = true;
			}
			catch ( IOException e1 )
			{
				e1.printStackTrace();
			}
		}
		
		configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		configuration.options().copyDefaults( true );
		configuration.setDefaults( YamlConfiguration.loadConfiguration( getClass().getClassLoader().getResourceAsStream( internalConfigFile ) ) );
		clientId = configuration.getString( "server.installationUID", clientId );
		
		if ( clientId == null || clientId.isEmpty() || clientId.equalsIgnoreCase( "null" ) )
		{
			clientId = UUID.randomUUID().toString();
			configuration.set( "server.installationUID", clientId );
		}
		
		saveConfig();
		
		if ( console.useColors == true )
			console.useColors = Loader.getConfig().getBoolean( "console.color", true );
		
		events.useTimings( configuration.getBoolean( "settings.plugin-profiling" ) );
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		
		webroot = configuration.getString( "settings.webroot" );
		
		tmpFileDirectory = new File( Loader.getConfig().getString( "server.tmpFileDirectory", "tmp" ) );
		
		if ( !tmpFileDirectory.exists() )
			tmpFileDirectory.mkdirs();
		
		if ( !tmpFileDirectory.isDirectory() )
			Loader.getLogger().warning( "The `server.tmpFileDirectory` in config, specifies a directory that is not a directory. File uploads and other similar operations that need the temp directory will fail to function correctly." );
		
		if ( !tmpFileDirectory.canWrite() )
			Loader.getLogger().warning( "The `server.tmpFileDirectory` in config, specifies a directory that is not writable to the server. File uploads and other similar operations that need the temp directory will fail to function correctly." );
		
		updater = new AutoUpdater( new ChioriDLUpdaterService( configuration.getString( "auto-updater.host" ) ), configuration.getString( "auto-updater.preferred-channel" ) );
		
		updater.setEnabled( configuration.getBoolean( "auto-updater.enabled" ) );
		updater.setSuggestChannels( configuration.getBoolean( "auto-updater.suggest-channels" ) );
		updater.getOnBroken().addAll( configuration.getStringList( "auto-updater.on-broken" ) );
		updater.getOnUpdate().addAll( configuration.getStringList( "auto-updater.on-update" ) );
		
		WebUtils.sendTracking( "startServer", "start", Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")" );
		
		if ( firstRun )
		{
			Loader.getLogger().highlight( "It appears that this is your first time running Chiori-chan's Web Server." );
			Loader.getLogger().highlight( "All the needed files have been extracted from the jar file." );
			Loader.getLogger().highlight( "The server will continue to start but it's recommended that you stop, review config and restart." );
			// TODO have the server pause and ask if the user would like to stop as to make changes to config.
		}
	}
	
	public boolean start() throws StartupException
	{
		pluginManager.loadPlugins();
		pluginManager.enablePlugins( PluginLoadOrder.INITIALIZATION );
		
		File root = new File( webroot );
		
		if ( !root.exists() )
			root.mkdirs();
		
		pluginManager.enablePlugins( PluginLoadOrder.STARTUP );
		
		if ( !options.has( "tcp-disable" ) && configuration.getBoolean( "server.enableTcpServer", true ) )
			NetworkManager.initTcpServer();
		else
			getLogger().warning( "The integrated tcp server has been disabled per the configuration. Change server.enableTcpServer to true to reenable it." );
		
		if ( !options.has( "web-disable" ) && configuration.getBoolean( "server.enableWebServer", true ) )
			NetworkManager.initWebServer();
		else
			getLogger().warning( "The integrated web server has been disabled per the configuration. Change server.enableWebServer to true to reenable it." );
		
		pluginManager.enablePlugins( PluginLoadOrder.POSTSERVER );
		
		getLogger().info( "Initalizing the Framework Database..." );
		initDatabase();
		
		getLogger().info( "Initalizing the Site Manager..." );
		sites.init();
		
		getLogger().info( "Initalizing the Accounts Manager..." );
		accounts.init();
		
		getLogger().info( "Initalizing the Session Manager..." );
		sessionManager.init();
		
		pluginManager.enablePlugins( PluginLoadOrder.INITIALIZED );
		
		console.primaryThread.start();
		
		pluginManager.enablePlugins( PluginLoadOrder.RUNNING );
		
		getLogger().info( ChatColor.RED + "" + ChatColor.NEGATIVE + "Done (" + ( System.currentTimeMillis() - startTime ) + "ms)! Type \"help\" for help or \"su\" to change accounts.!" );
		
		updater.check();
		
		return true;
	}
	
	public static DatabaseEngine getDatabase()
	{
		return fwDatabase;
	}
	
	public void initDatabase()
	{
		try
		{
			Class.forName( "com.mysql.jdbc.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
		}
		
		switch ( configuration.getString( "server.database.type", "mysql" ) )
		{
			case "sqlite":
				fwDatabase = new DatabaseEngine();
				String filename = configuration.getString( "server.database.dbfile", "chiori.db" );
				
				try
				{
					fwDatabase.init( filename );
				}
				catch ( SQLException e )
				{
					if ( e.getCause() instanceof ConnectException )
					{
						throw new StartupException( "We had a problem connecting to database '" + filename + "'. Reason: " + e.getCause().getMessage() );
					}
					else
					{
						throw new StartupException( e );
					}
				}
				
				break;
			case "mysql":
				fwDatabase = new DatabaseEngine();
				String host = configuration.getString( "server.database.host", "localhost" );
				String port = configuration.getString( "server.database.port", "3306" );
				String database = configuration.getString( "server.database.database", "chiorifw" );
				String username = configuration.getString( "server.database.username", "fwuser" );
				String password = configuration.getString( "server.database.password", "fwpass" );
				
				try
				{
					fwDatabase.init( database, username, password, host, port );
				}
				catch ( SQLException e )
				{
					// e.printStackTrace();
					
					if ( e.getCause() instanceof ConnectException )
					{
						throw new StartupException( "We had a problem connecting to database '" + host + "'. Reason: " + e.getCause().getMessage() );
					}
					else
					{
						throw new StartupException( e );
					}
				}
				
				break;
			case "none":
			case "":
				Loader.getLogger().warning( "The Framework Database is unconfigured. Some features maybe not function as expected. See config option 'accounts.database.type' in server config file." );
				break;
			default:
				Loader.getLogger().panic( "The Framework Database can not support anything other then mySql or sqLite at the moment. Please change 'framework-database.type' to 'mysql' or 'sqLite' in 'chiori.yml'" );
		}
	}
	
	public static YamlConfiguration getConfig()
	{
		return configuration;
	}
	
	public int getPort()
	{
		return this.getConfigInt( "server-port", 80 );
	}
	
	public String getIp()
	{
		return this.getConfigString( "server-ip", "" );
	}
	
	public String getServerName()
	{
		return this.getConfigString( "server-name", "Unknown Server" );
	}
	
	public String getServerId()
	{
		return this.getConfigString( "server-id", "unnamed" );
	}
	
	public boolean getQueryPlugins()
	{
		return configuration.getBoolean( "settings.query-plugins" );
	}
	
	public boolean hasWhitelist()
	{
		return this.getConfigBoolean( "white-list", false );
	}
	
	private String getConfigString( String variable, String defaultValue )
	{
		return configuration.getString( variable, defaultValue );
	}
	
	private int getConfigInt( String variable, int defaultValue )
	{
		return configuration.getInt( variable, defaultValue );
	}
	
	private boolean getConfigBoolean( String variable, boolean defaultValue )
	{
		return configuration.getBoolean( variable, defaultValue );
	}
	
	public String getUpdateFolder()
	{
		return configuration.getString( "settings.update-folder", "update" );
	}
	
	public File getUpdateFolderFile()
	{
		return new File( (File) options.valueOf( "plugins" ), configuration.getString( "settings.update-folder", "update" ) );
	}
	
	public static Loader getInstance()
	{
		return instance;
	}
	
	public static ChioriScheduler getScheduler()
	{
		return scheduler;
	}
	
	// TOOD: Reload seems to be broken. This needs some serious reworking.
	public void reload()
	{
		configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		
		pluginManager.clearPlugins();
		// ModuleBus.getCommandMap().clearCommands();
		
		int pollCount = 0;
		
		// Wait for at most 2.5 seconds for plugins to close their threads
		while ( pollCount < 50 && getScheduler().getActiveWorkers().size() > 0 )
		{
			try
			{
				Thread.sleep( 50 );
			}
			catch ( InterruptedException e )
			{}
			pollCount++;
		}
		
		List<ChioriWorker> overdueWorkers = getScheduler().getActiveWorkers();
		for ( ChioriWorker worker : overdueWorkers )
		{
			Plugin plugin = worker.getOwner();
			String author = "<NoAuthorGiven>";
			if ( plugin.getDescription().getAuthors().size() > 0 )
				author = plugin.getDescription().getAuthors().get( 0 );
			getLogger().log( Level.SEVERE, String.format( "Nag author: '%s' of '%s' about the following: %s", author, plugin.getDescription().getName(), "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin" ) );
		}
		
		getLogger().info( "Reinitalizing the Persistence Manager..." );
		
		sessionManager.reload();
		
		getLogger().info( "Reinitalizing the Site Manager..." );
		
		sites.reload();
		
		getLogger().info( "Reinitalizing the Accounts Manager..." );
		accounts.reload();
		
		pluginManager.loadPlugins();
		pluginManager.enablePlugins( PluginLoadOrder.RELOAD );
		pluginManager.enablePlugins( PluginLoadOrder.POSTSERVER );
	}
	
	public String toString()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
	
	public String getShutdownMessage()
	{
		return configuration.getString( "settings.shutdown-message" );
	}
	
	public static void gracefullyShutdownServer( String reason )
	{
		if ( !reason.isEmpty() )
			for ( Account User : accounts.getOnlineAccounts() )
			{
				User.kick( reason );
			}
		
		stop( reason );
	}
	
	public static void stop( String _stopReason )
	{
		getLogger().warning( "Server Stopping for Reason: " + _stopReason );
		stopReason = _stopReason;
		isRunning = false;
	}
	
	public static void unloadServer( String reason )
	{
		getSessionManager().shutdown();
		/*
		 * if ( !reason.isEmpty() )
		 * for ( Account User : accounts.getOnlineAccounts() )
		 * {
		 * User.kick( reason );
		 * }
		 */
		getAccountsManager().shutdown();
		NetworkManager.cleanup();
	}
	
	/**
	 * If you wish to shutdown the server, we recommend you use the stop() method instead.
	 */
	public static void shutdown()
	{
		sessionManager.shutdown();
		accounts.shutdown();
		pluginManager.shutdown();
		NetworkManager.cleanup();
		
		isRunning = false;
		
		if ( stopReason != null )
			System.err.println( "The server was stopped for reason: " + stopReason );
		else
		{
			System.err.println( "The server was stopped for an unknown reason" );
			for ( StackTraceElement se : Thread.currentThread().getStackTrace() )
				System.err.println( se );
		}
	}
	
	public static boolean isRunning()
	{
		return isRunning;
	}
	
	public WarningState getWarningState()
	{
		return warningState;
	}
	
	private File getConfigFile()
	{
		return (File) options.valueOf( "config" );
	}
	
	private void saveConfig()
	{
		try
		{
			configuration.save( getConfigFile() );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, "Could not save " + getConfigFile(), ex );
		}
	}
	
	public static ConsoleBus getConsole()
	{
		return console;
	}
	
	public static ConsoleBus getConsoleBus()
	{
		return console;
	}
	
	public static ConsoleLogManager getLogger()
	{
		return console.getLogger();
	}
	
	public static OptionSet getOptions()
	{
		return options;
	}
	
	public boolean getWarnOnOverload()
	{
		return configuration.getBoolean( "settings.warn-on-overload" );
	}
	
	public static SiteManager getSiteManager()
	{
		return sites;
	}
	
	public static AutoUpdater getAutoUpdater()
	{
		return updater;
	}
	
	public static EventBus getEventBus()
	{
		return events;
	}
	
	public static PluginManager getPluginManager()
	{
		return pluginManager;
	}
	
	public static SessionManager getSessionManager()
	{
		return sessionManager;
	}
	
	public static AccountManager getAccountsManager()
	{
		return accounts;
	}
	
	public static File getRoot()
	{
		return new File( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath() ).getParentFile();
	}
	
	public static PermissionsManager getPermissionsManager()
	{
		return permissions;
	}
	
	public static Messenger getMessenger()
	{
		return messenger;
	}
	
	public static ServicesManager getServicesManager()
	{
		return servicesManager;
	}
	
	public static File getTempFileDirectory()
	{
		return tmpFileDirectory;
	}
	
	public static File getWebRoot()
	{
		return new File( webroot );
	}
}
