package ru.tehkode.permissions.bukkit;

import java.util.logging.Level;

import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.backends.MemoryBackend;
import ru.tehkode.permissions.backends.SQLBackend;
import ru.tehkode.permissions.bukkit.commands.GroupCommands;
import ru.tehkode.permissions.bukkit.commands.PromotionCommands;
import ru.tehkode.permissions.bukkit.commands.SiteCommands;
import ru.tehkode.permissions.bukkit.commands.UserCommands;
import ru.tehkode.permissions.bukkit.commands.UtilityCommands;
import ru.tehkode.permissions.bukkit.regexperms.RegexPermissions;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;
import ru.tehkode.utils.StringUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLogoutEvent;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.ServicePriority;
import com.chiorichan.plugin.java.JavaPlugin;
import com.chiorichan.user.User;

/**
 * @author code
 */
public class PermissionsEx extends JavaPlugin
{
	protected static final String CONFIG_FILE = "config.yml";
	protected PermissionManager permissionsManager;
	protected CommandsManager commandsManager;
	protected FileConfiguration config;
	protected SuperpermsListener superms;
	private RegexPermissions regexPerms;
	private boolean errored = false;
	private static PermissionsEx instance;
	{
		instance = this;
	}
	
	public PermissionsEx()
	{
		super();
		
		PermissionBackend.registerBackendAlias( "sql", SQLBackend.class );
		PermissionBackend.registerBackendAlias( "file", FileBackend.class );
		PermissionBackend.registerBackendAlias( "memory", MemoryBackend.class );
		
	}
	
	private void logBackendExc( PermissionBackendException e )
	{
		getLogger().log( Level.SEVERE, "\n========== UNABLE TO LOAD PERMISSIONS BACKEND =========\n" + "Your configuration must be fixed before PEX will enable\n" + "Details: " + e.getMessage() + "\n" + "=======================================================", e );
	}
	
	@Override
	public void onLoad()
	{
		try
		{
			this.config = this.getConfig();
			this.commandsManager = new CommandsManager( this );
			this.permissionsManager = new PermissionManager( this.config );
		}
		catch ( PermissionBackendException e )
		{
			logBackendExc( e );
			errored = true;
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "In onLoad", t );
			errored = true;
		}
	}
	
	@Override
	public void onEnable()
	{
		if ( errored )
		{
			getLogger().severe( "==== PermissionsEx could not be enabled due to an earlier error. Look at the previous server log for more info ====" );
			this.getPluginLoader().disablePlugin( this );
			return;
		}
		try
		{
			if ( this.permissionsManager == null )
			{
				this.permissionsManager = new PermissionManager( this.config );
			}
			
			// Register commands
			this.commandsManager.register( new UserCommands() );
			this.commandsManager.register( new GroupCommands() );
			this.commandsManager.register( new PromotionCommands() );
			this.commandsManager.register( new SiteCommands() );
			this.commandsManager.register( new UtilityCommands() );
			
			// Register User permissions cleaner
			UserEventsListener cleaner = new UserEventsListener();
			cleaner.logLastUserLogin = this.config.getBoolean( "permissions.log-users", cleaner.logLastUserLogin );
			Loader.getEventBus().registerEvents( cleaner, this );
			
			// register service
			this.getInstance().getServicesManager().register( PermissionManager.class, this.permissionsManager, this, ServicePriority.Normal );
			regexPerms = new RegexPermissions( this );
			superms = new SuperpermsListener( this );
			Loader.getEventBus().registerEvents( superms, this );
			this.saveConfig();
			
			// Start timed permissions cleaner timer
			this.permissionsManager.initTimer();
		}
		catch ( PermissionBackendException e )
		{
			logBackendExc( e );
			this.getPluginLoader().disablePlugin( this );
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Error while enabling: ", t );
			this.getPluginLoader().disablePlugin( this );
		}
	}
	
	@Override
	public void onDisable()
	{
		try
		{
			if ( this.permissionsManager != null )
			{
				this.permissionsManager.end();
			}
			
			this.getInstance().getServicesManager().unregister( PermissionManager.class, this.permissionsManager );
			if ( this.regexPerms != null )
			{
				this.regexPerms.onDisable();
			}
			if ( this.superms != null )
			{
				this.superms.onDisable();
			}
			
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "While disabling", t );
		}
		ErrorReport.shutdown();
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String commandLabel, String[] args )
	{
		try
		{
			PluginDescriptionFile pdf = this.getDescription();
			if ( args.length > 0 )
			{
				return this.commandsManager.execute( sender, command, args );
			}
			else
			{
				if ( sender instanceof User )
				{
					sender.sendMessage( "[" + ChatColor.RED + "PermissionsEx" + ChatColor.WHITE + "] version [" + ChatColor.BLUE + pdf.getVersion() + ChatColor.WHITE + "]" );
					
					return !this.permissionsManager.has( (User) sender, "permissions.manage" );
				}
				else
				{
					sender.sendMessage( "[PermissionsEx] version [" + pdf.getVersion() + "]" );
					
					return false;
				}
			}
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "While " + sender.getName() + " was executing /" + command.getName() + " " + StringUtils.implode( args, " " ), t, sender );
			return true;
		}
	}
	
	public boolean isDebug()
	{
		return permissionsManager.isDebug();
	}
	
	public static Plugin getPlugin()
	{
		return instance;
	}
	
	public RegexPermissions getRegexPerms()
	{
		return regexPerms;
	}
	
	public static boolean isAvailable()
	{
		Plugin plugin = getPlugin();
		
		return plugin.isEnabled() && ( (PermissionsEx) plugin ).permissionsManager != null;
	}
	
	public static PermissionManager getPermissionManager()
	{
		if ( !isAvailable() )
		{
			throw new PermissionsNotAvailable();
		}
		
		return ( (PermissionsEx) getPlugin() ).permissionsManager;
	}
	
	public PermissionManager getPermissionsManager()
	{
		return permissionsManager;
	}
	
	public static PermissionUser getUser( User user )
	{
		return getPermissionManager().getUser( user );
	}
	
	public static PermissionUser getUser( String name )
	{
		return getPermissionManager().getUser( name );
	}
	
	public boolean has( User user, String permission )
	{
		return this.permissionsManager.has( user, permission );
	}
	
	public boolean has( User user, String permission, String site )
	{
		return this.permissionsManager.has( user, permission, site );
	}
	
	public class UserEventsListener implements Listener
	{
		
		protected boolean logLastUserLogin = false;
		
		@EventHandler
		public void onUserLogin( UserLoginEvent event )
		{
			try
			{
				if ( !logLastUserLogin )
				{
					return;
				}
				
				PermissionUser user = getPermissionManager().getUser( event.getUser() );
				user.setOption( "last-login-time", Long.toString( System.currentTimeMillis() / 1000L ) );
				// user.setOption("last-login-ip", event.getUser().getAddress().getAddress().getHostAddress()); // somehow this won't work
			}
			catch ( Throwable t )
			{
				ErrorReport.handleError( "While login cleanup event", t );
			}
		}
		
		@EventHandler
		public void onUserQuit( UserLogoutEvent event )
		{
			try
			{
				if ( logLastUserLogin )
				{
					getPermissionManager().getUser( event.getUser() ).setOption( "last-logout-time", Long.toString( System.currentTimeMillis() / 1000L ) );
				}
				
				getPermissionManager().resetUser( event.getUser().getName() );
			}
			catch ( Throwable t )
			{
				ErrorReport.handleError( "While logout cleanup event", t );
			}
		}
	}
}
