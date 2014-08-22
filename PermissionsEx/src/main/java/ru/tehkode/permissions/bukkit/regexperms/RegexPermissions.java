package ru.tehkode.permissions.bukkit.regexperms;

import java.util.logging.Level;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.events.PermissionSystemEvent;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.EventHandler;
import com.chiorichan.bus.events.EventPriority;
import com.chiorichan.bus.events.Listener;
import com.chiorichan.bus.events.account.AccountLoginEvent;
import com.chiorichan.bus.events.account.AccountLogoutEvent;
import com.chiorichan.permissions.Permissible;

public class RegexPermissions
{
	private final PermissionsEx plugin;
	private PermissionList permsList;
	// Permissions subscriptions handling
	private PEXPermissionSubscriptionMap subscriptionHandler;
	
	public RegexPermissions(PermissionsEx plugin)
	{
		this.plugin = plugin;
		subscriptionHandler = PEXPermissionSubscriptionMap.inject( plugin, Loader.getPermissionsManager() );
		permsList = PermissionList.inject( Loader.getPermissionsManager() );
		Loader.getEventBus().registerEvents( new EventListener(), plugin );
		injectAllPermissibles();
	}
	
	/*
	 * new PermissibleInjector.ClassPresencePermissibleInjector( "net.glowstone.entity.GlowHumanEntity", "permissions", true );
	 * new PermissibleInjector.ClassPresencePermissibleInjector( "org.getspout.server.entity.SpoutHumanEntity", "permissions", true );
	 * new PermissibleInjector.ClassNameRegexPermissibleInjector( "org.getspout.spout.user.SpoutCraftUser", "perm", false, "org\\.getspout\\.spout\\.user\\.SpoutCraftUser" );
	 * new PermissibleInjector.ClassPresencePermissibleInjector( getCBClassName( "entity.CraftHumanEntity" ), "perm", true );
	 */
	
	protected static final PermissibleInjector[] injectors = new PermissibleInjector[] { new PermissibleInjector.ClassPresencePermissibleInjector( "com.chiorichan.user.User", "perm", true ) };
	
	public void onDisable()
	{
		subscriptionHandler.uninject();
		uninjectAllPermissibles();
	}
	
	public boolean hasDebugMode()
	{
		PermissionManager manager = plugin.getPermissionsManager();
		return manager != null && manager.isDebug();
	}
	
	public PermissionList getPermissionList()
	{
		return permsList;
	}
	
	public void injectPermissible( Account user )
	{
		if ( user.hasPermission( "permissionsex.disabled" ) )
		{ // this user shouldn't get permissionsex matching
			return;
		}
		
		try
		{
			PermissiblePEX permissible = new PermissiblePEX( user, plugin );
			
			boolean success = false, found = false;
			for ( PermissibleInjector injector : injectors )
			{
				if ( injector.isApplicable( user ) )
				{
					found = true;
					Permissible oldPerm = injector.inject( user, permissible );
					if ( oldPerm != null )
					{
						permissible.setPreviousPermissible( oldPerm );
						success = true;
						break;
					}
				}
			}
			
			if ( !found )
			{
				plugin.getLogger().warning( "No Permissible injector found for your server implementation!" );
			}
			else if ( !success )
			{
				plugin.getLogger().warning( "Unable to inject PEX's permissible for " + user.getName() );
			}
			
			permissible.recalculatePermissions();
			
			if ( success && hasDebugMode() )
			{
				plugin.getLogger().info( "Permissions handler for " + user.getName() + " successfully injected" );
			}
		}
		catch ( Throwable e )
		{
			plugin.getLogger().log( Level.SEVERE, "Unable to inject permissible for " + user.getName(), e );
		}
	}
	
	private void injectAllPermissibles()
	{
		for ( Account user : Loader.getAccountsManager().getOnlineAccounts() )
		{
			injectPermissible( user );
		}
	}
	
	private void uninjectPermissible( Account user )
	{
		if ( user.hasPermission( "permissionsex.disabled" ) )
		{ // this user shouldn't get permissionsex matching
			return;
		}
		
		try
		{
			boolean success = false;
			for ( PermissibleInjector injector : injectors )
			{
				if ( injector.isApplicable( user ) )
				{
					Permissible pexPerm = injector.getPermissible( user );
					if ( pexPerm instanceof PermissiblePEX )
					{
						if ( injector.inject( user, ( (PermissiblePEX) pexPerm ).getPreviousPermissible() ) != null )
						{
							success = true;
							break;
						}
					}
				}
			}
			
			if ( !success )
			{
				plugin.getLogger().warning( "No Permissible injector found for your server implementation (while uninjecting for " + user.getName() + "!" );
			}
			else if ( hasDebugMode() )
			{
				plugin.getLogger().info( "Permissions handler for " + user.getName() + " successfully uninjected" );
			}
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
		}
	}
	
	private void uninjectAllPermissibles()
	{
		for ( Account user : Loader.getAccountsManager().getOnlineAccounts() )
		{
			uninjectPermissible( user );
		}
	}
	
	private class EventListener implements Listener
	{
		@EventHandler( priority = EventPriority.LOWEST )
		public void onUserLogin( AccountLoginEvent event )
		{
			injectPermissible( event.getAccount() );
		}
		
		@EventHandler( priority = EventPriority.MONITOR )
		// Technically not supposed to use MONITOR for this, but we don't want to remove before other plugins are done checking permissions
		public void onUserQuit( AccountLogoutEvent event )
		{
			uninjectPermissible( event.getAccount() );
		}
		
		@EventHandler( priority = EventPriority.LOWEST )
		public void onPermissionSystemEvent( PermissionSystemEvent event )
		{
			switch ( event.getAction() )
			{
				case REINJECT_PERMISSIBLES:
				case RELOADED:
					uninjectAllPermissibles();
					injectAllPermissibles();
					break;
				default:
					return;
			}
		}
	}
	
}
