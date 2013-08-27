package com.chiorichan;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.lang3.Validate;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import com.chiorichan.Warning.WarningState;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandException;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.PluginCommand;
import com.chiorichan.command.ServerCommand;
import com.chiorichan.command.SimpleCommandMap;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.conversations.Conversable;
import com.chiorichan.event.user.UserChatTabCompleteEvent;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.Framework;
import com.chiorichan.help.HelpMap;
import com.chiorichan.help.SimpleHelpMap;
import com.chiorichan.permissions.Permissible;
import com.chiorichan.permissions.Permission;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginLoadOrder;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.ServicesManager;
import com.chiorichan.plugin.SimplePluginManager;
import com.chiorichan.plugin.SimpleServicesManager;
import com.chiorichan.plugin.Template;
import com.chiorichan.plugin.java.JavaPluginLoader;
import com.chiorichan.plugin.messaging.Messenger;
import com.chiorichan.plugin.messaging.PluginMessageRecipient;
import com.chiorichan.plugin.messaging.StandardMessenger;
import com.chiorichan.scheduler.ChioriScheduler;
import com.chiorichan.scheduler.ChioriWorker;
import com.chiorichan.scheduler.IChioriScheduler;
import com.chiorichan.serialization.ConfigurationSerialization;
import com.chiorichan.server.Server;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.ChioriDLUpdaterService;
import com.chiorichan.user.BanEntry;
import com.chiorichan.user.User;
import com.chiorichan.user.UserList;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.StringUtil;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.permissions.DefaultPermissions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;

public class Main implements PluginMessageRecipient
{
	public static final String BROADCAST_CHANNEL_ADMINISTRATIVE = "chiori.broadcast.admin";
	public static final String BROADCAST_CHANNEL_USERS = "chiori.broadcast.user";
	
	public static boolean useJline = true;
	public static boolean useConsole = true;
	
	private final static String serverName = "ChioriWebServer";
	public static String webroot = "";
	private static String serverVersion = "";
	private final String version = Versioning.getVersion();
	private final ServicesManager servicesManager = new SimpleServicesManager();
	private final static IChioriScheduler scheduler = new ChioriScheduler();
	private final SimpleCommandMap commandMap = new SimpleCommandMap( this );;
	private final SimpleHelpMap helpMap = new SimpleHelpMap( this );
	private final StandardMessenger messenger = new StandardMessenger();
	private final PluginManager pluginManager = new SimplePluginManager( this, commandMap );
	protected static Server console = new Server();
	protected UserList userList = new UserList( console );
	private YamlConfiguration configuration;
	private final Yaml yaml = new Yaml( new SafeConstructor() );
	private final Map<String, User> offlineUsers = new MapMaker().softValues().makeMap();
	private final AutoUpdater updater;
	private File container;
	private WarningState warningState = WarningState.DEFAULT;
	private final BooleanWrapper online = new BooleanWrapper();
	
	private static YamlConfiguration config;
	private static Server server;
	private static ResourceLoader resourceLoader;
	private static Main instance;
	public static Map<ChatColor, String> replacements = new EnumMap<ChatColor, String>( ChatColor.class );
	
	private final class BooleanWrapper
	{
		private boolean value = true;
	}
	
	static
	{
		ConfigurationSerialization.registerClass( User.class );
	}
	
	public Main(OptionSet options)
	{
		resourceLoader = ResourceLoader.buildLoader( Main.class.getProtectionDomain().getCodeSource().getLocation().getPath() );
		
		instance = this;
		
		console.setOptions( options );
		
		if ( !Main.useConsole )
		{
			getLogger().info( "Console input is disabled due to --noconsole command argument" );
		}
		
		if ( getConfigFile() == null )
			crashApp( "We had problems loading the configuration file! Did you define the --chiori-settings argument?" );
		
		try
		{
			configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			
			FileUtil.copy( new File( getClass().getClassLoader().getResource( "configurations/chiori.yml" ).toString() ), getConfigFile() );
			configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		}
		
		configuration.options().copyDefaults( true );
		configuration.setDefaults( YamlConfiguration.loadConfiguration( getClass().getClassLoader().getResourceAsStream( "configurations/chiori.yml" ) ) );
		saveConfig();
		( (SimplePluginManager) pluginManager ).useTimings( configuration.getBoolean( "settings.plugin-profiling" ) );
		console.autosavePeriod = configuration.getInt( "seconds-per.autosave" );
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		webroot = configuration.getString( "settings.webroot" );
		serverVersion = Versioning.getVersion();
		config = configuration;
		
		updater = new AutoUpdater( new ChioriDLUpdaterService( configuration.getString( "auto-updater.host" ) ), getLogger(), configuration.getString( "auto-updater.preferred-channel" ) );
		updater.setEnabled( configuration.getBoolean( "auto-updater.enabled" ) );
		updater.setSuggestChannels( configuration.getBoolean( "auto-updater.suggest-channels" ) );
		updater.getOnBroken().addAll( configuration.getStringList( "auto-updater.on-broken" ) );
		updater.getOnUpdate().addAll( configuration.getStringList( "auto-updater.on-update" ) );
		updater.check( serverVersion );
		
		loadPlugins();
		enablePlugins( PluginLoadOrder.STARTUP );
		
		console.init( configuration );
		
		Framework.initalizeFramework();
		
		enablePlugins( PluginLoadOrder.POSTWORLD ); // Temp until there is a better solution for POSTWORLD
	}
	
	private void crashApp( String string )
	{
		getLogger().severe( string );
		System.exit( 1 );
	}

	public static YamlConfiguration getConfig()
	{
		return config;
	}
	
	public static void main( String... args ) throws Exception
	{
		try
		{
			Thread.setDefaultUncaughtExceptionHandler( new ExceptionHandler() );
			
			// Todo: Installation script
			OptionParser parser = new OptionParser()
			{
				{
					acceptsAll( Arrays.asList( "?", "help" ), "Show the help" );
					
					acceptsAll( Arrays.asList( "c", "config" ), "Properties file to use" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "server.properties" ) ).describedAs( "Properties file" );
					
					acceptsAll( Arrays.asList( "P", "plugins" ), "Plugin directory to use" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "plugins" ) ).describedAs( "Plugin directory" );
					
					acceptsAll( Arrays.asList( "h", "host", "server-ip" ), "Host to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
					
					acceptsAll( Arrays.asList( "p", "port", "server-port" ), "Port to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
					
					acceptsAll( Arrays.asList( "s", "size", "max-users" ), "Maximum amount of users" ).withRequiredArg().ofType( Integer.class ).describedAs( "Server size" );
					
					acceptsAll( Arrays.asList( "d", "date-format" ), "Format of the date to display in the console (for log entries)" ).withRequiredArg().ofType( SimpleDateFormat.class ).describedAs( "Log date format" );
					
					acceptsAll( Arrays.asList( "log-pattern" ), "Specfies the log filename pattern" ).withRequiredArg().ofType( String.class ).defaultsTo( "server.log" ).describedAs( "Log filename" );
					
					acceptsAll( Arrays.asList( "log-limit" ), "Limits the maximum size of the log file (0 = unlimited)" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 0 ).describedAs( "Max log size" );
					
					acceptsAll( Arrays.asList( "log-count" ), "Specified how many log files to cycle through" ).withRequiredArg().ofType( Integer.class ).defaultsTo( 1 ).describedAs( "Log count" );
					
					acceptsAll( Arrays.asList( "log-append" ), "Whether to append to the log file" ).withRequiredArg().ofType( Boolean.class ).defaultsTo( true ).describedAs( "Log append" );
					
					acceptsAll( Arrays.asList( "log-strip-color" ), "Strips color codes from log file" );
					
					acceptsAll( Arrays.asList( "b", "chiori-settings" ), "File for chiori settings" ).withRequiredArg().ofType( File.class ).defaultsTo( new File( "chiori.yml" ) ).describedAs( "Yml file" );
					
					acceptsAll( Arrays.asList( "nojline" ), "Disables jline and emulates the vanilla console" );
					
					acceptsAll( Arrays.asList( "noconsole" ), "Disables the console" );
					
					acceptsAll( Arrays.asList( "v", "version" ), "Show the Version" );
				}
			};
			
			OptionSet options = null;
			
			try
			{
				options = parser.parse( args );
			}
			catch ( joptsimple.OptionException ex )
			{
				Logger.getLogger( Main.class.getName() ).log( Level.SEVERE, ex.getLocalizedMessage() );
			}
			
			if ( ( options == null ) || ( options.has( "?" ) ) )
			{
				try
				{
					parser.printHelpOn( System.out );
				}
				catch ( IOException ex )
				{
					Logger.getLogger( Main.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
			else if ( options.has( "v" ) )
			{
				System.out.println( Server.class.getPackage().getImplementationVersion() );
			}
			else
			{
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
					
					// This trick bypasses Maven Shade's clever rewriting of our getProperty call when using String literals
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
						// This ensures the terminal literal will always match the jline implementation
						System.setProperty( jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName() );
					}
					
					if ( options.has( "noconsole" ) )
					{
						useConsole = false;
					}
					
					new Main( options );
				}
				catch ( Throwable t )
				{
					t.printStackTrace();
				}
			}
		}
		catch ( Exception e )
		{	
			
		}
	}
	
	public void loadPlugins()
	{
		pluginManager.registerInterface( JavaPluginLoader.class );
		
		File pluginFolder = (File) console.options.valueOf( "plugins" );
		
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
					Logger.getLogger( Server.class.getName() ).log( Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
				}
			}
		}
		else
		{
			pluginFolder.mkdir();
		}
		
		pluginManager.loadInternalPlugin( Template.class.getResourceAsStream("template.yml" ) );
	}
	
	public void enablePlugins( PluginLoadOrder type )
	{
		if ( type == PluginLoadOrder.STARTUP )
		{
			helpMap.clear();
			helpMap.initializeGeneralTopics();
		}
		
		Plugin[] plugins = pluginManager.getPlugins();
		
		for ( Plugin plugin : plugins )
		{
			if ( ( !plugin.isEnabled() ) && ( plugin.getDescription().getLoad() == type ) )
			{
				loadPlugin( plugin );
			}
		}
		
		if ( type == PluginLoadOrder.POSTWORLD )
		{
			commandMap.registerServerAliases();
			loadCustomPermissions();
			DefaultPermissions.registerCorePermissions();
			helpMap.initializeCommands();
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
			Logger.getLogger( Server.class.getName() ).log( Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
		}
	}
	
	public static String getName()
	{
		return serverName;
	}
	
	public static String getVersion()
	{
		return serverVersion;
	}
	
	@SuppressWarnings( "unchecked" )
	public User[] getOnlineUsers()
	{
		List<User> online = userList.Users;
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
		return userList.getMaxUsers();
	}
	
	// NOTE: These are dependent on the corrisponding call in Server
	// so if that changes this will need to as well
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
		return this.configuration.getBoolean( "settings.query-plugins" );
	}
	
	public boolean hasWhitelist()
	{
		return this.getConfigBoolean( "white-list", false );
	}
	
	// NOTE: Temporary calls through to server.properies until its replaced
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
		return new File( (File) console.options.valueOf( "plugins" ), this.configuration.getString( "settings.update-folder", "update" ) );
	}
	
	public static PluginManager getPluginManager()
	{
		return Main.getInstance().pluginManager;
	}
	
	public static Main getInstance()
	{
		return instance;
	}
	
	public static IChioriScheduler getScheduler()
	{
		return scheduler;
	}
	
	public ServicesManager getServicesManager()
	{
		return servicesManager;
	}
	
	public UserList getHandle()
	{
		return userList;
	}
	
	// NOTE: Should only be called from DedicatedServer.ah()
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
	
	public void reload()
	{
		configuration = YamlConfiguration.loadConfiguration( getConfigFile() );
		
		warningState = WarningState.value( configuration.getString( "settings.deprecated-verbose" ) );
		console.autosavePeriod = configuration.getInt( "ticks-per.autosave" );
		
		userList.getIPBans().load();
		userList.getNameBans().load();
		
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
		loadPlugins();
		enablePlugins( PluginLoadOrder.STARTUP );
		enablePlugins( PluginLoadOrder.POSTWORLD );
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
		return "ChioriWebServer{" + "serverName=" + serverName + ",serverVersion=" + getVersion() + ",frameworkVersion=" + Versioning.getFrameworkVersion() + "}";
	}
	
	public static Server getServer()
	{
		return console;
	}
	
	public static Logger getLogger()
	{
		return console.getLogger().getLogger();
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
		userList.saveUsers();
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
	
	public void shutdown()
	{
		console.safeShutdown();
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
		
		User result = getUserExact( name );
		String lname = name.toLowerCase();
		
		if ( result == null )
		{
			result = offlineUsers.get( lname );
			
			if ( result == null )
			{
				if ( search )
				{	
					
				}
				
				result = new User();
				offlineUsers.put( lname, result );
			}
		}
		else
		{
			offlineUsers.remove( lname );
		}
		
		return result;
	}
	
	@SuppressWarnings( "unchecked" )
	public Set<String> getIPBans()
	{
		return userList.getIPBans().getEntries().keySet();
	}
	
	public void banIP( String address )
	{
		Validate.notNull( address, "Address cannot be null." );
		
		BanEntry entry = new BanEntry( address );
		userList.getIPBans().add( entry );
		userList.getIPBans().save();
	}
	
	public void unbanIP( String address )
	{
		userList.getIPBans().remove( address );
		userList.getIPBans().save();
	}
	
	public Set<User> getBannedUsers()
	{
		Set<User> result = new HashSet<User>();
		
		for ( Object name : userList.getNameBans().getEntries().keySet() )
		{
			result.add( getOfflineUser( (String) name ) );
		}
		
		return result;
	}
	
	public void setWhitelist( boolean value )
	{
		userList.hasWhitelist = value;
		configuration.set( "settings.whitelist", value );
	}
	
	public Set<User> getWhitelistedUsers()
	{
		Set<User> result = new LinkedHashSet<User>();
		
		for ( Object name : userList.getWhitelisted() )
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
		
		for ( Object name : userList.getOPs() )
		{
			result.add( getOfflineUser( (String) name ) );
		}
		
		return result;
	}
	
	public void reloadWhitelist()
	{
		userList.reloadWhitelist();
	}
	
	public ConsoleCommandSender getConsoleSender()
	{
		return console.console;
	}
	
	public User[] getOfflineUsers()
	{
		// WorldNBTStorage storage = (WorldNBTStorage) console.worlds.get( 0 ).getDataManager();
		// String[] files = storage.getUserDir().list( new DatFileFilter() );
		Set<User> Users = new HashSet<User>();
		
		// for ( String file : files )
		// {
		// Users.add( getOfflineUser( file.substring( 0, file.length() - 4 ), false ) );
		// }
		Users.addAll( Arrays.asList( getOnlineUsers() ) );
		
		return Users.toArray( new User[Users.size()] );
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
	
	public void onUserJoin( User User )
	{
		if ( ( updater.isEnabled() ) && ( updater.getCurrent() != null ) && ( User.hasPermission( BROADCAST_CHANNEL_ADMINISTRATIVE ) ) )
		{
			if ( ( updater.getCurrent().isBroken() ) && ( updater.getOnBroken().contains( AutoUpdater.WARN_OPERATORS ) ) )
			{
				User.sendMessage( ChatColor.DARK_RED + "The version of CraftBukkit that this server is running is known to be broken. Please consider updating to the latest version at dl.bukkit.org." );
			}
			else if ( ( updater.isUpdateAvailable() ) && ( updater.getOnUpdate().contains( AutoUpdater.WARN_OPERATORS ) ) )
			{
				User.sendMessage( ChatColor.DARK_PURPLE + "The version of CraftBukkit that this server is running is out of date. Please consider updating to the latest version at dl.bukkit.org." );
			}
		}
	}
	
	public HelpMap getHelpMap()
	{
		return helpMap;
	}
	
	public SimpleCommandMap getCommandMap()
	{
		return commandMap;
	}
	
	public boolean isPrimaryThread()
	{
		return Thread.currentThread().equals( console.primaryThread );
	}
	
	public WarningState getWarningState()
	{
		return warningState;
	}
	
	public List<String> tabComplete( CommandSender user, String message )
	{
		if ( !( user instanceof User ) )
		{
			return ImmutableList.of();
		}
		
		if ( message.startsWith( "/" ) )
		{
			return tabCompleteCommand( user, message );
		}
		else
		{
			return tabCompleteChat( (User) user, message );
		}
	}
	
	public List<String> tabCompleteCommand( CommandSender user, String message )
	{
		List<String> completions = null;
		try
		{
			completions = getCommandMap().tabComplete( user, message.substring( 1 ) );
		}
		catch ( CommandException ex )
		{
			user.sendMessage( ChatColor.RED + "An internal error occurred while attempting to tab-complete this command" );
			getLogger().log( Level.SEVERE, "Exception when " + user.getName() + " attempted to tab complete " + message, ex );
		}
		
		return completions == null ? ImmutableList.<String> of() : completions;
	}
	
	public List<String> tabCompleteChat( User User, String message )
	{
		User[] Users = getOnlineUsers();
		List<String> completions = new ArrayList<String>();
		UserChatTabCompleteEvent event = new UserChatTabCompleteEvent( User, message, completions );
		String token = event.getLastToken();
		for ( User p : Users )
		{
			if ( StringUtil.startsWithIgnoreCase( p.getName(), token ) )
			{
				completions.add( p.getName() );
			}
		}
		pluginManager.callEvent( event );
		
		Iterator<?> it = completions.iterator();
		while ( it.hasNext() )
		{
			Object current = it.next();
			if ( !( current instanceof String ) )
			{
				// Sanity
				it.remove();
			}
		}
		Collections.sort( completions, String.CASE_INSENSITIVE_ORDER );
		return completions;
	}
	
	public static void setServer( Server server )
	{
		Main.server = server;
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
	
	public static ResourceLoader getResourceLoader()
	{
		return resourceLoader;
	}
	
	private File getConfigFile()
	{
		return (File) console.options.valueOf( "chiori-settings" );
	}
	
	private void saveConfig()
	{
		try
		{
			configuration.save( getConfigFile() );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( Main.class.getName() ).log( Level.SEVERE, "Could not save " + getConfigFile(), ex );
		}
	}
}
