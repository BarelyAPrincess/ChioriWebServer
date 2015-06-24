/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.commands.advanced.CommandHandler;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.tasks.Timings;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class UserCommands extends PermissionsCommand
{
	@CommandHandler( name = "pex", syntax = "entity <entity> group add <group> [ref] [lifetime]", permission = "permissions.manage.membership.<group>", description = "Add <entity> to <group>" )
	public void entityAddGroup( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "lifetime" ) )
			try
			{
				int lifetime = Timings.parseInterval( args.get( "lifetime" ) );
				entity.addGroup( PermissionManager.INSTANCE.getGroup( groupName ), lifetime, refName );
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( ConsoleColor.RED + "Group lifetime should be number!" );
				return;
			}
		else
			entity.addGroup( PermissionManager.INSTANCE.getGroup( groupName ), refName );
		
		
		sender.sendMessage( ConsoleColor.WHITE + "User added to group \"" + groupName + "\"!" );
		informEntity( entityName, "You are assigned to \"" + groupName + "\" group" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> add <permission> [ref]", permission = "permissions.manage.entitys.permissions.<entity>", description = "Add <permission> to <entity> in [ref]" )
	public void entityAddPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		entity.addPermission( args.get( "permission" ), true, refName );
		
		sender.sendMessage( ConsoleColor.WHITE + "Permission \"" + args.get( "permission" ) + "\" added!" );
		
		informEntity( entityName, "Your permissions have been changed!" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> timed add <permission> [lifetime] [ref]", permission = "permissions.manage.entitys.permissions.timed.<entity>", description = "Add timed <permissions> to <entity> for [lifetime] seconds in [ref]" )
	public void entityAddTimedPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		int lifetime = 0;
		
		if ( args.containsKey( "lifetime" ) )
			lifetime = Timings.parseInterval( args.get( "lifetime" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		String permission = args.get( "permission" );
		
		entity.addTimedPermission( permission, refName, lifetime );
		
		sender.sendMessage( ConsoleColor.WHITE + "Timed permission \"" + permission + "\" added!" );
		informEntity( entityName, "Your permissions have been changed!" );
		
		PermissionManager.getLogger().info( "User " + entityName + " get timed permission \"" + args.get( "permission" ) + "\" " + ( lifetime > 0 ? "for " + lifetime + " seconds " : " " ) + "from " + getSenderName( sender ) );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> check <permission> [ref]", permission = "permissions.manage.<entity>", description = "Checks meta for <permission>" )
	public void entityCheckPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		refName = getSafeSite( refName, entityName );
		
		String permission = entity.getMatchingExpression( args.get( "permission" ), refName );
		
		if ( permission == null )
			sender.sendMessage( "Account \"" + entityName + "\" don't such have no permission" );
		else
			sender.sendMessage( "Account \"" + entityName + "\" have \"" + permission + "\" = " + entity.explainExpression( permission ) );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> get <option> [ref]", permission = "permissions.manage.<entity>", description = "Toggle debug only for <entity>" )
	public void entityGetOption( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		refName = getSafeSite( refName, entityName );
		
		String value = entity.getOption( args.get( "option" ), refName, null );
		
		sender.sendMessage( "AccountMeta " + entityName + " @ " + refName + " option \"" + args.get( "option" ) + "\" = \"" + value + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "entitys", permission = "permissions.manage.entitys", description = "List all registered entitys (alias)", isPrimary = true )
	public void entityListAlias( InteractiveConsole sender, Map<String, String> args )
	{
		entitysList( sender, args );
	}
	
	/**
	 * User permission management
	 */
	@CommandHandler( name = "pex", syntax = "entity <entity>", permission = "permissions.manage.entitys.permissions.<entity>", description = "List entity permissions (list alias)" )
	public void entityListAliasPermissions( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( "'" + entityName + "' is a member of:" );
		printEntityInheritance( sender, entity.getParentGroups() );
		
		for ( String ref : entity.getReferences() )
		{
			if ( ref == null )
				continue;
			
			sender.sendMessage( "  @" + ref + ":" );
			printEntityInheritance( sender, entity.getParentGroups( ref ) );
		}
		
		sender.sendMessage( entityName + "'s permissions:" );
		
		sendMessage( sender, mapPermissions( refName, entity, 0 ) );
		
		sender.sendMessage( entityName + "'s options:" );
		for ( Map.Entry<String, String> option : entity.getOptions( refName ).entrySet() )
			sender.sendMessage( "  " + option.getKey() + " = \"" + option.getValue() + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity", permission = "permissions.manage.entitys", description = "List all registered entitys (alias)" )
	public void entityListAnotherAlias( InteractiveConsole sender, Map<String, String> args )
	{
		entitysList( sender, args );
	}
	
	/**
	 * User's groups management
	 */
	@CommandHandler( name = "pex", syntax = "entity <entity> group list [ref]", permission = "permissions.manage.membership.<entity>", description = "List all <entity> groups" )
	public void entityListGroup( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( "User " + args.get( "entity" ) + " @" + refName + " currently in:" );
		for ( PermissibleGroup group : entity.getParentGroups( refName ) )
			sender.sendMessage( "  " + group.getId() );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> list [ref]", permission = "permissions.manage.entitys.permissions.<entity>", description = "List entity permissions" )
	public void entityListPermissions( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		sender.sendMessage( entityName + "'s permissions:" );
		
		for ( String permission : entity.getPermissionNodes( refName ) )
			sender.sendMessage( "  " + permission );
		
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> superperms", permission = "permissions.manage.entitys.permissions.<entity>", description = "List entity actual superperms" )
	public void entityListSuperPermissions( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		
		AccountMeta meta = AccountManager.INSTANCE.getAccount( entityName );
		if ( meta == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Account not found!" );
			return;
		}
		
		sender.sendMessage( entityName + "'s superperms:" );
		
		for ( ChildPermission info : meta.getPermissibleEntity().getChildPermissions() )
			sender.sendMessage( " '" + ConsoleColor.GREEN + info.getPermission() + ConsoleColor.WHITE + "' = " + ConsoleColor.BLUE + info.getValue() );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> prefix [newprefix] [ref]", permission = "permissions.manage.entitys.prefix.<entity>", description = "Get or set <entity> prefix" )
	public void entityPrefix( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "newprefix" ) )
			entity.setPrefix( args.get( "newprefix" ), refName );
		
		sender.sendMessage( entity.getId() + "'s prefix = \"" + entity.getPrefix() + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> group remove <group> [ref]", permission = "permissions.manage.membership.<group>", description = "Remove <entity> from <group>" )
	public void entityRemoveGroup( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		entity.removeGroup( groupName, refName );
		
		sender.sendMessage( ConsoleColor.WHITE + "User removed from group " + groupName + "!" );
		
		informEntity( entityName, "You were removed from \"" + groupName + "\" group" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> remove <permission> [ref]", permission = "permissions.manage.entitys.permissions.<entity>", description = "Remove permission from <entity> in [ref]" )
	public void entityRemovePermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		String permission = autoCompletePermission( entity, args.get( "permission" ), refName );
		
		entity.detachPermission( permission, refName );
		entity.removeTimedPermission( permission, refName );
		
		sender.sendMessage( ConsoleColor.WHITE + "Permission \"" + permission + "\" removed!" );
		informEntity( entityName, "Your permissions have been changed!" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> timed remove <permission> [ref]", permission = "permissions.manage.entitys.permissions.timed.<entity>", description = "Remove timed <permission> from <entity> in [ref]" )
	public void entityRemoveTimedPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		String permission = args.get( "permission" );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		entity.removeTimedPermission( args.get( "permission" ), refName );
		
		sender.sendMessage( ConsoleColor.WHITE + "Timed permission \"" + permission + "\" removed!" );
		informEntity( entityName, "Your permissions have been changed!" );
	}
	
	@CommandHandler( name = "pex", syntax = "entitys cleanup <group> [threshold]", permission = "permissions.manage.entitys.cleanup", description = "Clean entitys of specified group, which last login was before threshold (in days). By default threshold is 30 days." )
	public void entitysCleanup( InteractiveConsole sender, Map<String, String> args )
	{
		long threshold = 2304000;
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( args.containsKey( "threshold" ) )
			try
			{
				threshold = Integer.parseInt( args.get( "threshold" ) ) * 86400; // 86400 - seconds in one day
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( ConsoleColor.RED + "Threshold should be number (in days)" );
				return;
			}
		
		int removed = 0;
		
		Long deadline = ( System.currentTimeMillis() / 1000L ) - threshold;
		for ( PermissibleEntity entity : group.getChildEntities() )
		{
			int lastLogin = entity.getOption( "last-login-time", null, 0 );
			
			if ( lastLogin > 0 && lastLogin < deadline )
			{
				entity.remove();
				removed++;
			}
		}
		
		sender.sendMessage( "Cleaned " + removed + " entitys" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> group set <group> [ref]", permission = "", description = "Set <group> for <entity>" )
	public void entitySetGroup( InteractiveConsole sender, Map<String, String> args )
	{
		PermissionManager manager = PermissionManager.INSTANCE;
		
		PermissibleEntity entity = manager.getEntity( autoCompleteAccount( args.get( "entity" ) ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		String groupName = args.get( "group" );
		
		List<PermissibleGroup> groups = Lists.newArrayList();
		
		if ( groupName.contains( "," ) )
		{
			String[] groupsNames = groupName.split( "," );
			
			for ( int i = 0; i < groupsNames.length; i++ )
			{
				if ( !sender.getPersistence().getSession().isLoginPresent() || !sender.getPersistence().getSession().getPermissibleEntity().checkPermission( "permissions.manage.membership." + groupsNames[i].toLowerCase() ).isTrue() )
				{
					sender.sendMessage( ConsoleColor.RED + "Don't have enough permission for group " + groupsNames[i] );
					return;
				}
				
				groups.add( manager.getGroup( autoCompleteGroupName( groupsNames[i] ) ) );
			}
			
		}
		else
		{
			groupName = autoCompleteGroupName( groupName );
			
			if ( groupName != null )
			{
				if ( !sender.getPersistence().getSession().isLoginPresent() || !sender.getPersistence().getSession().getPermissibleEntity().checkPermission( "permissions.manage.membership." + groupName.toLowerCase() ).isTrue() )
				{
					sender.sendMessage( ConsoleColor.RED + "Don't have enough permission for group " + groupName );
					return;
				}
				
			}
			else
			{
				sender.sendMessage( ConsoleColor.RED + "No groups set!" );
				return;
			}
		}
		
		if ( groups.size() > 0 )
		{
			entity.setParentGroups( groups, refName );
			sender.sendMessage( ConsoleColor.WHITE + "User groups set!" );
		}
		else
			sender.sendMessage( ConsoleColor.RED + "No groups set!" );
		
		informEntity( entity.getId(), "You are now only in \"" + groupName + "\" group" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> set <option> <value> [ref]", permission = "permissions.manage.entitys.permissions.<entity>", description = "Set <option> to <value> in [ref]" )
	public void entitySetOption( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		entity.setOption( args.get( "option" ), args.get( "value" ), refName );
		
		
		if ( args.containsKey( "value" ) && args.get( "value" ).isEmpty() )
			sender.sendMessage( ConsoleColor.WHITE + "Option \"" + args.get( "option" ) + "\" cleared!" );
		else
			sender.sendMessage( ConsoleColor.WHITE + "Option \"" + args.get( "option" ) + "\" set!" );
		
		informEntity( entityName, "Your permissions have been changed!" );
	}
	
	@CommandHandler( name = "pex", syntax = "entitys list", permission = "permissions.manage.entitys", description = "List all registered entitys" )
	public void entitysList( InteractiveConsole sender, Map<String, String> args )
	{
		Collection<PermissibleEntity> entitys = PermissionManager.INSTANCE.getEntities();
		
		sender.sendMessage( ConsoleColor.WHITE + "Currently registered entitys: " );
		for ( PermissibleEntity entity : entitys )
			sender.sendMessage( " " + entity.getId() + " " + ConsoleColor.DARK_GREEN + "[" + Joiner.on( ", " ).join( entity.getParentGroupNames() ) + "]" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> suffix [newsuffix] [ref]", permission = "permissions.manage.entitys.suffix.<entity>", description = "Get or set <entity> suffix" )
	public void entitySuffix( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		if ( args.containsKey( "newsuffix" ) )
			entity.setSuffix( args.get( "newsuffix" ), refName );
		
		sender.sendMessage( entity.getId() + "'s suffix = \"" + entity.getSuffix() + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> swap <permission> <targetPermission> [ref]", permission = "permissions.manage.entitys.permissions.<entity>", description = "Swap <permission> and <targetPermission> in permission list. Could be number or permission itself" )
	public void entitySwapPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		String refName = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		Permission[] permissions = entity.getPermissions( refName ).toArray( new Permission[0] );
		
		try
		{
			int sourceIndex = getPosition( autoCompletePermission( entity, args.get( "permission" ), refName, "permission" ), permissions );
			int targetIndex = getPosition( autoCompletePermission( entity, args.get( "targetPermission" ), refName, "targetPermission" ), permissions );
			
			Permission targetPermission = permissions[targetIndex];
			
			permissions[targetIndex] = permissions[sourceIndex];
			permissions[sourceIndex] = targetPermission;
			
			// entity.setPermissions( permissions, refName );
			
			sender.sendMessage( "Permissions swapped!" );
		}
		catch ( Throwable e )
		{
			sender.sendMessage( ConsoleColor.RED + "Error: " + e.getMessage() );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "entity <entity> toggle debug", permission = "permissions.manage.<entity>", description = "Toggle debug only for <entity>" )
	public void entityToggleDebug( InteractiveConsole sender, Map<String, String> args )
	{
		String entityName = autoCompleteAccount( args.get( "entity" ) );
		
		PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
		
		if ( entity == null )
		{
			sender.sendMessage( ConsoleColor.RED + "User does not exist" );
			return;
		}
		
		entity.setDebug( !entity.isDebug() );
		
		sender.sendMessage( "Debug mode for entity " + entityName + " " + ( entity.isDebug() ? "enabled" : "disabled" ) + "!" );
	}
}
