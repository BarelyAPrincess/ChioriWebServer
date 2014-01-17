package ru.tehkode.permissions.bukkit.commands;

import java.util.Map;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.DateUtils;
import ru.tehkode.utils.StringUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.user.User;

public class UserCommands extends PermissionsCommand
{
	
	@Command( name = "pex", syntax = "users list", permission = "permissions.manage.users", description = "List all registered users" )
	public void usersList( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers();
		
		sender.sendMessage( ChatColor.WHITE + "Currently registered users: " );
		for ( PermissionUser user : users )
		{
			sender.sendMessage( " " + user.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode( user.getGroupsNames(), ", " ) + "]" );
		}
	}
	
	@Command( name = "pex", syntax = "users", permission = "permissions.manage.users", description = "List all registered users (alias)", isPrimary = true )
	public void userListAlias( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		this.usersList( plugin, sender, args );
	}
	
	@Command( name = "pex", syntax = "user", permission = "permissions.manage.users", description = "List all registered users (alias)" )
	public void userListAnotherAlias( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		this.usersList( plugin, sender, args );
	}
	
	/**
	 * User permission management
	 */
	@Command( name = "pex", syntax = "user <user>", permission = "permissions.manage.users.permissions.<user>", description = "List user permissions (list alias)" )
	public void userListAliasPermissions( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( "'" + userName + "' is a member of:" );
		printEntityInheritance( sender, user.getGroups() );
		
		for ( String site : user.getAllGroups().keySet() )
		{
			if ( site == null )
			{
				continue;
			}
			
			sender.sendMessage( "  @" + site + ":" );
			printEntityInheritance( sender, user.getAllGroups().get( site ) );
		}
		
		sender.sendMessage( userName + "'s permissions:" );
		
		this.sendMessage( sender, this.mapPermissions( siteName, user, 0 ) );
		
		sender.sendMessage( userName + "'s options:" );
		for ( Map.Entry<String, String> option : user.getOptions( siteName ).entrySet() )
		{
			sender.sendMessage( "  " + option.getKey() + " = \"" + option.getValue() + "\"" );
		}
	}
	
	@Command( name = "pex", syntax = "user <user> list [site]", permission = "permissions.manage.users.permissions.<user>", description = "List user permissions" )
	public void userListPermissions( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( userName + "'s permissions:" );
		
		for ( String permission : user.getPermissions( siteName ) )
		{
			sender.sendMessage( "  " + permission );
		}
		
	}
	
	@Command( name = "pex", syntax = "user <user> superperms", permission = "permissions.manage.users.permissions.<user>", description = "List user actual superperms" )
	public void userListSuperPermissions( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		
		User user = Loader.getInstance().getUser( userName );
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User not found (offline?)" );
			return;
		}
		
		sender.sendMessage( userName + "'s superperms:" );
		
		for ( PermissionAttachmentInfo info : user.getEffectivePermissions() )
		{
			String pluginName = "built-in";
			
			if ( info.getAttachment() != null && info.getAttachment().getPlugin() != null )
			{
				pluginName = info.getAttachment().getPlugin().getDescription().getName();
			}
			
			sender.sendMessage( " '" + ChatColor.GREEN + info.getPermission() + ChatColor.WHITE + "' = " + ChatColor.BLUE + info.getValue() + ChatColor.WHITE + " by " + ChatColor.DARK_GREEN + pluginName );
		}
	}
	
	@Command( name = "pex", syntax = "user <user> prefix [newprefix] [site]", permission = "permissions.manage.users.prefix.<user>", description = "Get or set <user> prefix" )
	public void userPrefix( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "newprefix" ) )
		{
			user.setPrefix( args.get( "newprefix" ), siteName );
		}
		
		sender.sendMessage( user.getName() + "'s prefix = \"" + user.getPrefix() + "\"" );
	}
	
	@Command( name = "pex", syntax = "user <user> suffix [newsuffix] [site]", permission = "permissions.manage.users.suffix.<user>", description = "Get or set <user> suffix" )
	public void userSuffix( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "newsuffix" ) )
		{
			user.setSuffix( args.get( "newsuffix" ), siteName );
		}
		
		sender.sendMessage( user.getName() + "'s suffix = \"" + user.getSuffix() + "\"" );
	}
	
	@Command( name = "pex", syntax = "user <user> toggle debug", permission = "permissions.manage.<user>", description = "Toggle debug only for <user>" )
	public void userToggleDebug( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		user.setDebug( !user.isDebug() );
		
		sender.sendMessage( "Debug mode for user " + userName + " " + ( user.isDebug() ? "enabled" : "disabled" ) + "!" );
	}
	
	@Command( name = "pex", syntax = "user <user> check <permission> [site]", permission = "permissions.manage.<user>", description = "Checks user for <permission>" )
	public void userCheckPermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		siteName = this.getSafeSiteName( siteName, userName );
		
		String permission = user.getMatchingExpression( args.get( "permission" ), siteName );
		
		if ( permission == null )
		{
			sender.sendMessage( "User \"" + userName + "\" don't such have no permission" );
		}
		else
		{
			sender.sendMessage( "User \"" + userName + "\" have \"" + permission + "\" = " + user.explainExpression( permission ) );
		}
	}
	
	@Command( name = "pex", syntax = "user <user> get <option> [site]", permission = "permissions.manage.<user>", description = "Toggle debug only for <user>" )
	public void userGetOption( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		siteName = this.getSafeSiteName( siteName, userName );
		
		String value = user.getOption( args.get( "option" ), siteName, null );
		
		sender.sendMessage( "User " + userName + " @ " + siteName + " option \"" + args.get( "option" ) + "\" = \"" + value + "\"" );
	}
	
	@Command( name = "pex", syntax = "user <user> delete", permission = "permissions.manage.users.<user>", description = "Remove <user>" )
	public void userDelete( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		if ( user.isVirtual() )
		{
			sender.sendMessage( ChatColor.RED + "User is virtual" );
		}
		
		user.remove();
		
		PermissionsEx.getPermissionManager().resetUser( userName );
		
		sender.sendMessage( ChatColor.WHITE + "User \"" + user.getName() + "\" removed!" );
	}
	
	@Command( name = "pex", syntax = "user <user> add <permission> [site]", permission = "permissions.manage.users.permissions.<user>", description = "Add <permission> to <user> in [site]" )
	public void userAddPermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		user.addPermission( args.get( "permission" ), siteName );
		
		sender.sendMessage( ChatColor.WHITE + "Permission \"" + args.get( "permission" ) + "\" added!" );
		
		this.informUser( plugin, userName, "Your permissions have been changed!" );
	}
	
	@Command( name = "pex", syntax = "user <user> remove <permission> [site]", permission = "permissions.manage.users.permissions.<user>", description = "Remove permission from <user> in [site]" )
	public void userRemovePermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		String permission = this.autoCompletePermission( user, args.get( "permission" ), siteName );
		
		user.removePermission( permission, siteName );
		user.removeTimedPermission( permission, siteName );
		
		sender.sendMessage( ChatColor.WHITE + "Permission \"" + permission + "\" removed!" );
		this.informUser( plugin, userName, "Your permissions have been changed!" );
	}
	
	@Command( name = "pex", syntax = "user <user> swap <permission> <targetPermission> [site]", permission = "permissions.manage.users.permissions.<user>", description = "Swap <permission> and <targetPermission> in permission list. Could be number or permission itself" )
	public void userSwapPermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		String[] permissions = user.getOwnPermissions( siteName );
		
		try
		{
			int sourceIndex = this.getPosition( this.autoCompletePermission( user, args.get( "permission" ), siteName, "permission" ), permissions );
			int targetIndex = this.getPosition( this.autoCompletePermission( user, args.get( "targetPermission" ), siteName, "targetPermission" ), permissions );
			
			String targetPermission = permissions[targetIndex];
			
			permissions[targetIndex] = permissions[sourceIndex];
			permissions[sourceIndex] = targetPermission;
			
			user.setPermissions( permissions, siteName );
			
			sender.sendMessage( "Permissions swapped!" );
		}
		catch ( Throwable e )
		{
			sender.sendMessage( ChatColor.RED + "Error: " + e.getMessage() );
		}
	}
	
	@Command( name = "pex", syntax = "user <user> timed add <permission> [lifetime] [site]", permission = "permissions.manage.users.permissions.timed.<user>", description = "Add timed <permissions> to <user> for [lifetime] seconds in [site]" )
	public void userAddTimedPermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		int lifetime = 0;
		
		if ( args.containsKey( "lifetime" ) )
		{
			lifetime = DateUtils.parseInterval( args.get( "lifetime" ) );
		}
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		String permission = args.get( "permission" );
		
		user.addTimedPermission( permission, siteName, lifetime );
		
		sender.sendMessage( ChatColor.WHITE + "Timed permission \"" + permission + "\" added!" );
		this.informUser( plugin, userName, "Your permissions have been changed!" );
		
		logger.info( "User " + userName + " get timed permission \"" + args.get( "permission" ) + "\" " + ( lifetime > 0 ? "for " + lifetime + " seconds " : " " ) + "from " + getSenderName( sender ) );
	}
	
	@Command( name = "pex", syntax = "user <user> timed remove <permission> [site]", permission = "permissions.manage.users.permissions.timed.<user>", description = "Remove timed <permission> from <user> in [site]" )
	public void userRemoveTimedPermission( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		String permission = args.get( "permission" );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		user.removeTimedPermission( args.get( "permission" ), siteName );
		
		sender.sendMessage( ChatColor.WHITE + "Timed permission \"" + permission + "\" removed!" );
		this.informUser( plugin, userName, "Your permissions have been changed!" );
	}
	
	@Command( name = "pex", syntax = "user <user> set <option> <value> [site]", permission = "permissions.manage.users.permissions.<user>", description = "Set <option> to <value> in [site]" )
	public void userSetOption( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		user.setOption( args.get( "option" ), args.get( "value" ), siteName );
		
		if ( args.containsKey( "value" ) && args.get( "value" ).isEmpty() )
		{
			sender.sendMessage( ChatColor.WHITE + "Option \"" + args.get( "option" ) + "\" cleared!" );
		}
		else
		{
			sender.sendMessage( ChatColor.WHITE + "Option \"" + args.get( "option" ) + "\" set!" );
		}
		
		this.informUser( plugin, userName, "Your permissions have been changed!" );
	}
	
	/**
	 * User's groups management
	 */
	@Command( name = "pex", syntax = "user <user> group list [site]", permission = "permissions.manage.membership.<user>", description = "List all <user> groups" )
	public void userListGroup( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( "User " + args.get( "user" ) + " @" + siteName + " currently in:" );
		for ( PermissionGroup group : user.getGroups( siteName ) )
		{
			sender.sendMessage( "  " + group.getName() );
		}
	}
	
	@Command( name = "pex", syntax = "user <user> group add <group> [site] [lifetime]", permission = "permissions.manage.membership.<group>", description = "Add <user> to <group>" )
	public void userAddGroup( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String groupName = this.autoCompleteGroupName( args.get( "group" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "lifetime" ) )
		{
			try
			{
				int lifetime = DateUtils.parseInterval( args.get( "lifetime" ) );
				
				user.addGroup( groupName, siteName, lifetime );
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( ChatColor.RED + "Group lifetime should be number!" );
				return;
			}
			
		}
		else
		{
			user.addGroup( groupName, siteName );
		}
		
		sender.sendMessage( ChatColor.WHITE + "User added to group \"" + groupName + "\"!" );
		this.informUser( plugin, userName, "You are assigned to \"" + groupName + "\" group" );
	}
	
	@Command( name = "pex", syntax = "user <user> group set <group> [site]", permission = "", description = "Set <group> for <user>" )
	public void userSetGroup( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		PermissionManager manager = PermissionsEx.getPermissionManager();
		
		PermissionUser user = manager.getUser( this.autoCompleteUserName( args.get( "user" ) ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		String groupName = args.get( "group" );
		
		PermissionGroup[] groups;
		
		if ( groupName.contains( "," ) )
		{
			String[] groupsNames = groupName.split( "," );
			groups = new PermissionGroup[groupsNames.length];
			
			for ( int i = 0; i < groupsNames.length; i++ )
			{
				if ( sender instanceof User && !manager.has( (User) sender, "permissions.manage.membership." + groupsNames[i].toLowerCase() ) )
				{
					sender.sendMessage( ChatColor.RED + "Don't have enough permission for group " + groupsNames[i] );
					return;
				}
				
				groups[i] = manager.getGroup( this.autoCompleteGroupName( groupsNames[i] ) );
			}
			
		}
		else
		{
			groupName = this.autoCompleteGroupName( groupName );
			
			if ( groupName != null )
			{
				groups = new PermissionGroup[] { manager.getGroup( groupName ) };
				
				if ( sender instanceof User && !manager.has( (User) sender, "permissions.manage.membership." + groupName.toLowerCase() ) )
				{
					sender.sendMessage( ChatColor.RED + "Don't have enough permission for group " + groupName );
					return;
				}
				
			}
			else
			{
				sender.sendMessage( ChatColor.RED + "No groups set!" );
				return;
			}
		}
		
		if ( groups.length > 0 )
		{
			user.setGroups( groups, siteName );
			sender.sendMessage( ChatColor.WHITE + "User groups set!" );
		}
		else
		{
			sender.sendMessage( ChatColor.RED + "No groups set!" );
		}
		
		this.informUser( plugin, user.getName(), "You are now only in \"" + groupName + "\" group" );
	}
	
	@Command( name = "pex", syntax = "user <user> group remove <group> [site]", permission = "permissions.manage.membership.<group>", description = "Remove <user> from <group>" )
	public void userRemoveGroup( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		String userName = this.autoCompleteUserName( args.get( "user" ) );
		String groupName = this.autoCompleteGroupName( args.get( "group" ) );
		String siteName = this.autoCompleteSiteName( args.get( "site" ) );
		
		PermissionUser user = PermissionsEx.getPermissionManager().getUser( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ChatColor.RED + "User does not exist" );
			return;
		}
		
		user.removeGroup( groupName, siteName );
		
		sender.sendMessage( ChatColor.WHITE + "User removed from group " + groupName + "!" );
		
		this.informUser( plugin, userName, "You were removed from \"" + groupName + "\" group" );
	}
	
	@Command( name = "pex", syntax = "users cleanup <group> [threshold]", permission = "permissions.manage.users.cleanup", description = "Clean users of specified group, which last login was before threshold (in days). By default threshold is 30 days." )
	public void usersCleanup( Plugin plugin, CommandSender sender, Map<String, String> args )
	{
		long threshold = 2304000;
		
		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup( args.get( "group" ) );
		
		if ( args.containsKey( "threshold" ) )
		{
			try
			{
				threshold = Integer.parseInt( args.get( "threshold" ) ) * 86400; // 86400 - seconds in one day
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( ChatColor.RED + "Threshold should be number (in days)" );
				return;
			}
		}
		
		int removed = 0;
		
		Long deadline = ( System.currentTimeMillis() / 1000L ) - threshold;
		for ( PermissionUser user : group.getUsers() )
		{
			int lastLogin = user.getOwnOptionInteger( "last-login-time", null, 0 );
			
			if ( lastLogin > 0 && lastLogin < deadline )
			{
				user.remove();
				removed++;
			}
		}
		
		sender.sendMessage( "Cleaned " + removed + " users" );
	}
}
