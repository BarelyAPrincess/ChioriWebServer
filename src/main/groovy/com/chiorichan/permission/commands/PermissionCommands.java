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
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.account.AccountAttachment;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionType;
import com.chiorichan.terminal.commands.advanced.CommandHandler;
import com.chiorichan.util.PermissionNamespace;

/**
 * Manages permissions
 */
public class PermissionCommands extends PermissionBaseCommand
{
	@CommandHandler( name = "pex", syntax = "perm create <node> [type]", permission = "permissions.manage.permissions", description = "Create permission" )
	public void permsCreate( AccountAttachment sender, Map<String, String> args )
	{
		if ( !args.containsKey( "node" ) || args.get( "node" ).isEmpty() )
		{
			sender.sendMessage( ConsoleColor.RED + "You must specify a permission node!" );
			return;
		}
		
		PermissionType type = args.containsKey( "type" ) && !args.get( "type" ).isEmpty() ? PermissionType.valueOf( args.get( "type" ) ) : PermissionType.DEFAULT;
		
		if ( type == null )
		{
			sender.sendMessage( ConsoleColor.RED + "We could not find a permission type that matches '" + args.get( "type" ) + "'!" );
			return;
		}
		
		PermissionNamespace ns = new PermissionNamespace( args.get( "node" ) );
		
		ns.createPermission( type );
		
		sender.sendMessage( ConsoleColor.AQUA + "Good news everybody, we successfully created permission node '" + ns.getNamespace() + "' with type '" + type.name() + "'!" );
	}
	
	@CommandHandler( name = "pex", syntax = "perm list [parent]", permission = "permissions.manage.permissions", description = "List all permissions" )
	public void permsList( AccountAttachment sender, Map<String, String> args )
	{
		if ( args.containsKey( "parent" ) )
		{
			Permission root = PermissionManager.INSTANCE.getNode( args.get( "parent" ) );
			
			if ( root == null )
				sender.sendMessage( ConsoleColor.RED + "There was no such permission '" + args.get( "parent" ) + "'!" );
			else
			{
				sender.sendMessage( ConsoleColor.WHITE + "Permissions stack dump for '" + args.get( "parent" ) + "':" );
				root.debugPermissionStack( 0 );
			}
		}
		else
		{
			Collection<Permission> perms = PermissionManager.INSTANCE.getRootNodes();
			
			sender.sendMessage( ConsoleColor.WHITE + "Permissions stack dump:" );
			for ( Permission root : perms )
				root.debugPermissionStack( 0 );
		}
	}
}
