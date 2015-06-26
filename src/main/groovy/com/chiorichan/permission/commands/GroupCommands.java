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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.commands.AdvancedCommand;
import com.chiorichan.console.commands.advanced.CommandHandler;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.References;
import com.chiorichan.tasks.Timings;
import com.google.common.base.Joiner;

public class GroupCommands extends PermissionsCommand
{
	@CommandHandler( name = "pex", syntax = "group <group> swap <permission> <targetPermission> [ref]", permission = "permissions.manage.groups.permissions.<group>", description = "Swap <permission> and <targetPermission> in permission list. Could be number or permission itself" )
	public void entitySwapPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		
		Permission[] permissions = group.getPermissions( refs ).toArray( new Permission[0] );
		
		try
		{
			int sourceIndex = getPosition( autoCompletePermission( group, args.get( "permission" ), refs, "permission" ), permissions );
			int targetIndex = getPosition( autoCompletePermission( group, args.get( "targetPermission" ), refs, "targetPermission" ), permissions );
			
			Permission targetPermission = permissions[targetIndex];
			
			permissions[targetIndex] = permissions[sourceIndex];
			permissions[sourceIndex] = targetPermission;
			
			// XXX TROUBLE ONE!
			
			// group.attachPermission( );.setPermissions( permissions, ref );
			
			sender.sendMessage( "Permissions swapped!" );
		}
		catch ( Throwable e )
		{
			sender.sendMessage( ConsoleColor.RED + "Error: " + e.getMessage() );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> parents add <parents> [ref]", permission = "permissions.manage.groups.inheritance.<group>", description = "Set parent(s) for <group> (single or comma-separated list)" )
	public void groupAddParents( AdvancedCommand command, InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.get( "parents" ) != null )
		{
			String[] parents = args.get( "parents" ).split( "," );
			Collection<PermissibleGroup> groups = group.getGroups( refs );
			
			for ( String parent : parents )
			{
				PermissibleGroup parentGroup = PermissionManager.INSTANCE.getGroup( autoCompleteGroupName( parent ) );
				
				if ( parentGroup != null && !groups.contains( parentGroup ) )
					groups.add( parentGroup );
			}
			
			group.setGroups( groups, refs );
			
			sender.sendMessage( ConsoleColor.WHITE + "Group " + group.getId() + " inheritance updated!" );
			
			group.save();
		}
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> add <permission> [ref]", permission = "permissions.manage.groups.permissions.<group>", description = "Add <permission> to <group> in [ref]" )
	public void groupAddPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		group.addPermission( args.get( "permission" ), true, refs );
		
		sender.sendMessage( ConsoleColor.WHITE + "Permission \"" + args.get( "permission" ) + "\" added to " + group.getId() + " !" );
		
		informGroup( group, "Your permissions have been changed" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> timed add <permission> [lifetime] [ref]", permission = "permissions.manage.groups.permissions.timed.<group>", description = "Add timed <permission> to <group> with [lifetime] in [ref]" )
	public void groupAddTimedPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		int lifetime = 0;
		
		if ( args.containsKey( "lifetime" ) )
			lifetime = Timings.parseInterval( args.get( "lifetime" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group does not exist" );
			return;
		}
		
		group.addTimedPermission( args.get( "permission" ), true, refs, lifetime );
		
		sender.sendMessage( ConsoleColor.WHITE + "Timed permission added!" );
		informGroup( group, "Your permissions have been changed!" );
		
		PermissionManager.getLogger().info( "Group " + groupName + " get timed permission \"" + args.get( "permission" ) + "\" " + ( lifetime > 0 ? "for " + lifetime + " seconds " : " " ) + "from " + getSenderName( sender ) );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> create [parents]", permission = "permissions.manage.groups.create.<group>", description = "Create <group> and/or set [parents]" )
	public void groupCreate( InteractiveConsole sender, Map<String, String> args )
	{
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( !group.isCommitted() )
		{
			sender.sendMessage( ConsoleColor.RED + "Group " + args.get( "group" ) + " already exists" );
			return;
		}
		
		if ( args.get( "parents" ) != null )
		{
			String[] parents = args.get( "parents" ).split( "," );
			List<PermissibleGroup> groups = new LinkedList<PermissibleGroup>();
			
			for ( String parent : parents )
				groups.add( PermissionManager.INSTANCE.getGroup( parent ) );
			
			group.setGroups( groups, References.format() );
		}
		
		sender.sendMessage( ConsoleColor.WHITE + "Group " + group.getId() + " created!" );
		
		group.save();
	}
	
	@CommandHandler( name = "pex", syntax = "default group [ref]", permission = "permissions.manage.groups.inheritance", description = "Print default group for specified ref" )
	public void groupDefaultCheck( InteractiveConsole sender, Map<String, String> args )
	{
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		
		PermissibleGroup defaultGroup = PermissionManager.INSTANCE.getDefaultGroup( refs );
		sender.sendMessage( "Default group in " + refs + " ref is " + defaultGroup.getId() + " group" );
	}
	
	@CommandHandler( name = "pex", syntax = "set default group <group> [ref]", permission = "permissions.manage.groups.inheritance", description = "Set default group for specified ref" )
	public void groupDefaultSet( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null || group.isCommitted() )
		{
			sender.sendMessage( ConsoleColor.RED + "Specified group doesn't exist" );
			return;
		}
		
		PermissionManager.INSTANCE.setDefaultGroup( group, refs );
		sender.sendMessage( "New default group in " + refs + " ref is " + group.getId() + " group" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> delete", permission = "permissions.manage.groups.remove.<group>", description = "Remove <group>" )
	public void groupDelete( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		sender.sendMessage( ConsoleColor.WHITE + "Group " + group.getId() + " removed!" );
		
		group.remove();
		PermissionManager.INSTANCE.resetGroup( group.getId() );
		group = null;
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> entity add <entity> [ref]", permission = "permissions.manage.membership.<group>", description = "Add <entity> (single or comma-separated list) to <group>" )
	public void groupEntitysAdd( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		String[] entities;
		
		if ( !args.get( "entity" ).contains( "," ) )
			entities = new String[] {args.get( "entity" )};
		else
			entities = args.get( "entity" ).split( "," );
		
		for ( String entityName : entities )
		{
			entityName = autoCompleteAccount( entityName );
			PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
			
			if ( entity == null )
			{
				sender.sendMessage( ConsoleColor.RED + "Entity does not exist" );
				return;
			}
			
			entity.addGroup( PermissionManager.INSTANCE.getGroup( groupName ), refs );
			
			sender.sendMessage( ConsoleColor.WHITE + "Entity " + entity.getId() + " added to " + groupName + " !" );
			informEntity( entityName, "You are assigned to \"" + groupName + "\" group" );
		}
	}
	
	/**
	 * Group entities management
	 */
	@CommandHandler( name = "pex", syntax = "group <group> entities", permission = "permissions.manage.membership.<group>", description = "List all entities in <group>" )
	public void groupEntitysList( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		
		Collection<PermissibleGroup> entities = PermissionManager.INSTANCE.getGroups( groupName );
		
		if ( entities == null || entities.size() == 0 )
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist or empty" );
		
		sender.sendMessage( "Group " + groupName + " entities:" );
		
		for ( PermissibleEntity entity : entities )
			sender.sendMessage( "   " + entity.getId() );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> entity remove <entity> [ref]", permission = "permissions.manage.membership.<group>", description = "Add <entity> (single or comma-separated list) to <group>" )
	public void groupEntitysRemove( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		String[] entities;
		
		if ( !args.get( "entity" ).contains( "," ) )
			entities = new String[] {args.get( "entity" )};
		else
			entities = args.get( "entity" ).split( "," );
		
		for ( String entityName : entities )
		{
			entityName = autoCompleteAccount( entityName );
			PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( entityName );
			
			if ( entity == null )
			{
				sender.sendMessage( ConsoleColor.RED + "Entity does not exist" );
				return;
			}
			
			entity.removeGroup( groupName, refs );
			
			sender.sendMessage( ConsoleColor.WHITE + "Entity " + entity.getId() + " removed from " + args.get( "group" ) + " !" );
			informEntity( entityName, "You were removed from \"" + groupName + "\" group" );
		}
	}
	
	/**
	 * Group permissions
	 */
	@CommandHandler( name = "pex", syntax = "group <group>", permission = "permissions.manage.groups.permissions.<group>", description = "List all <group> permissions (alias)" )
	public void groupListAliasPermissions( InteractiveConsole sender, Map<String, String> args )
	{
		groupListPermissions( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> parents list [ref]", permission = "permissions.manage.groups.inheritance.<group>", description = "List parents for <group>" )
	public void groupListParents( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( group.getGroups( refs ).size() == 0 )
		{
			sender.sendMessage( ConsoleColor.RED + "Group " + group.getId() + " doesn't have parents" );
			return;
		}
		
		sender.sendMessage( "Group " + group.getId() + " parents:" );
		
		for ( PermissibleGroup parent : group.getGroups( refs ) )
			sender.sendMessage( "  " + parent.getId() );
		
	}
	
	/**
	 * Group inheritance
	 */
	@CommandHandler( name = "pex", syntax = "group <group> parents [ref]", permission = "permissions.manage.groups.inheritance.<group>", description = "List parents for <group> (alias)" )
	public void groupListParentsAlias( InteractiveConsole sender, Map<String, String> args )
	{
		groupListParents( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> list [ref]", permission = "permissions.manage.groups.permissions.<group>", description = "List all <group> permissions in [ref]" )
	public void groupListPermissions( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		sender.sendMessage( "'" + groupName + "' inherits the following groups:" );
		printEntityInheritance( sender, group.getGroups( References.format( "" ) ) );
		
		References ref = group.getGroupReferences();
		sender.sendMessage( "  @" + ref + ":" );
		printEntityInheritance( sender, group.getGroups( ref ) );
		
		sender.sendMessage( "Group " + group.getId() + "'s permissions:" );
		sendMessage( sender, mapPermissions( refs, group, 0 ) );
		
		sender.sendMessage( "Group " + group.getId() + "'s Options: " );
		for ( Map.Entry<String, String> option : group.getOptions().entrySet() )
			sender.sendMessage( "  " + option.getKey() + " = \"" + option.getValue() + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> prefix [newprefix] [ref]", permission = "permissions.manage.groups.prefix.<group>", description = "Get or set <group> prefix." )
	public void groupPrefix( InteractiveConsole sender, Map<String, String> args )
	{
		// String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.containsKey( "newprefix" ) )
			group.setPrefix( args.get( "newprefix" ), refs );
		
		sender.sendMessage( group.getId() + "'s prefix = \"" + group.getPrefix( refs ) + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> weight [weight]", permission = "permissions.manage.groups.weight.<group>", description = "Print or set group weight" )
	public void groupPrintSetWeight( InteractiveConsole sender, Map<String, String> args )
	{
		// String groupName = autoCompleteGroupName( args.get( "group" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.containsKey( "weight" ) )
			try
			{
				group.setWeight( Integer.parseInt( args.get( "weight" ) ) );
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( "Error! Weight should be integer value." );
				return;
			}
		
		sender.sendMessage( "Group " + group.getId() + " have " + group.getWeight() + " calories." );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> parents remove <parents> [ref]", permission = "permissions.manage.groups.inheritance.<group>", description = "Set parent(s) for <group> (single or comma-separated list)" )
	public void groupRemoveParents( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.get( "parents" ) != null )
		{
			String[] parents = args.get( "parents" ).split( "," );
			Collection<PermissibleGroup> groups = group.getGroups( refs );
			
			for ( String parent : parents )
			{
				PermissibleGroup parentGroup = PermissionManager.INSTANCE.getGroup( autoCompleteGroupName( parent ) );
				
				groups.remove( parentGroup );
			}
			
			group.setGroups( groups, refs );
			
			sender.sendMessage( ConsoleColor.WHITE + "Group " + group.getId() + " inheritance updated!" );
			
			group.save();
		}
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> remove <permission> [ref]", permission = "permissions.manage.groups.permissions.<group>", description = "Remove <permission> from <group> in [ref]" )
	public void groupRemovePermission( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		String permission = autoCompletePermission( group, args.get( "permission" ), refs );
		
		group.removePermission( permission, refs );
		group.removeTimedPermission( permission, refs );
		
		sender.sendMessage( ConsoleColor.WHITE + "Permission \"" + permission + "\" removed from " + group.getId() + " !" );
		
		informGroup( group, "Your permissions have been changed" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> timed remove <permission> [ref]", permission = "permissions.manage.groups.permissions.timed.<group>", description = "Remove timed <permissions> for <group> in [ref]" )
	public void groupRemoveTimedPermission( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group does not exist" );
			return;
		}
		
		group.removeTimedPermission( args.get( "permission" ), refs );
		
		sender.sendMessage( ConsoleColor.WHITE + "Timed permission \"" + args.get( "permission" ) + "\" removed!" );
		informGroup( group, "Your permissions have been changed!" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> set <option> <value> [ref]", permission = "permissions.manage.groups.permissions.<group>", description = "Set <option> <value> for <group> in [ref]" )
	public void groupSetOption( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		group.setOption( args.get( "option" ), args.get( "value" ), refs );
		
		if ( args.containsKey( "value" ) && args.get( "value" ).isEmpty() )
			sender.sendMessage( ConsoleColor.WHITE + "Option \"" + args.get( "option" ) + "\" cleared!" );
		else
			sender.sendMessage( ConsoleColor.WHITE + "Option \"" + args.get( "option" ) + "\" set!" );
		
		informGroup( group, "Your permissions has been changed" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> parents set <parents> [ref]", permission = "permissions.manage.groups.inheritance.<group>", description = "Set parent(s) for <group> (single or comma-separated list)" )
	public void groupSetParents( InteractiveConsole sender, Map<String, String> args )
	{
		String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.get( "parents" ) != null )
		{
			String[] parents = args.get( "parents" ).split( "," );
			List<PermissibleGroup> groups = new LinkedList<PermissibleGroup>();
			
			for ( String parent : parents )
			{
				PermissibleGroup parentGroup = PermissionManager.INSTANCE.getGroup( autoCompleteGroupName( parent ) );
				
				if ( parentGroup != null && !groups.contains( parentGroup ) )
					groups.add( parentGroup );
			}
			
			group.setGroups( groups, refs );
			
			sender.sendMessage( ConsoleColor.WHITE + "Group " + group.getId() + " inheritance updated!" );
			
			group.save();
		}
	}
	
	@CommandHandler( name = "pex", syntax = "groups list [ref]", permission = "permissions.manage.groups.list", description = "List all registered groups" )
	public void groupsList( InteractiveConsole sender, Map<String, String> args )
	{
		Collection<PermissibleGroup> groups = PermissionManager.INSTANCE.getGroups();
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		sender.sendMessage( ConsoleColor.WHITE + "Registered groups: " );
		for ( PermissibleGroup group : groups )
		{
			String rank = "";
			if ( group.isRanked() )
				rank = " (rank: " + group.getRank() + "@";// TODO + group.getRankLadder() + ") ";
				
			sender.sendMessage( String.format( "  %s %s %s %s[%s]", group.getId(), " #" + group.getWeight(), rank, ConsoleColor.DARK_GREEN, Joiner.on( ", " ).join( group.getParentGroupsNames( refs ) ) ) );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "groups", permission = "permissions.manage.groups.list", description = "List all registered groups (alias)" )
	public void groupsListAlias( InteractiveConsole sender, Map<String, String> args )
	{
		groupsList( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "group", permission = "permissions.manage.groups.list", description = "List all registered groups (alias)" )
	public void groupsListAnotherAlias( InteractiveConsole sender, Map<String, String> args )
	{
		groupsList( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> suffix [newsuffix] [ref]", permission = "permissions.manage.groups.suffix.<group>", description = "Get or set <group> suffix" )
	public void groupSuffix( InteractiveConsole sender, Map<String, String> args )
	{
		// String groupName = autoCompleteGroupName( args.get( "group" ) );
		References refs = autoCompleteRef( args.get( "ref" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		if ( args.containsKey( "newsuffix" ) )
			group.setSuffix( args.get( "newsuffix" ), refs );
		
		sender.sendMessage( group.getId() + "'s suffix is = \"" + group.getSuffix( refs ) + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> toggle debug", permission = "permissions.manage.groups.debug.<group>", description = "Toggle debug mode for group" )
	public void groupToggleDebug( InteractiveConsole sender, Map<String, String> args )
	{
		// String groupName = autoCompleteGroupName( args.get( "group" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "group" ) );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group doesn't exist" );
			return;
		}
		
		group.setDebug( !group.isDebug() );
		
		sender.sendMessage( "Debug mode for group " + group.getId() + " have been " + ( group.isDebug() ? "enabled" : "disabled" ) + "!" );
	}
}
