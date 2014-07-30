package ru.tehkode.permissions.bukkit.commands;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.CommandListener;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.commands.exceptions.AutoCompleteChoicesException;
import ru.tehkode.utils.StringUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.framework.Site;
import com.chiorichan.plugin.Plugin;

public abstract class PermissionsCommand implements CommandListener
{
	
	protected static final Logger logger = Logger.getLogger( "" );
	protected CommandsManager manager;
	
	@Override
	public void onRegistered( CommandsManager manager )
	{
		this.manager = manager;
	}
	
	protected void informGroup( Plugin plugin, PermissionGroup group, String message )
	{
		for ( PermissionUser user : group.getUsers() )
		{
			this.informUser( plugin, user.getName(), message );
		}
	}
	
	protected void informUser( Plugin plugin, String userName, String message )
	{
		if ( !( plugin instanceof PermissionsEx ) || !( (PermissionsEx) plugin ).getConfig().getBoolean( "permissions.informusers.changes", false ) )
		{
			return; // User informing is disabled
		}
		
		Account user = Loader.getAccountsManager().getAccount( userName );
		if ( user == null )
		{
			return;
		}
		
		user.sendMessage( ChatColor.BLUE + "[PermissionsEx] " + ChatColor.WHITE + message );
	}
	
	protected String autoCompleteUserName( String userName )
	{
		return autoCompleteUserName( userName, "user" );
	}
	
	protected void printEntityInheritance( Sentient sender, PermissionGroup[] groups )
	{
		for ( PermissionGroup group : groups )
		{
			String rank = "not ranked";
			if ( group.isRanked() )
			{
				rank = "rank " + group.getRank() + " @ " + group.getRankLadder();
			}
			
			sender.sendMessage( "   " + group.getName() + " (" + rank + ")" );
		}
	}
	
	protected String autoCompleteUserName( String userName, String argName )
	{
		if ( userName == null )
		{
			return null;
		}
		
		if ( userName.startsWith( "#" ) )
		{
			return userName.substring( 1 );
		}
		
		List<String> users = new LinkedList<String>();
		
		// Collect online User names
		for ( Account user : Loader.getAccountsManager().getOnlineAccounts() )
		{
			if ( user.getName().equalsIgnoreCase( userName ) )
			{
				return user.getName();
			}
			
			if ( user.getName().toLowerCase().startsWith( userName.toLowerCase() ) && !users.contains( user.getName() ) )
			{
				users.add( user.getName() );
			}
		}
		
		// Collect registered PEX user names
		for ( String user : PermissionsEx.getPermissionManager().getUserNames() )
		{
			if ( user.equalsIgnoreCase( userName ) )
			{
				return user;
			}
			
			if ( user.toLowerCase().startsWith( userName.toLowerCase() ) && !users.contains( user ) )
			{
				users.add( user );
			}
		}
		
		if ( users.size() > 1 )
		{
			throw new AutoCompleteChoicesException( users.toArray( new String[0] ), argName );
		}
		else if ( users.size() == 1 )
		{
			return users.get( 0 );
		}
		
		// Nothing found
		return userName;
	}
	
	protected String getSenderName( Sentient sender )
	{
		if ( sender instanceof Account )
		{
			return ( (Account) sender ).getName();
		}
		
		return "console";
	}
	
	protected String autoCompleteGroupName( String groupName )
	{
		return this.autoCompleteGroupName( groupName, "group" );
	}
	
	protected String autoCompleteGroupName( String groupName, String argName )
	{
		
		if ( groupName.startsWith( "#" ) )
		{
			return groupName.substring( 1 );
		}
		
		List<String> groups = new LinkedList<String>();
		
		for ( String group : PermissionsEx.getPermissionManager().getGroupNames() )
		{
			if ( group.equalsIgnoreCase( groupName ) )
			{
				return group;
			}
			
			if ( group.toLowerCase().startsWith( groupName.toLowerCase() ) && !groups.contains( group ) )
			{
				groups.add( group );
			}
		}
		
		if ( groups.size() > 1 )
		{ // Found several choices
			throw new AutoCompleteChoicesException( groups.toArray( new String[0] ), argName );
		}
		else if ( groups.size() == 1 )
		{ // Found one name
			return groups.get( 0 );
		}
		
		// Nothing found
		return groupName;
	}
	
	protected String autoCompleteSiteName( String siteName )
	{
		return this.autoCompleteSiteName( siteName, "site" );
	}
	
	protected String autoCompleteSiteName( String siteName, String argName )
	{
		if ( siteName == null || siteName.isEmpty() || "*".equals( siteName ) )
		{
			return null;
		}
		
		List<String> sites = new LinkedList<String>();
		
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			if ( site.getName().equalsIgnoreCase( siteName ) )
			{
				return site.getName();
			}
			
			if ( site.getName().toLowerCase().startsWith( siteName.toLowerCase() ) && !sites.contains( site.getName() ) )
			{
				sites.add( site.getName() );
			}
		}
		
		if ( sites.size() > 1 )
		{ // Found several choices
			throw new AutoCompleteChoicesException( sites.toArray( new String[0] ), argName );
		}
		else if ( sites.size() == 1 )
		{ // Found one name
			return sites.get( 0 );
		}
		
		return siteName;
	}
	
	protected String getSafeSiteName( String siteName, String userName )
	{
		if ( siteName == null )
		{
			Account user = Loader.getAccountsManager().getAccount( userName );
			
			if ( user != null )
			{
				siteName = user.getSite().getName();
			}
			else
			{
				siteName = Loader.getSiteManager().getSites().get( 0 ).getName();
			}
		}
		
		return siteName;
	}
	
	protected String autoCompletePermission( PermissionEntity entity, String permission, String siteName )
	{
		return this.autoCompletePermission( entity, permission, siteName, "permission" );
	}
	
	protected String autoCompletePermission( PermissionEntity entity, String permission, String siteName, String argName )
	{
		if ( permission == null )
		{
			return permission;
		}
		
		Set<String> permissions = new HashSet<String>();
		for ( String currentPermission : entity.getPermissions( siteName ) )
		{
			if ( currentPermission.equalsIgnoreCase( permission ) )
			{
				return currentPermission;
			}
			
			if ( currentPermission.toLowerCase().startsWith( permission.toLowerCase() ) )
			{
				permissions.add( currentPermission );
			}
		}
		
		if ( permissions.size() > 0 )
		{
			String[] permissionArray = permissions.toArray( new String[0] );
			
			if ( permissionArray.length == 1 )
			{
				return permissionArray[0];
			}
			
			throw new AutoCompleteChoicesException( permissionArray, argName );
		}
		
		return permission;
	}
	
	protected int getPosition( String permission, String[] permissions )
	{
		try
		{
			// permission is permission index
			int position = Integer.parseInt( permission ) - 1;
			
			if ( position < 0 || position >= permissions.length )
			{
				throw new RuntimeException( "Wrong permission index specified!" );
			}
			
			return position;
		}
		catch ( NumberFormatException e )
		{
			// permission is permission text
			for ( int i = 0; i < permissions.length; i++ )
			{
				if ( permission.equalsIgnoreCase( permissions[i] ) )
				{
					return i;
				}
			}
		}
		
		throw new RuntimeException( "Specified permission not found" );
	}
	
	protected String printHierarchy( PermissionGroup parent, String siteName, int level )
	{
		StringBuilder buffer = new StringBuilder();
		
		PermissionGroup[] groups;
		if ( parent == null )
		{
			groups = PermissionsEx.getPermissionManager().getGroups();
		}
		else
		{
			groups = parent.getChildGroups( siteName );
		}
		
		for ( PermissionGroup group : groups )
		{
			if ( parent == null && group.getParentGroups( siteName ).length > 0 )
			{
				continue;
			}
			
			buffer.append( StringUtils.repeat( "  ", level ) ).append( " - " ).append( group.getName() ).append( "\n" );
			
			// Groups
			buffer.append( printHierarchy( group, siteName, level + 1 ) );
			
			for ( PermissionUser user : group.getUsers( siteName ) )
			{
				buffer.append( StringUtils.repeat( "  ", level + 1 ) ).append( " + " ).append( user.getName() ).append( "\n" );
			}
		}
		
		return buffer.toString();
	}
	
	protected String mapPermissions( String siteName, PermissionEntity entity, int level )
	{
		StringBuilder builder = new StringBuilder();
		
		int index = 1;
		for ( String permission : this.getPermissionsTree( entity, siteName, 0 ) )
		{
			if ( level > 0 )
			{
				builder.append( "   " );
			}
			else
			{
				builder.append( index++ ).append( ") " );
			}
			
			builder.append( permission );
			if ( level > 0 )
			{
				builder.append( " (from " ).append( entity.getName() ).append( ")" );
			}
			else
			{
				builder.append( " (own)" );
			}
			builder.append( "\n" );
		}
		
		PermissionGroup[] parents;
		
		if ( entity instanceof PermissionUser )
		{
			parents = ( (PermissionUser) entity ).getGroups( siteName );
		}
		else if ( entity instanceof PermissionGroup )
		{
			parents = ( (PermissionGroup) entity ).getParentGroups( siteName );
		}
		else
		{
			throw new RuntimeException( "Unknown class in hierarchy. Nag t3hk0d3 please." );
		}
		
		level++; // Just increment level once
		return builder.toString();
	}
	
	protected List<String> getPermissionsTree( PermissionEntity entity, String site, int level )
	{
		List<String> permissions = new LinkedList<String>();
		Map<String, String[]> allPermissions = entity.getAllPermissions();
		
		String[] sitesPermissions = allPermissions.get( site );
		if ( sitesPermissions != null )
		{
			permissions.addAll( sprintPermissions( site, sitesPermissions ) );
		}
		
		for ( String parentSite : PermissionsEx.getPermissionManager().getSiteInheritance( site ) )
		{
			if ( parentSite != null && !parentSite.isEmpty() )
			{
				permissions.addAll( getPermissionsTree( entity, parentSite, level + 1 ) );
			}
		}
		
		if ( level == 0 && allPermissions.get( "" ) != null )
		{ // default site permissions
			permissions.addAll( sprintPermissions( "common", allPermissions.get( "" ) ) );
		}
		
		return permissions;
	}
	
	protected List<String> sprintPermissions( String site, String[] permissions )
	{
		List<String> permissionList = new LinkedList<String>();
		
		if ( permissions == null )
		{
			return permissionList;
		}
		
		for ( String permission : permissions )
		{
			permissionList.add( permission + ( site != null ? " @" + site : "" ) );
		}
		
		return permissionList;
	}
	
	protected Object parseValue( String value )
	{
		if ( value == null )
		{
			return null;
		}
		
		if ( value.equalsIgnoreCase( "true" ) || value.equalsIgnoreCase( "false" ) )
		{
			return Boolean.parseBoolean( value );
		}
		
		try
		{
			return Integer.parseInt( value );
		}
		catch ( NumberFormatException e )
		{}
		
		try
		{
			return Double.parseDouble( value );
		}
		catch ( NumberFormatException e )
		{}
		
		return value;
	}
	
	protected void sendMessage( Sentient sender, String message )
	{
		for ( String messagePart : message.split( "\n" ) )
		{
			sender.sendMessage( messagePart );
		}
	}
}
