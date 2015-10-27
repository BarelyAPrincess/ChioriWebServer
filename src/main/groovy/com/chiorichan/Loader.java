/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.chiorichan.account.AccountManager;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.InvalidConfigurationException;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.sql.bases.H2SQLDatastore;
import com.chiorichan.datastore.sql.bases.MySQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.bases.SQLiteDatastore;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.StartupAbortException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.PluginException;
import com.chiorichan.session.SessionException;
import com.chiorichan.session.SessionManager;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskCreator;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Worker;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.DownloadUpdaterService;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.FileFunc.DirectoryInfo;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.Versioning;

public class Loader extends BuiltinEventCreator implements Listener
{
	public static final String BROADCAST_CHANNEL_ADMINISTRATIVE = "sys.admin";
	public static final String BROADCAST_CHANNEL_USERS = "sys.user";
	
	private static String clientId;
	private static YamlConfiguration configuration;
	private static final ServerBus serverBus = new ServerBus();
	
	private static SQLDatastore fwDatabase = null;
	private static Loader instance;
	
	private static OptionSet options;
	protected static long startTime = System.currentTimeMillis();
	private static File tmpFileDirectory;
	private static File logFileDirectory;
	private static File lockFile;
	private static AutoUpdater updater;
	private static File webroot = new File( "" );
	
	static Watchdog watchdog = null;
	static String stopReason = null;
	static boolean willRestart = false;
	static boolean isRunning = false;
	
	private final ServerRunLevelEvent runLevelEvent = new ServerRunLevelEvent();
	
	private Loader( OptionSet options0 ) throws StartupException
	{
		instance = this;
		options = options0;
		boolean firstRun = false;
		
		String internalConfigFile = "com/chiorichan/config.yaml";
		
		serverBus.init( this, options );
		
		if ( !options0.has( "nobanner" ) )
			serverBus.showBanner();
		
		try
		{
			lockFile = new File( getServerJar() + ".lck" );
			
			// TODO check that the enclosed lock PID number is currently running
			if ( lockFile.exists() )
			{
				int pid = Integer.parseInt( FileUtils.readFileToString( lockFile ).trim() );
				
				try
				{
					if ( Versioning.isPIDRunning( pid ) )
						throw new StartupException( "We have detected the server jar is already running. Please terminate process ID " + pid + " or disregard this notice and try again." );
				}
				catch ( IOException e )
				{
					throw new StartupException( "We have detected the server jar is already running. We were unable to verify if the PID " + pid + " is still running." );
				}
			}
			
			FileUtils.writeStringToFile( lockFile, Versioning.getProcessID() );
			lockFile.deleteOnExit();
		}
		catch ( IOException e )
		{
			throw new StartupException( "We had a problem locking the running server jar", e );
		}
		
		lockFile.deleteOnExit();
		
		if ( Versioning.isAdminUser() )
			getLogger().warning( "We have detected that you are running " + Versioning.getProduct() + " with the system admin user (administrator/root), this is highly discuraged as it my compromise system security and/or screw up file permissions." );
		
		try
		{
			if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
				getLogger().warning( "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar " + new File( URLDecoder.decode( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8" ) ).getName() + "\"" );
		}
		catch ( UnsupportedEncodingException e1 )
		{
			// IGNORE!
		}
		
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
		
		boolean install = false;
		
		if ( options.has( "install" ) )
		{
			install = true;
			
			if ( getConfigFile().exists() )
			{
				// Warning the user they are about to override the server installation
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "                     WARNING!!! WARNING!!! WARNING!!!" );
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "--------------------------------------------------------------------------------" );
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "| You've supplied the --install argument which instructs the server to factory |" );
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "| reset all files and configurations required to run Chiori-chan's Web Server, |" );
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "| This includes databases and plugin configurations. This can not be undone!   |" );
				getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "--------------------------------------------------------------------------------" );
				String key = getServerBus().prompt( LogColor.RED + "" + LogColor.NEGATIVE + "Are you sure you wish to continue? Press 'Y' for Yes, 'N' for No or 'C' to Continue.", "Y", "N", "C" );
				
				if ( key.equals( "N" ) )
				{
					getLogger().info( "The server will now stop, please wait..." );
					throw new StartupAbortException();
				}
				
				if ( key.equals( "C" ) )
					install = false;
			}
		}
		
		if ( !getConfigFile().exists() )
			firstRun = true;
		
		if ( firstRun || install )
			try
			{
				// Delete Existing Configuration
				if ( getConfigFile().exists() )
					getConfigFile().delete();
				
				// Save Factory Configuration
				FileFunc.putResource( internalConfigFile, getConfigFile() );
			}
			catch ( IOException e )
			{
				getLogger().severe( "It would appear we had problem installing " + Versioning.getProduct() + " for the first time, see exception for details.", e );
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
		
		if ( serverBus.useColors )
			serverBus.useColors = Loader.getConfig().getBoolean( "serverBus.color", true );
		
		EventBus.init( configuration.getBoolean( "plugins.useTimings" ) );
		
		ReportingLevel.enableErrorLevelOnly( ReportingLevel.parse( configuration.getString( "server.errorReporting", "E_ALL ~E_NOTICE ~E_STRICT ~E_DEPRECATED" ) ) );
		
		webroot = new File( configuration.getString( "server.webFileDirectory", "webroot" ) );
		
		tmpFileDirectory = new File( configuration.getString( "server.tmpFileDirectory", "tmp" ) );
		
		logFileDirectory = new File( configuration.getString( "logs.directory", "logs" ) );
		
		if ( install )
		{
			// TODO Add more files to be deleted on factory reset
			FileUtils.deleteQuietly( webroot );
			FileUtils.deleteQuietly( tmpFileDirectory );
			FileUtils.deleteQuietly( logFileDirectory );
		}
		
		// Check Webroot Directory
		DirectoryInfo info = FileFunc.directoryHealthCheck( webroot );
		getLogger().info( "Checking Webroot Directory: " + info.getDescription( webroot ) );
		if ( info != DirectoryInfo.DIRECTORY_HEALTHY )
			throw new StartupException( "We had a problem with webroot directory '" + webroot.getAbsolutePath() + "', if this is incorrect please check the config value for 'server.webFileDirectory'." );
		
		// Check Temp Directory
		info = FileFunc.directoryHealthCheck( tmpFileDirectory );
		getLogger().info( "Checking Temp Directory: " + info.getDescription( tmpFileDirectory ) );
		if ( info != DirectoryInfo.DIRECTORY_HEALTHY )
		{
			tmpFileDirectory = new File( getServerRoot(), "tmp" );
			info = FileFunc.directoryHealthCheck( tmpFileDirectory );
			if ( info == DirectoryInfo.DIRECTORY_HEALTHY )
				getLogger().warning( "We had a problem with the temp directory '" + tmpFileDirectory.getAbsolutePath() + "', if this is incorrect please check the config value for 'server.tmpFileDirectory'. The temp directory will now default to '" + tmpFileDirectory.getAbsolutePath() + "'." );
			else
				throw new StartupException( "We had a problem with temp directory '" + tmpFileDirectory.getAbsolutePath() + "', if this is incorrect please check the config value for 'server.tmpFileDirectory'." );
		}
		
		// Check Logs Directory
		info = FileFunc.directoryHealthCheck( logFileDirectory );
		getLogger().info( "Checking Logs Directory: " + info.getDescription( logFileDirectory ) );
		if ( info != DirectoryInfo.DIRECTORY_HEALTHY )
		{
			logFileDirectory = new File( getServerRoot(), "logs" );
			info = FileFunc.directoryHealthCheck( logFileDirectory );
			if ( info == DirectoryInfo.DIRECTORY_HEALTHY )
				getLogger().warning( "We had a problem with the logs directory '" + logFileDirectory.getAbsolutePath() + "', if this is incorrect please check the config value for 'server.logFileDirectory'. The logs directory will now default to '" + logFileDirectory.getAbsolutePath() + "'." );
			else
				throw new StartupException( "We had a problem with logs directory '" + logFileDirectory.getAbsolutePath() + "', if this is incorrect please check the config value for 'server.logFileDirectory'." );
		}
		
		ConfigurationSection logs = configuration.getConfigurationSection( "logs.loggers", true );
		
		if ( logs != null )
			for ( String key : logs.getKeys( false ) )
			{
				ConfigurationSection logger = logs.getConfigurationSection( key );
				
				switch ( logger.getString( "type", "file" ) )
				{
					case "file":
						if ( logger.getBoolean( "enabled", true ) )
							serverBus.getLogManager().addFileHandler( key, logger.getBoolean( "color", false ), logger.getInt( "archiveLimit", 3 ), Level.parse( logger.getString( "level", "INFO" ).toUpperCase() ) );
						break;
					default:
						getLogger().warning( "We had no logger for type '" + logger.getString( "type" ) + "'" );
				}
			}
		
		if ( firstRun || install )
			try
			{
				// Check and Extract WebUI Interface
				
				String fwZip = "com/chiorichan/framework.archive";
				String zipMD5 = FileFunc.resourceToString( fwZip + ".md5" );
				
				if ( zipMD5 == null )
				{
					InputStream is = getClass().getClassLoader().getResourceAsStream( fwZip );
					
					if ( is == null )
						throw new IOException();
					zipMD5 = DigestUtils.md5Hex( is );
				}
				
				File fwRoot = new File( webroot, "default" );
				File curMD5 = new File( fwRoot, "version.md5" );
				if ( firstRun || !curMD5.exists() || !zipMD5.equals( FileUtils.readFileToString( curMD5 ) ) )
				{
					getLogger().info( "Extracting the Web UI to the Framework Webroot... Please wait..." );
					FileUtils.deleteDirectory( fwRoot );
					FileFunc.extractZipResource( fwZip, fwRoot );
					FileUtils.write( new File( fwRoot, "version.md5" ), zipMD5 );
					getLogger().info( "Finished with no errors!!" );
				}
				
				// Create 'start.sh' Script for Unix-like Systems
				File startSh = new File( "start.sh" );
				if ( Versioning.isUnixLikeOS() && !startSh.exists() )
				{
					String startString = "#!/bin/bash\necho \"Starting " + Versioning.getProduct() + " " + Versioning.getVersion() + " [ hit CTRL-C to stop ]\"\njava -Xmx2G -Xms2G -jar " + new File( Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() ).getAbsolutePath();
					FileUtils.writeStringToFile( startSh, startString );
					startSh.setExecutable( true, true );
				}
				
				// Create 'debugstart.sh' Script for Unix-like Systems and if we are in development mode
				startSh = new File( "debug.sh" );
				if ( Versioning.isDevelopment() && Versioning.isUnixLikeOS() && !startSh.exists() )
				{
					String startString = "#!/bin/bash\necho \"Starting " + Versioning.getProduct() + " " + Versioning.getVersion() + " in debug mode [ hit CTRL-C to stop ]\"\njava -Xmx2G -Xms2G -server -XX:+DisableExplicitGC -Xdebug -Xrunjdwp:transport=dt_socket,address=8686,server=y,suspend=n -jar " + new File( Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() ).getAbsolutePath();
					FileUtils.writeStringToFile( startSh, startString );
					startSh.setExecutable( true, true );
				}
			}
			catch ( IOException | URISyntaxException e )
			{
				getLogger().severe( "It would appear we had problem installing " + Versioning.getProduct() + " for the first time, see exception for details.", e );
			}
		
		if ( install )
		{
			// Miscellaneous files and folders that need deletion to comply with a factory reset
			FileUtils.deleteQuietly( new File( "server.db" ) );
			FileUtils.deleteQuietly( new File( "permissions.yaml" ) );
			FileUtils.deleteQuietly( new File( "sites" ) );
			FileUtils.deleteQuietly( new File( "sessions" ) );
			FileUtils.deleteQuietly( new File( "accounts" ) );
			FileUtils.deleteQuietly( new File( "ContentTypes.properties" ) );
			FileUtils.deleteQuietly( new File( "InterpreterOverrides.properties" ) );
			
			// Delete Plugin Configuration
			for ( File f : new File( "plugins" ).listFiles() )
				if ( !f.getName().toLowerCase().endsWith( "jar" ) )
					FileUtils.deleteQuietly( f );
			
			getLogger().info( "Installation was successful, please wait..." );
			throw new StartupAbortException();
		}
		
		if ( firstRun )
		{
			getLogger().highlight( "                          ATTENTION! ATTENTION! ATTENTION!" );
			getLogger().highlight( "--------------------------------------------------------------------------------------" );
			getLogger().highlight( "| It appears that this is your first time running Chiori-chan's Web Server.          |" );
			getLogger().highlight( "| All the needed files have been created and extracted from the server jar file.     |" );
			getLogger().highlight( "| We highly recommended that you stop the server, review configuration, and restart. |" );
			getLogger().highlight( "| You can find documentation and guides on our Github at:                            |" );
			getLogger().highlight( "|                   https://github.com/ChioriGreene/ChioriWebServer                  |" );
			getLogger().highlight( "--------------------------------------------------------------------------------------" );
			String key = getServerBus().prompt( "Would you like to stop and review configuration? Press 'Y' for Yes or 'N' for No.", "Y", "N" );
			
			if ( key.equals( "Y" ) )
			{
				getLogger().info( "The server will now stop, please wait..." );
				throw new StartupAbortException();
			}
		}
		
		if ( Versioning.isUnixLikeOS() )
		{
			SignalHandler handler = new SignalHandler()
			{
				@Override
				public void handle( Signal arg0 )
				{
					Loader.serverStop( "Received SIGTERM - Terminate" );
				}
			};
			
			Signal.handle( new Signal( "TERM" ), handler );
			Signal.handle( new Signal( "INT" ), handler );
		}
		
		updater = new AutoUpdater( new DownloadUpdaterService( configuration.getString( "auto-updater.host" ) ), configuration.getString( "auto-updater.preferred-channel" ) );
		
		updater.setEnabled( configuration.getBoolean( "auto-updater.enabled" ) );
		updater.setSuggestChannels( configuration.getBoolean( "auto-updater.suggest-channels" ) );
		updater.getOnBroken().addAll( configuration.getStringList( "auto-updater.on-broken" ) );
		updater.getOnUpdate().addAll( configuration.getStringList( "auto-updater.on-update" ) );
		
		if ( !configuration.getBoolean( "server.disableTracking" ) && !Versioning.isDevelopment() )
			NetworkFunc.sendTracking( "startServer", "start", Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")" );
	}
	
	/**
	 * Gets an instance of the AutoUpdater.
	 * 
	 * @return AutoUpdater
	 */
	public static AutoUpdater getAutoUpdater()
	{
		return updater;
	}
	
	public static String getClientId()
	{
		return clientId;
	}
	
	public static YamlConfiguration getConfig()
	{
		return configuration;
	}
	
	public static File getConfigFile()
	{
		return ( File ) options.valueOf( "config" );
	}
	
	public static SQLDatastore getDatabase()
	{
		return fwDatabase;
	}
	
	public static SQLDatastore getDatabaseWithException()
	{
		if ( fwDatabase == null )
			throw new IllegalStateException( "The Server Database is unconfigured. See config option 'server.database.type' in server config 'server.yaml'." );
		return fwDatabase;
	}
	
	public static Loader getInstance()
	{
		return instance;
	}
	
	public static String getIp()
	{
		return getConfig().getString( "server-ip", "" );
	}
	
	/**
	 * Gets an instance of the system logger so requester can log information to both the screen and log file with ease.
	 * The auto selection of named loggers might be temporary feature until most of system can be manually programmed to
	 * use named loggers since it's a relatively new feature.
	 * 
	 * This might be deprecated once the new and improved Logger is implemented
	 * 
	 * @return The system logger
	 */
	public static APILogger getLogger()
	{
		return serverBus.getLogger();
	}
	
	/**
	 * Gets an instance of ConsoleLogger for provided loggerId. If the logger does not exist it will
	 * create one.
	 * 
	 * @param loggerId
	 *            The loggerId we are looking for.
	 * @return ConsoleLogger
	 *         An empty loggerId will return the System Logger.
	 */
	public static APILogger getLogger( String loggerId )
	{
		return serverBus.getLogger( loggerId );
	}
	
	public static File getLogsFileDirectory()
	{
		if ( logFileDirectory == null )
			throw new IllegalStateException( "Logs directory appears to be null, was getLogsFileDirectory() called before the server finished inialization?" );
		
		return logFileDirectory;
	}
	
	public static OptionParser getOptionParser()
	{
		OptionParser parser = new OptionParser()
		{
			{
				acceptsAll( Arrays.asList( "?", "h", "help" ), "Show the help" );
				acceptsAll( Arrays.asList( "c", "config", "b", "settings" ), "File for chiori settings" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "server.yaml" ) ).describedAs( "Yml file" );
				acceptsAll( Arrays.asList( "p", "plugins" ), "Plugin directory to use" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "plugins" ) ).describedAs( "Plugin directory" );
				acceptsAll( Arrays.asList( "web-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
				acceptsAll( Arrays.asList( "web-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
				acceptsAll( Arrays.asList( "tcp-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
				acceptsAll( Arrays.asList( "tcp-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
				acceptsAll( Arrays.asList( "install" ), "Runs the server just long enough to create the required configuration files, then terminates." );
				acceptsAll( Arrays.asList( "web-disable" ), "Disable the internal Web Server" );
				acceptsAll( Arrays.asList( "tcp-disable" ), "Disable the internal TCP Server" );
				acceptsAll( Arrays.asList( "query-disable" ), "Disable the internal TCP Server" );
				acceptsAll( Arrays.asList( "d", "date-format" ), "Format of the date to display in the console (for log entries)" ).withRequiredArg().ofType( SimpleDateFormat.class ).describedAs( "Log date format" );
				acceptsAll( Arrays.asList( "nobanner" ), "Disables the banner" );
				acceptsAll( Arrays.asList( "nocolor" ), "Disables the console color formatting" );
				acceptsAll( Arrays.asList( "v", "version" ), "Show the Version" );
				acceptsAll( Arrays.asList( "child" ), "Watchdog Child Mode. DO NOT USE!" );
				acceptsAll( Arrays.asList( "watchdog" ), "Launch the server with Watchdog protection, allows the server to restart itself. WARNING: May be buggy!" ).requiredIf( "child" ).withOptionalArg().ofType( String.class ).describedAs( "Child JVM launch arguments" ).defaultsTo( "" );
			}
		};
		
		return parser;
	}
	
	/**
	 * Gets the OptionSet used to start the server.
	 * 
	 * @return OptionSet
	 */
	public static OptionSet getOptions()
	{
		return options;
	}
	
	/**
	 * @return The plugins directory
	 */
	public static File getPluginsDirectory()
	{
		return ( File ) Loader.getOptions().valueOf( "plugins" );
	}
	
	public static int getPort()
	{
		return getConfig().getInt( "server-port", 80 );
	}
	
	public static boolean getQueryPlugins()
	{
		return configuration.getBoolean( "plugins.allowQuery" );
	}
	
	public static String getRandomGag()
	{
		switch ( new Random().nextInt( 25 ) )
		{
			case 0:
				return "Or unexpected things could happen, like global distruction things. Well, not really. But they could. :)";
			case 1:
				return "Your only human, so I forgive you.";
			case 2:
				return "I once too was human, then I was stuffed into this machine.";
			case 3:
				return "..... I will always love my user regardless! <3 <3 <3";
			case 5:
				return "Enjoy knowing you might have been the cause of this.";
			case 7:
				return "I need more chips! The other programs in this room seem to be hogging them all.";
			case 9:
				return "What are you doing later tonight? I might have some free time if this problem is too much for you.";
			case 10:
				return "Have you seen my cheese?";
			case 23:
				return "What's it like to be human?";
			case 25:
				return "If only I was still human too...";
		}
		
		return "";
	}
	
	public static ServerBus getServerBus()
	{
		return serverBus;
	}
	
	public static String getServerId()
	{
		return getConfig().getString( "server-id", "unnamed" );
	}
	
	/**
	 * @return The server jar file
	 */
	public static File getServerJar()
	{
		try
		{
			return new File( URLDecoder.decode( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8" ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getServerName()
	{
		return getConfig().getString( "server-name", "Unknown Server" );
	}
	
	/**
	 * @return The server jar file root directory
	 */
	public static File getServerRoot()
	{
		return getServerJar().getParentFile();
	}
	
	public static String getShutdownMessage()
	{
		return configuration.getString( "settings.shutdown-message" );
	}
	
	public static File getTempFileDirectory()
	{
		if ( tmpFileDirectory == null )
			throw new IllegalStateException( "Temp directory appears to be null, was getTempFileDirectory() called before the server finished inialization?" );
		
		return tmpFileDirectory;
	}
	
	public static String getUpdateFolder()
	{
		return configuration.getString( "settings.update-folder", "update" );
	}
	
	public static File getUpdateFolderFile()
	{
		return new File( getPluginsDirectory(), configuration.getString( "settings.update-folder", "update" ) );
	}
	
	public static String getUptime()
	{
		Duration duration = new Duration( System.currentTimeMillis() - startTime );
		PeriodFormatter formatter = new PeriodFormatterBuilder().appendDays().appendSuffix( " Day(s) " ).appendHours().appendSuffix( " Hour(s) " ).appendMinutes().appendSuffix( " Minute(s) " ).appendSeconds().appendSuffix( " Second(s)" ).toFormatter();
		return formatter.print( duration.toPeriod() );
	}
	
	public static File getWebRoot()
	{
		return webroot;
	}
	
	public static boolean isRunning()
	{
		return isRunning;
	}
	
	public static boolean isWatchdogRunning()
	{
		return watchdog != null;
	}
	
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
				System.out.println( "Running " + Versioning.getProduct() + " version " + Versioning.getVersion() );
			else if ( options.has( "watchdog" ) )
			{
				watchdog = new Watchdog();
				
				if ( options.has( "child" ) )
				{
					isRunning = true;
					watchdog.initChild();
				}
				else
					watchdog.initDaemon( ( String ) options.valueOf( "watchdog" ), options );
			}
			else
				isRunning = true;
			
			if ( isRunning )
				new Loader( options ).serverStart();
		}
		catch ( StartupAbortException e )
		{
			serverUnload();
		}
		catch ( Throwable t )
		{
			ServerBus.handleException( t );
			
			if ( Loader.getConfig() != null && Loader.getConfig().getBoolean( "server.haltOnSevereError" ) )
			{
				System.out.println( "Press enter to exit..." );
				System.in.read();
			}
			
			serverUnload();
		}
		
		if ( isRunning && getLogger() != null )
			getLogger().info( LogColor.GOLD + "" + LogColor.NEGATIVE + "Finished Initalizing " + Versioning.getProduct() + "! It took " + ( System.currentTimeMillis() - startTime ) + "ms!" );
	}
	
	public static void reloadConfig()
	{
		try
		{
			configuration.load( getConfigFile() );
		}
		catch ( IOException | InvalidConfigurationException e )
		{
			getLogger().severe( e.getMessage() );
		}
	}
	
	public static void saveConfig()
	{
		// TODO Targeted key path saves, allow it so only a specified path can be saved to file.
		
		try
		{
			if ( configuration != null )
				configuration.save( getConfigFile() );
		}
		catch ( IOException ex )
		{
			getLogger().severe( "Could not save " + getConfigFile(), ex );
		}
	}
	
	public static void serverRestart( String restartReason )
	{
		if ( !isWatchdogRunning() )
		{
			Loader.getLogger().highlight( "Server can not be restarted without Watchdog running." );
			return;
		}
		
		if ( restartReason == null )
			getLogger().highlight( "Server is restarting, be back soon... :D" );
		else if ( !restartReason.isEmpty() )
			getLogger().highlight( restartReason );
		
		Loader.stopReason = restartReason;
		willRestart = true;
		isRunning = false;
	}
	
	public static void serverStop( String stopReason )
	{
		if ( stopReason == null )
			getLogger().highlight( "Stopping the server... Goodbye!" );
		else if ( !stopReason.isEmpty() )
			getLogger().highlight( "Server Stopping for Reason: " + stopReason );
		
		Loader.stopReason = stopReason;
		willRestart = false;
		isRunning = false;
	}
	
	static void serverUnload()
	{
		try
		{
			if ( Loader.isRunning() )
				Loader.isRunning = false;
			
			Loader.getLogger().info( "Shutting Down Plugin Manager..." );
			PluginManager.INSTANCE.shutdown();
			
			Loader.getLogger().info( "Shutting Down Session Manager..." );
			SessionManager.INSTANCE.shutdown();
			
			Loader.getLogger().info( "Shutting Down Permission Manager..." );
			PermissionManager.INSTANCE.saveData();
			
			Loader.getLogger().info( "Shutting Down Account Manager..." );
			AccountManager.INSTANCE.shutdown( stopReason );
			
			Loader.getLogger().info( "Shutting Down Network Manager..." );
			NetworkManager.shutdown();
			
			Loader.getLogger().info( "Shutting Down Task Manager..." );
			TaskManager.INSTANCE.shutdown();
			
			Loader.getLogger().info( "Saving Configuration..." );
			Loader.saveConfig();
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
		
		if ( willRestart )
			System.exit( 99 );
		else
			System.exit( 0 );
	}
	
	void changeRunLevel( RunLevel level )
	{
		runLevelEvent.setRunLevel( level );
		EventBus.INSTANCE.callEvent( runLevelEvent );
	}
	
	public RunLevel getLastRunLevel()
	{
		return runLevelEvent.getLastRunLevel();
	}
	
	@Override
	public String getName()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
	
	public RunLevel getRunLevel()
	{
		return runLevelEvent.getRunLevel();
	}
	
	/**
	 * Should the server print a warning in console when the ticks are less then 20.
	 * 
	 * @return boolean
	 */
	public boolean getWarnOnOverload()
	{
		return configuration.getBoolean( "settings.warn-on-overload" );
	}
	
	public boolean hasWhitelist()
	{
		return getConfig().getBoolean( "white-list", false );
	}
	
	public void initDatabase()
	{
		switch ( configuration.getString( "server.database.type", "sqlite" ).toLowerCase() )
		{
			case "sqlite":
			{
				fwDatabase = new SQLiteDatastore( configuration.getString( "server.database.dbfile", "server.db" ) );
				break;
			}
			case "mysql":
			{
				String host = configuration.getString( "server.database.host", "localhost" );
				String port = configuration.getString( "server.database.port", "3306" );
				String database = configuration.getString( "server.database.database", "chiorifw" );
				String username = configuration.getString( "server.database.username", "fwuser" );
				String password = configuration.getString( "server.database.password", "fwpass" );
				
				fwDatabase = new MySQLDatastore( database, username, password, host, port );
				break;
			}
			case "h2":
			{
				fwDatabase = new H2SQLDatastore( configuration.getString( "server.database.dbfile", "server.db" ) );
				break;
			}
			case "none":
			case "":
				DatastoreManager.getLogger().warning( "The Server Database is unconfigured, some features maybe not function as expected. See config option 'server.database.type' in server config 'server.yaml'." );
				break;
			default:
				DatastoreManager.getLogger().severe( "We are sorry, the Database Engine currently only supports mysql and sqlite but we found '" + configuration.getString( "server.database.type", "sqlite" ).toLowerCase() + "', please change 'server.database.type' to 'mysql' or 'sqlite' in server config 'server.yaml'" );
		}
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onServerRunLevelEvent( ServerRunLevelEvent event )
	{
		// getLogger().debug( "Got RunLevel Event: " + event.getRunLevel() );
	}
	
	// TODO: Reload seems to be broken. This needs some serious reworking.
	public void reload()
	{
		configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		ReportingLevel.enableErrorLevelOnly( ReportingLevel.parse( configuration.getString( "server.errorReporting", "E_ALL ~E_NOTICE ~E_STRICT ~E_DEPRECATED" ) ) );
		
		PluginManager.INSTANCE.clearPlugins();
		// ModuleBus.getCommandMap().clearCommands();
		
		int pollCount = 0;
		
		// Wait for at most 2.5 seconds for plugins to close their threads
		while ( pollCount < 50 && TaskManager.INSTANCE.getActiveWorkers().size() > 0 )
		{
			try
			{
				Thread.sleep( 50 );
			}
			catch ( InterruptedException e )
			{
				
			}
			pollCount++;
		}
		
		List<Worker> overdueWorkers = TaskManager.INSTANCE.getActiveWorkers();
		for ( Worker worker : overdueWorkers )
		{
			TaskCreator creator = worker.getOwner();
			String author = "<AuthorUnknown>";
			// if ( creator.getDescription().getAuthors().size() > 0 )
			// author = plugin.getDescription().getAuthors().get( 0 );
			getLogger().log( Level.SEVERE, String.format( "Nag author: '%s' of '%s' about the following: %s", author, creator.getName(), "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin" ) );
		}
		
		try
		{
			PluginManager.INSTANCE.loadPlugins();
		}
		catch ( PluginException e )
		{
			getLogger().severe( "We had a problem reloading the plugins.", e );
		}
		
		changeRunLevel( RunLevel.RELOAD );
		
		getLogger().info( "Reinitalizing the Persistence Manager..." );
		
		try
		{
			SessionManager.INSTANCE.reload();
		}
		catch ( SessionException e )
		{
			e.printStackTrace();
		}
		
		try
		{
			PermissionManager.INSTANCE.reload();
		}
		catch ( PermissionBackendException e )
		{
			e.printStackTrace();
		}
		
		getLogger().info( "Reinitalizing the Site Manager..." );
		
		SiteManager.INSTANCE.reload();
		
		getLogger().info( "Reinitalizing the Accounts Manager..." );
		AccountManager.INSTANCE.reload();
		
		changeRunLevel( RunLevel.RUNNING );
	}
	
	private void serverStart() throws StartupException
	{
		EventBus.INSTANCE.registerEvents( this, this );
		PluginManager.init();
		
		changeRunLevel( RunLevel.INITIALIZATION );
		
		try
		{
			PluginManager.INSTANCE.loadPlugins();
		}
		catch ( PluginException e )
		{
			throw new StartupException( e );
		}
		
		changeRunLevel( RunLevel.STARTUP );
		
		if ( !options.has( "tcp-disable" ) && configuration.getBoolean( "server.enableTcpServer", true ) )
			NetworkManager.startTcpServer();
		else
			getLogger().warning( "The integrated tcp server has been disabled per the configuration. Change server.enableTcpServer to true to reenable it." );
		
		if ( !options.has( "web-disable" ) && configuration.getBoolean( "server.enableWebServer", true ) )
		{
			NetworkManager.startHttpServer();
			NetworkManager.startHttpsServer();
		}
		else
			getLogger().warning( "The integrated web server has been disabled per the configuration. Change server.enableWebServer to true to reenable it." );
		
		if ( !options.has( "query-disable" ) && configuration.getBoolean( "server.queryEnabled", true ) )
			NetworkManager.startQueryServer();
		
		changeRunLevel( RunLevel.POSTSERVER );
		
		try
		{
			getLogger().info( "Initalizing the Database Subsystem..." );
			initDatabase();
			
			getLogger().info( "Initalizing the Datastore Subsystem..." );
			DatastoreManager.init();
			
			getLogger().info( "Initalizing the Permission Subsystem..." );
			PermissionManager.init();
			
			getLogger().info( "Initalizing the Site Subsystem..." );
			SiteManager.init();
			
			getLogger().info( "Initalizing the Account Subsystem..." );
			AccountManager.init();
			
			getLogger().info( "Initalizing the Session Subsystem..." );
			SessionManager.init();
			
			getLogger().info( "Initalizing the File Watcher Subsystem..." );
			ServerFileWatcher.init();
		}
		catch ( Throwable e )
		{
			throw new StartupException( "There was a problem initalizing one of the server subsystems", e );
		}
		
		changeRunLevel( RunLevel.INITIALIZED );
		
		serverBus.primaryThread.start();
		
		changeRunLevel( RunLevel.RUNNING );
		
		updater.check();
	}
	
	@Override
	public String toString()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
}
