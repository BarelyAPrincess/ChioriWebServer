package ru.tehkode.permissions.bukkit;

import java.util.HashMap;
import java.util.Map;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

import com.chiorichan.Loader;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.user.UserChangedEvent;
import com.chiorichan.event.user.UserJoinEvent;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLogoutEvent;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionAttachment;
import com.chiorichan.permissions.PermissionDefault;
import com.chiorichan.user.User;

/**
 * PEX permissions database integration with superperms
 */
public class SuperpermsListener implements Listener
{
	private final PermissionsEx plugin;
	private final Map<String, PermissionAttachment> attachments = new HashMap<String, PermissionAttachment>();
	
	public SuperpermsListener(PermissionsEx plugin)
	{
		this.plugin = plugin;
		for ( User user : plugin.getInstance().getOnlineUsers() )
		{
			updateAttachment( user );
		}
	}
	
	protected void updateAttachment( User user )
	{
		updateAttachment( user, user.getSite().getName() );
	}
	
	protected void updateAttachment( User user, String siteName )
	{
		PermissionAttachment attach = attachments.get( user.getName() );
		Permission userPerm = getCreateWrapper( user, "" );
		Permission userOptionPerm = getCreateWrapper( user, ".options" );
		if ( attach == null )
		{
			attach = user.addAttachment( plugin );
			attachments.put( user.getName(), attach );
			attach.setPermission( userPerm, true );
		}
		
		PermissionUser puser = plugin.getPermissionsManager().getUser( user );
		if ( user != null )
		{
			updateUserPermission( userPerm, user, puser, siteName );
			updateUserMetadata( userOptionPerm, puser, siteName );
			user.recalculatePermissions();
		}
	}
	
	private String permissionName( User user, String suffix )
	{
		return "permissionsex.user." + user.getName() + suffix;
	}
	
	private void removePEXPerm( User user, String suffix )
	{
		Loader.getPluginManager().removePermission( permissionName( user, suffix ) );
	}
	
	private Permission getCreateWrapper( User user, String suffix )
	{
		final String name = permissionName( user, suffix );
		Permission perm = Loader.getPluginManager().getPermission( name );
		if ( perm == null )
		{
			perm = new Permission( name, "Internal permission for PEX. DO NOT SET DIRECTLY", PermissionDefault.FALSE );
			plugin.getInstance().getPluginManager().addPermission( perm );
		}
		
		return perm;
		
	}
	
	private void updateUserPermission( Permission permission, User user, PermissionUser puser, String siteName )
	{
		permission.getChildren().clear();
		permission.getChildren().put( permissionName( user, ".options" ), true );
		for ( String perm : puser.getPermissions( siteName ) )
		{
			boolean value = true;
			if ( perm.startsWith( "-" ) )
			{
				value = false;
				perm = perm.substring( 1 );
			}
			if ( !permission.getChildren().containsKey( perm ) )
			{
				permission.getChildren().put( perm, value );
			}
		}
	}
	
	private void updateUserMetadata( Permission rootPermission, PermissionUser user, String siteName )
	{
		rootPermission.getChildren().clear();
		final String[] groups = user.getGroupsNames( siteName );
		final Map<String, String> options = user.getOptions( siteName );
		// Metadata
		// Groups
		for ( String group : groups )
		{
			rootPermission.getChildren().put( "groups." + group, true );
			rootPermission.getChildren().put( "group." + group, true );
		}
		
		// Options
		for ( Map.Entry<String, String> option : options.entrySet() )
		{
			rootPermission.getChildren().put( "options." + option.getKey() + "." + option.getValue(), true );
		}
		
		// Prefix and Suffix
		rootPermission.getChildren().put( "prefix." + user.getPrefix( siteName ), true );
		rootPermission.getChildren().put( "suffix." + user.getSuffix( siteName ), true );
		
	}
	
	protected void removeAttachment( User user )
	{
		PermissionAttachment attach = attachments.remove( user.getName() );
		if ( attach != null )
		{
			attach.remove();
		}
		
		removePEXPerm( user, "" );
		removePEXPerm( user, ".options" );
	}
	
	public void onDisable()
	{
		for ( PermissionAttachment attach : attachments.values() )
		{
			attach.remove();
		}
		attachments.clear();
	}
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onUserJoin( UserJoinEvent event )
	{
		try
		{
			updateAttachment( event.getUser() );
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event join", t );
		}
	}
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onUserLogin( UserLoginEvent event )
	{
		try
		{
			final User user = event.getUser();
			// Because user site is inaccurate in the login event (at least with MV), start with null site and then reset to the real site in join event
			updateAttachment( user, null );
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event login", t );
		}
	}
	
	@EventHandler( priority = EventPriority.MONITOR )
	// Technically not supposed to use MONITOR for this, but we don't want to remove before other plugins are done checking permissions
	public void onUserQuit( UserLogoutEvent event )
	{
		try
		{
			removeAttachment( event.getUser() );
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event quit", t );
		}
	}
	
	private void updateSelective( PermissionEntityEvent event, PermissionUser user )
	{
		final User p = plugin.getInstance().getUserExact( user.getName() );
		if ( p != null )
		{
			switch ( event.getAction() )
			{
				case SAVED:
					break;
				
				case PERMISSIONS_CHANGED:
				case TIMEDPERMISSION_EXPIRED:
					updateUserPermission( getCreateWrapper( p, "" ), p, user, p.getSite().getName() );
					p.recalculatePermissions();
					break;
				
				case OPTIONS_CHANGED:
				case INFO_CHANGED:
					updateUserMetadata( getCreateWrapper( p, ".options" ), user, p.getSite().getName() );
					p.recalculatePermissions();
					break;
				
				default:
					updateAttachment( p );
			}
		}
	}
	
	@EventHandler( priority = EventPriority.LOW )
	public void onEntityEvent( PermissionEntityEvent event )
	{
		try
		{
			if ( event.getEntity() instanceof PermissionUser )
			{ // update user only
				updateSelective( event, (PermissionUser) event.getEntity() );
			}
			else if ( event.getEntity() instanceof PermissionGroup )
			{ // update all members of group, might be resource hog
				for ( PermissionUser user : plugin.getPermissionsManager().getUsers( event.getEntity().getName(), true ) )
				{
					updateSelective( event, user );
				}
			}
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event permission entity", t );
		}
	}
	
	@EventHandler
	public void onSiteChanged( UserChangedEvent event )
	{
		try
		{
			updateAttachment( event.getUser() );
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event site change", t );
		}
	}
	
	@EventHandler( priority = EventPriority.LOW )
	public void onSystemEvent( PermissionSystemEvent event )
	{
		try
		{
			if ( event.getAction() == PermissionSystemEvent.Action.DEBUGMODE_TOGGLE )
			{
				return;
			}
			switch ( event.getAction() )
			{
				case DEBUGMODE_TOGGLE:
				case REINJECT_PERMISSIBLES:
					return;
				default:
					for ( User p : plugin.getInstance().getOnlineUsers() )
					{
						updateAttachment( p );
					}
			}
		}
		catch ( Throwable t )
		{
			ErrorReport.handleError( "Superperms event permission system event", t );
		}
	}
}