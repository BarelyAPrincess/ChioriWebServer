package com.chiorichan;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.lang3.Validate;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import sun.misc.IOUtils;

import com.chiorichan.Warning.WarningState;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandMap;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.PluginCommand;
import com.chiorichan.command.ServerCommand;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.conversations.Conversable;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.http.WebHandler;
import com.chiorichan.net.Packet;
import com.chiorichan.net.PacketListener;
import com.chiorichan.permissions.Permissible;
import com.chiorichan.permissions.Permission;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginLoadOrder;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.ServicesManager;
import com.chiorichan.plugin.SimplePluginManager;
import com.chiorichan.plugin.SimpleServicesManager;
import com.chiorichan.plugin.java.JavaPluginLoader;
import com.chiorichan.plugin.messaging.Messenger;
import com.chiorichan.plugin.messaging.PluginMessageRecipient;
import com.chiorichan.plugin.messaging.StandardMessenger;
import com.chiorichan.scheduler.ChioriScheduler;
import com.chiorichan.scheduler.ChioriWorker;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.ChioriDLUpdaterService;
import com.chiorichan.user.BanEntry;
import com.chiorichan.user.User;
import com.chiorichan.user.UserManager;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebUtils;
import com.chiorichan.util.permissions.DefaultPermissions;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.collect.ImmutableList;
import com.sun.net.httpserver.HttpServer;

public class Loader implements PluginMessageRecipient
{
	public static final String BROADCAST_CHANNEL_ADMINISTRATIVE = "chiori.broadcast.admin";
	public static final String BROADCAST_CHANNEL_USERS = "chiori.broadcast.user";
	
	private static boolean isClientMode = false;
	
	private final AutoUpdater updater;
	private final Yaml yaml = new Yaml( new SafeConstructor() );
	private static YamlConfiguration configuration;
	private static Loader instance;
	private static OptionSet options;
	private static long startTime = System.currentTimeMillis();
	
	public static String webroot = "";
	private WarningState warningState = WarningState.DEFAULT;
	private final CommandMap commandMap = new CommandMap();
	private final PluginManager pluginManager = new SimplePluginManager( this, commandMap );
	private final StandardMessenger messenger = new StandardMessenger();
	protected final static Console console = new Console();
	protected UserManager userManager;
	
	protected PersistenceManager persistence;
	
	private final ServicesManager servicesManager = new SimpleServicesManager();
	private final static ChioriScheduler scheduler = new ChioriScheduler();
	private static HttpServer httpServer;
	private static Server tcpServer;
	public static Boolean isRunning = false;
	public static String clientId;
	private PluginLoadOrder currentState = PluginLoadOrder.INITIALIZATION;
	
	private String remoteTcpIp = null;
	private Integer remoteTcpPort = -1;
	
	public static void main( String... args ) throws Exception
	{
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
			{
				try
				{
					parser.printHelpOn( System.out );
				}
				catch ( IOException ex )
				{
					Logger.getLogger( Loader.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
			else if ( options.has( "v" ) )
			{
				System.out.println( Versioning.getVersion() );
			}
			else
			{
				boolean clientMode = options.has( "client" );
				
				if ( ( options.has( "client-ip" ) || options.has( "client-port" ) ) && !clientMode )
				{
					System.err.println( "You must also define --client (Puts application is client mode) if you want to use the --client-ip or --client-port arguments." );
				}
				else
					isRunning = new Loader( options, clientMode ).start();
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
				httpServer.stop( 0 );
				tcpServer.stop();
				console.isRunning = false;
				
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
				
				acceptsAll( Arrays.asList( "client" ), "Runs the application in Client Mode" );
				
				acceptsAll( Arrays.asList( "client-ip" ), "Host for the remote server" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
				
				acceptsAll( Arrays.asList( "client-port" ), "Port for the remote server" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
				
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
				
				acceptsAll( Arrays.asList( "nocolor" ), "Disables the console color formatting" );
				
				acceptsAll( Arrays.asList( "v", "version" ), "Show the Version" );
			}
		};
		
		return parser;
	}
	
	public Loader(OptionSet options0) throws StartupException
	{
		this( options0, false );
	}
	
	public Loader(OptionSet options0, boolean clientMode) throws StartupException
	{
		instance = this;
		options = options0;
		isClientMode = clientMode;
		
		String internalConfigFile = ( isClientMode ) ? "com/chiorichan/config-client.yaml" : "com/chiorichan/config-server.yaml";
		
		console.init( this, options );
		
		if ( !isClientMode )
			showBanner();
		
		if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
			getLogger().warning( "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar server.jar\"" );
		
		if ( getConfigFile() == null )
			getLogger().panic( "We had problems loading the configuration file! Did you define the --config argument?" );
		
		try
		{
			configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		}
		catch ( Exception e )
		{
			try
			{
				FileUtil.copy( new File( getClass().getClassLoader().getResource( internalConfigFile ).toURI() ), getConfigFile() );
				configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
			}
			catch ( URISyntaxException e1 )
			{}
		}
		
		configuration.options().copyDefaults( true );
		configuration.setDefaults( YamlConfiguration.loadConfiguration( getClass().getClassLoader().getResourceAsStream( internalConfigFile ) ) );
		clientId = configuration.getString( "server.installationUID", clientId );
		
		if ( clientId == null || clientId.isEmpty() || clientId.equalsIgnoreCase( "null" ) )
		{
			clientId = UUID.randomUUID().toString();
			configuration.set( "server.installationUID", clientId );
		}
		
		saveConfig();
		
		remoteTcpIp = configuration.getString( "client.remoteTcpHost", null );
		remoteTcpPort = configuration.getInt( "client.remoteTcpHost", 1024 );
		
		if ( options.has( "client-ip" ) )
			remoteTcpIp = (String) options.valueOf( "client-ip" );
		
		if ( options.has( "client-port" ) )
			remoteTcpPort = (Integer) options.valueOf( "client-port" );
		
		if ( remoteTcpIp == null || remoteTcpIp.isEmpty() || remoteTcpPort < 1 )
		{
			throw new StartupException( "The remote Host/IP and/or Port are missconfigured, Please define them in the local config file or use --client-ip and/or --client-port arguments." );
		}
		
		( (SimplePluginManager) pluginManager ).useTimings( configuration.getBoolean( "settings.plugin-profiling" ) );
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		
		if ( !isClientMode )
			webroot = configuration.getString( "settings.webroot" );
		
		updater = new AutoUpdater( new ChioriDLUpdaterService( configuration.getString( "auto-updater.host" ) ), configuration.getString( "auto-updater.preferred-channel" ) );
		if ( !isClientMode )
		{
			updater.setEnabled( configuration.getBoolean( "auto-updater.enabled" ) );
			updater.setSuggestChannels( configuration.getBoolean( "auto-updater.suggest-channels" ) );
			updater.getOnBroken().addAll( configuration.getStringList( "auto-updater.on-broken" ) );
			updater.getOnUpdate().addAll( configuration.getStringList( "auto-updater.on-update" ) );
			// TODO Get updater working for client side. Idea: Only make available so client can automatically match the software version the server is running.
		}
		else
			updater.setEnabled( false );
		
		WebUtils.sendTracking( ( ( isClientMode ) ? "startClient" : "startServer" ), "start", Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")" );
	}
	
	public boolean start()
	{
		loadPlugins();
		enablePlugins( PluginLoadOrder.INITIALIZATION );
		
		if ( !isClientMode )
		{
			File root = new File( webroot );
			
			if ( !root.exists() )
				root.mkdirs();
		}
		
		enablePlugins( PluginLoadOrder.STARTUP );
		
		if ( !isClientMode )
		{
			if ( !options.has( "tcp-disable" ) && configuration.getBoolean( "server.enableTcpServer", true ) )
				initTcpServer();
			else
				getLogger().warning( "The integrated tcp server has been disabled per the configuration. Change server.enableTcpServer to true to reenable it." );
			
			if ( !options.has( "web-disable" ) && configuration.getBoolean( "server.enableWebServer", true ) )
				initWebServer();
			else
				getLogger().warning( "The integrated web server has been disabled per the configuration. Change server.enableWebServer to true to reenable it." );
			
			enablePlugins( PluginLoadOrder.POSTSERVER );
			
			getLogger().info( "Initalizing the Persistence Manager..." );
			
			persistence = new PersistenceManager();
			persistence.getSiteManager().loadSites();
			
			getLogger().info( "Initalizing the User Manager..." );
			userManager = new UserManager( this );
			
			persistence.loadSessions();
		}
		else
		{
			// TODO Make connection to remote server
			
			enablePlugins( PluginLoadOrder.POSTCLIENT );
		}
		
		enablePlugins( PluginLoadOrder.INITIALIZED );
		
		console.primaryThread.start();
		
		getLogger().info( ChatColor.DARK_AQUA + "" + ChatColor.NEGATIVE + "Done (" + ( System.currentTimeMillis() - startTime ) + "ms)! For help, type \"help\" or \"?\"" );
		
		enablePlugins( PluginLoadOrder.RUNNING );
		
		updater.check();
		
		return true;
	}
	
	private static void showBanner()
	{
		try
		{
			InputStream is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
			String[] banner = new String( IOUtils.readFully( is, is.available(), true ) ).split( "\\n" );
			
			for ( String l : banner )
				Loader.getConsole().sendMessage( ChatColor.GOLD + l );
			
			getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Starting " + Versioning.getProduct() + " Version " + Versioning.getVersion() );
			getLogger().info( ChatColor.NEGATIVE + "" + ChatColor.GOLD + Versioning.getCopyright() );
		}
		catch ( Exception e )
		{}
	}
	
	private boolean initTcpServer()
	{
		try
		{
			InetSocketAddress socket;
			String serverIp = configuration.getString( "server.tcpHost", "" );
			int serverPort = configuration.getInt( "server.tcpPort", 80 );
			
			// If there was no tcp host specified then attempt to use the same one as the http server.
			if ( serverIp.isEmpty() )
				serverIp = configuration.getString( "server.httpHost", "" );
			
			if ( serverIp.isEmpty() )
				socket = new InetSocketAddress( serverPort );
			else
				socket = new InetSocketAddress( serverIp, serverPort );
			
			tcpServer = new Server();
			
			getLogger().info( "Starting Tcp Server on " + ( serverIp.length() == 0 ? "*" : serverIp ) + ":" + serverPort );
			
			tcpServer.start();
			tcpServer.bind( socket, null );
			
			tcpServer.addListener( new ThreadedListener( new PacketListener( tcpServer.getKryo() ), Executors.newFixedThreadPool( 3 ) ) );
		}
		catch ( IOException ioexception )
		{
			getLogger().warning( "**** FAILED TO BIND TCP SERVER TO PORT!" );
			getLogger().warning( "The exception was: {0}", new Object[] { ioexception.toString() } );
			getLogger().warning( "Perhaps a server is already running on that port?" );
			return false;
		}
		
		return true;
	}
	
	public static boolean registerPacket( Class<? extends Packet> packet )
	{
		if ( tcpServer != null )
		{
			tcpServer.getKryo().register( packet );
			return true;
		}
		else
			return false;
	}
	
	private boolean initWebServer()
	{
		boolean isRunning = false;
		
		try
		{
			InetSocketAddress socket;
			String serverIp = configuration.getString( "server.httpHost", "" );
			int serverPort = configuration.getInt( "server.httpPort", 80 );
			
			if ( serverIp.isEmpty() )
				socket = new InetSocketAddress( serverPort );
			else
				socket = new InetSocketAddress( serverIp, serverPort );
			
			httpServer = HttpServer.create( socket, 0 );
			
			httpServer.setExecutor( null );
			httpServer.createContext( "/", new WebHandler() );
			
			// TODO: Add SSL support ONEDAY!
			
			getLogger().info( "Starting Web Server on " + ( serverIp.length() == 0 ? "*" : serverIp ) + ":" + serverPort );
			
			try
			{
				httpServer.start();
			}
			catch ( NullPointerException e )
			{
				getLogger().severe( "There was a problem starting the Web Server. Check logs and try again.", e );
				System.exit( 1 );
			}
			catch ( Throwable e )
			{
				getLogger().warning( "**** FAILED TO BIND WEB SERVER TO PORT!" );
				getLogger().warning( "The exception was: {0}", new Object[] { e.toString() } );
				getLogger().warning( "Perhaps a server is already running on that port?" );
			}
			
			isRunning = true;
		}
		catch ( Throwable e )
		{
			getLogger().panic( e );
		}
		
		return isRunning;
	}
	
	public static YamlConfiguration getConfig()
	{
		return configuration;
	}
	
	public void loadPlugins()
	{
		pluginManager.registerInterface( JavaPluginLoader.class );
		
		File pluginFolder = (File) options.valueOf( "plugins" );
		
		if ( pluginFolder.exists() )
		{
			Plugin[] plugins = pluginManager.loadPlugins( pluginFolder );
			for ( Plugin plugin : plugins )
			{
				try
				{
					String message = String.format( "Loading %s", plugin.getDescription().getFullName() );
					plugin.getLogger().info( message );
					plugin.onLoad();
				}
				catch ( Throwable ex )
				{
					getLogger().log( Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
				}
			}
		}
		else
		{
			pluginFolder.mkdir();
		}
		
		// pluginManager.loadInternalPlugin( Template.class.getResourceAsStream( "template.yml" ) );
	}
	
	public void enablePlugins( PluginLoadOrder type )
	{
		currentState = type;
		
		Plugin[] plugins = pluginManager.getPlugins();
		
		for ( Plugin plugin : plugins )
		{
			if ( ( !plugin.isEnabled() ) && ( plugin.getDescription().getLoad() == type ) )
			{
				loadPlugin( plugin );
			}
		}
		
		if ( type == PluginLoadOrder.POSTSERVER )
		{
			commandMap.registerServerAliases();
			loadCustomPermissions();
			DefaultPermissions.registerCorePermissions();
		}
	}
	
	public void disablePlugins()
	{
		pluginManager.disablePlugins();
	}
	
	private void loadPlugin( Plugin plugin )
	{
		try
		{
			pluginManager.enablePlugin( plugin );
			
			List<Permission> perms = plugin.getDescription().getPermissions();
			
			for ( Permission perm : perms )
			{
				try
				{
					pluginManager.addPermission( perm );
				}
				catch ( IllegalArgumentException ex )
				{
					getLogger().log( Level.WARNING, "Plugin " + plugin.getDescription().getFullName() + " tried to register permission '" + perm.getName() + "' but it's already registered", ex );
				}
			}
		}
		catch ( Throwable ex )
		{
			getLogger().log( Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
		}
	}
	
	public User[] getOnlineUsers()
	{
		List<User> online = userManager.users;
		User[] Users = new User[online.size()];
		
		for ( int i = 0; i < Users.length; i++ )
		{
			Users[i] = online.get( i );
		}
		
		return Users;
	}
	
	public User getUser( final String name )
	{
		Validate.notNull( name, "Name cannot be null" );
		
		User[] Users = getOnlineUsers();
		
		User found = null;
		String lowerName = name.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for ( User User : Users )
		{
			if ( User.getName().toLowerCase().startsWith( lowerName ) )
			{
				int curDelta = User.getName().length() - lowerName.length();
				if ( curDelta < delta )
				{
					found = User;
					delta = curDelta;
				}
				if ( curDelta == 0 )
					break;
			}
		}
		return found;
	}
	
	public User getUserExact( String name )
	{
		Validate.notNull( name, "Name cannot be null" );
		
		String lname = name.toLowerCase();
		
		for ( User User : getOnlineUsers() )
		{
			if ( User.getName().equalsIgnoreCase( lname ) )
			{
				return User;
			}
		}
		
		return null;
	}
	
	public int broadcastMessage( String message )
	{
		return broadcast( message, BROADCAST_CHANNEL_USERS );
	}
	
	public List<User> matchUser( String partialName )
	{
		Validate.notNull( partialName, "PartialName cannot be null" );
		
		List<User> matchedUsers = new ArrayList<User>();
		
		for ( User iterUser : this.getOnlineUsers() )
		{
			String iterUserName = iterUser.getName();
			
			if ( partialName.equalsIgnoreCase( iterUserName ) )
			{
				// Exact match
				matchedUsers.clear();
				matchedUsers.add( iterUser );
				break;
			}
			if ( iterUserName.toLowerCase().contains( partialName.toLowerCase() ) )
			{
				// Partial match
				matchedUsers.add( iterUser );
			}
		}
		
		return matchedUsers;
	}
	
	public int getMaxUsers()
	{
		return userManager.getMaxUsers();
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
	
	public static PluginManager getPluginManager()
	{
		return Loader.getInstance().pluginManager;
	}
	
	public static Loader getInstance()
	{
		return instance;
	}
	
	public static ChioriScheduler getScheduler()
	{
		return scheduler;
	}
	
	public ServicesManager getServicesManager()
	{
		return servicesManager;
	}
	
	public UserManager getHandle()
	{
		return userManager;
	}
	
	public boolean dispatchServerCommand( CommandSender sender, ServerCommand serverCommand )
	{
		if ( sender instanceof Conversable )
		{
			Conversable conversable = (Conversable) sender;
			
			if ( conversable.isConversing() )
			{
				conversable.acceptConversationInput( serverCommand.command );
				return true;
			}
		}
		try
		{
			return dispatchCommand( sender, serverCommand.command );
		}
		catch ( Exception ex )
		{
			getLogger().log( Level.WARNING, "Unexpected exception while parsing console command \"" + serverCommand.command + '"', ex );
			return false;
		}
	}
	
	public boolean dispatchCommand( CommandSender sender, String commandLine )
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( commandLine, "CommandLine cannot be null" );
		
		if ( commandMap.dispatch( sender, commandLine ) )
		{
			return true;
		}
		
		if ( sender instanceof User )
		{
			sender.sendMessage( "Unknown command. Type \"/help\" for help." );
		}
		else
		{
			sender.sendMessage( "Unknown command. Type \"help\" for help." );
		}
		
		return false;
	}
	
	// TOOD: Reload might need some checking over.
	public void reload()
	{
		configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		
		userManager.getIPBans().load();
		userManager.getNameBans().load();
		
		pluginManager.clearPlugins();
		commandMap.clearCommands();
		
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
			{
				author = plugin.getDescription().getAuthors().get( 0 );
			}
			getLogger().log( Level.SEVERE, String.format( "Nag author: '%s' of '%s' about the following: %s", author, plugin.getDescription().getName(), "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin" ) );
		}
		
		getPersistenceManager().shutdown();
		getUserManager().saveUsers();
		
		getLogger().info( "Reinitalizing the Persistence Manager..." );
		
		persistence = new PersistenceManager();
		persistence.loadSessions();
		persistence.getSiteManager().loadSites();
		
		getLogger().info( "Reinitalizing the User Manager..." );
		userManager = new UserManager( this );
		
		loadPlugins();
		enablePlugins( PluginLoadOrder.STARTUP );
		enablePlugins( PluginLoadOrder.POSTSERVER );
	}
	
	@SuppressWarnings( { "unchecked", "finally" } )
	private void loadCustomPermissions()
	{
		File file = new File( configuration.getString( "settings.permissions-file" ) );
		FileInputStream stream;
		
		try
		{
			stream = new FileInputStream( file );
		}
		catch ( FileNotFoundException ex )
		{
			try
			{
				file.createNewFile();
			}
			finally
			{
				return;
			}
		}
		
		Map<String, Map<String, Object>> perms;
		
		try
		{
			perms = (Map<String, Map<String, Object>>) yaml.load( stream );
		}
		catch ( MarkedYAMLException ex )
		{
			getLogger().log( Level.WARNING, "Server permissions file " + file + " is not valid YAML: " + ex.toString() );
			return;
		}
		catch ( Throwable ex )
		{
			getLogger().log( Level.WARNING, "Server permissions file " + file + " is not valid YAML.", ex );
			return;
		}
		finally
		{
			try
			{
				stream.close();
			}
			catch ( IOException ex )
			{}
		}
		
		if ( perms == null )
		{
			getLogger().log( Level.INFO, "Server permissions file " + file + " is empty, ignoring it" );
			return;
		}
		
		List<Permission> permsList = Permission.loadPermissions( perms, "Permission node '%s' in " + file + " is invalid", Permission.DEFAULT_PERMISSION );
		
		for ( Permission perm : permsList )
		{
			try
			{
				pluginManager.addPermission( perm );
			}
			catch ( IllegalArgumentException ex )
			{
				getLogger().log( Level.SEVERE, "Permission in " + file + " was already defined", ex );
			}
		}
	}
	
	public String toString()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
	
	public ConsoleReader getReader()
	{
		return console.reader;
	}
	
	public PluginCommand getPluginCommand( String name )
	{
		Command command = commandMap.getCommand( name );
		
		if ( command instanceof PluginCommand )
		{
			return (PluginCommand) command;
		}
		else
		{
			return null;
		}
	}
	
	public void saveUsers()
	{
		userManager.saveUsers();
	}
	
	public Map<String, String[]> getCommandAliases()
	{
		ConfigurationSection section = configuration.getConfigurationSection( "aliases" );
		Map<String, String[]> result = new LinkedHashMap<String, String[]>();
		
		if ( section != null )
		{
			for ( String key : section.getKeys( false ) )
			{
				List<String> commands;
				
				if ( section.isList( key ) )
				{
					commands = section.getStringList( key );
				}
				else
				{
					commands = ImmutableList.of( section.getString( key ) );
				}
				
				result.put( key, commands.toArray( new String[commands.size()] ) );
			}
		}
		
		return result;
	}
	
	public String getShutdownMessage()
	{
		return configuration.getString( "settings.shutdown-message" );
	}
	
	public static void gracefullyShutdownServer( String reason )
	{
		if ( !reason.isEmpty() )
		{
			for ( User User : Loader.getInstance().getOnlineUsers() )
			{
				User.kick( reason );
			}
		}
		
		stop();
	}
	
	public static void stop()
	{
		Loader.getConsole().isRunning = false;
	}
	
	public static void unloadServer( String reason )
	{
		getPersistenceManager().shutdown();
		getUserManager().saveUsers();
		
		if ( !reason.isEmpty() )
		{
			for ( User User : Loader.getInstance().getOnlineUsers() )
			{
				User.kick( reason );
			}
		}
		
		instance.pluginManager.clearPlugins();
		instance.commandMap.clearCommands();
		
		httpServer.stop( 0 );
		tcpServer.stop();
	}
	
	public static void shutdown()
	{
		getPersistenceManager().shutdown();
		getUserManager().saveUsers();
		
		getConsole().primaryThread.interrupt();
		
		instance.pluginManager.clearPlugins();
		instance.commandMap.clearCommands();
		
		httpServer.stop( 0 );
		tcpServer.stop();
		
		isRunning = false;
		
		System.exit( 1 );
	}
	
	public int broadcast( String message, String permission )
	{
		int count = 0;
		Set<Permissible> permissibles = getPluginManager().getPermissionSubscriptions( permission );
		
		for ( Permissible permissible : permissibles )
		{
			if ( permissible instanceof CommandSender && permissible.hasPermission( permission ) )
			{
				CommandSender user = (CommandSender) permissible;
				user.sendMessage( message );
				count++;
			}
		}
		
		return count;
	}
	
	public User getOfflineUser( String name )
	{
		return getOfflineUser( name, true );
	}
	
	public User getOfflineUser( String name, boolean search )
	{
		Validate.notNull( name, "Name cannot be null" );
		
		// TOOD: Fix Me
		
		return null;
	}
	
	public Set<String> getIPBans()
	{
		return userManager.getIPBans().getEntries().keySet();
	}
	
	public void banIP( String address )
	{
		Validate.notNull( address, "Address cannot be null." );
		
		BanEntry entry = new BanEntry( address );
		userManager.getIPBans().add( entry );
		userManager.getIPBans().save();
	}
	
	public void unbanIP( String address )
	{
		userManager.getIPBans().remove( address );
		userManager.getIPBans().save();
	}
	
	public Set<User> getBannedUsers()
	{
		Set<User> result = new HashSet<User>();
		
		for ( Object name : userManager.getNameBans().getEntries().keySet() )
		{
			result.add( getOfflineUser( (String) name ) );
		}
		
		return result;
	}
	
	public void setWhitelist( boolean value )
	{
		userManager.hasWhitelist = value;
		configuration.set( "settings.whitelist", value );
	}
	
	public Set<User> getWhitelistedUsers()
	{
		Set<User> result = new LinkedHashSet<User>();
		
		for ( Object name : userManager.getWhitelisted() )
		{
			if ( ( (String) name ).length() == 0 || ( (String) name ).startsWith( "#" ) )
			{
				continue;
			}
			result.add( getOfflineUser( (String) name ) );
		}
		
		return result;
	}
	
	public Set<User> getOperators()
	{
		Set<User> result = new HashSet<User>();
		
		for ( Object name : userManager.getOPs() )
		{
			result.add( getOfflineUser( (String) name ) );
		}
		
		return result;
	}
	
	public void reloadWhitelist()
	{
		userManager.reloadWhitelist();
	}
	
	public ConsoleCommandSender getConsoleSender()
	{
		return console;
	}
	
	public User[] getOfflineUsers()
	{
		// TODO: Fix ME
		
		return null;
	}
	
	public Messenger getMessenger()
	{
		return messenger;
	}
	
	public void sendPluginMessage( Plugin source, String channel, byte[] message )
	{
		StandardMessenger.validatePluginMessage( getMessenger(), source, channel, message );
		
		for ( User User : getOnlineUsers() )
		{
			User.sendPluginMessage( source, channel, message );
		}
	}
	
	public Set<String> getListeningPluginChannels()
	{
		Set<String> result = new HashSet<String>();
		
		for ( User User : getOnlineUsers() )
		{
			result.addAll( User.getListeningPluginChannels() );
		}
		
		return result;
	}
	
	public void onUserLogin( User User )
	{
		if ( ( updater.isEnabled() ) && ( updater.getCurrent() != null ) && ( User.hasPermission( BROADCAST_CHANNEL_ADMINISTRATIVE ) ) )
		{
			if ( ( updater.getCurrent().isBroken() ) && ( updater.getOnBroken().contains( AutoUpdater.WARN_OPERATORS ) ) )
			{
				User.sendMessage( ChatColor.DARK_RED + "The version of Chiori Web Server that this server is running is known to be broken. Please consider updating to the latest version at dl.bukkit.org." );
			}
			else if ( ( updater.isUpdateAvailable() ) && ( updater.getOnUpdate().contains( AutoUpdater.WARN_OPERATORS ) ) )
			{
				User.sendMessage( ChatColor.DARK_PURPLE + "The version of Chiori Web Server that this server is running is out of date. Please consider updating to the latest version at dl.bukkit.org." );
			}
		}
	}
	
	public CommandMap getCommandMap()
	{
		return commandMap;
	}
	
	public boolean isPrimaryThread()
	{
		return true;
	}
	
	public WarningState getWarningState()
	{
		return warningState;
	}
	
	public static Color parseColor( String color )
	{
		Pattern c = Pattern.compile( "rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)" );
		Matcher m = c.matcher( color );
		
		// First try to parse RGB(0,0,0);
		if ( m.matches() )
		{
			return new Color( Integer.valueOf( m.group( 1 ) ), // r
			Integer.valueOf( m.group( 2 ) ), // g
			Integer.valueOf( m.group( 3 ) ) ); // b
		}
		
		try
		{
			Field field = Class.forName( "java.awt.Color" ).getField( color.trim().toUpperCase() );
			return (Color) field.get( null );
		}
		catch ( Exception e )
		{}
		
		try
		{
			return Color.decode( color );
		}
		catch ( Exception e )
		{}
		
		return null;
	}
	
	private File getConfigFile()
	{
		if ( ( (File) options.valueOf( "config" ) ).getName() == "server.yaml" && isClientMode )
			return new File( "client.yaml" );
		
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
	
	public static Console getConsole()
	{
		return console;
	}
	
	public static ConsoleLogManager getLogger()
	{
		return console.getLogger();
	}
	
	public static String getName()
	{
		return Versioning.getProduct();
	}
	
	public static String getVersion()
	{
		return Versioning.getVersion();
	}
	
	public static Server getTcpServer()
	{
		return tcpServer;
	}
	
	public static HttpServer getWebServer()
	{
		return httpServer;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public static OptionSet getOptions()
	{
		return options;
	}
	
	public static UserManager getUserManager()
	{
		return instance.userManager;
	}
	
	public static PersistenceManager getPersistenceManager()
	{
		return getInstance().persistence;
	}
	
	public boolean getWarnOnOverload()
	{
		return configuration.getBoolean( "settings.warn-on-overload" );
	}
	
	public PluginLoadOrder getCurrentLoadState()
	{
		return currentState;
	}
	
	public List<Site> getSites()
	{
		return getPersistenceManager().getSiteManager().getSites();
	}
	
	public Site getSite( String siteName )
	{
		return getPersistenceManager().getSiteManager().getSiteById( siteName );
	}
	
	public AutoUpdater getAutoUpdater()
	{
		return updater;
	}
	
	public static boolean isClientMode()
	{
		return isClientMode;
	}
}
