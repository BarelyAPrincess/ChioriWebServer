/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.console.commands;

import java.util.Arrays;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.console.Command;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissionDefault;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.References;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;

/**
 * Used for builtin server commands
 */
public abstract class BuiltinCommand extends Command
{
	BuiltinCommand( String name )
	{
		super( name );
	}
	
	BuiltinCommand( String name, String permission )
	{
		super( name, permission );
	}
	
	public static void registerBuiltinCommands()
	{
		CommandDispatch.registerCommand( new BuiltinCommand( "version" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( ConsoleColor.AQUA + Versioning.getProduct() + " is running version " + Versioning.getVersion() + ( ( Versioning.getBuildNumber().equals( "0" ) ) ? " (dev)" : " (build #" + Versioning.getBuildNumber() + ")" ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "uptime" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( "Server Uptime: " + Loader.getUptime() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "echo" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( Joiner.on( " " ).join( args ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "help" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( ConsoleColor.YELLOW + "We're sorry, help has not been implemented as of yet, try again in a later version." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "whoami" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( handler.getPersistence().getSession().account().getAcctId() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "color" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				String color = "" + ( ( args.length < 1 ) ? !StringFunc.isTrue( handler.getMetadata( "color", "true" ) ) : StringFunc.isTrue( args[0] ) );
				handler.setMetadata( "color", color );
				handler.sendMessage( ConsoleColor.AQUA + "Console color has been " + ( ( StringFunc.isTrue( color ) ) ? "enabled" : "disabled" ) + "." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "stop" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				if ( handler.getPersistence().getSession().isOp() )
					Loader.stop( "The server is shutting down as requested by user " + handler.getPersistence().getSession().getAcctId() );
				else
					handler.sendMessage( ConsoleColor.RED + "Only server operators can request the server to stop." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "deop" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				if ( handler.getPersistence().getSession().isOp() )
				{
					if ( args.length < 1 )
						handler.sendMessage( ConsoleColor.RED + "You must specify which account you wish to deop." );
					else
					{
						PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( args[0], false );
						if ( entity == null )
							handler.sendMessage( ConsoleColor.RED + "We could not find an entity by that id." );
						entity.removePermission( PermissionDefault.OP.getNode(), References.format() );
						handler.sendMessage( ConsoleColor.AQUA + "We successfully deop'ed entity " + entity.getId() );
					}
				}
				else
					handler.sendMessage( ConsoleColor.RED + "Only server operators can demote entities from server operator." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "op" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				if ( handler.getPersistence().getSession().isOp() )
				{
					if ( args.length < 1 )
						handler.sendMessage( ConsoleColor.RED + "You must specify which account you wish to op." );
					else
					{
						PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( args[0], false );
						if ( entity == null )
							handler.sendMessage( ConsoleColor.RED + "We could not find an entity by that id." );
						entity.addPermission( PermissionDefault.OP.getNode(), true, null );
						handler.sendMessage( ConsoleColor.AQUA + "We successfully op'ed entity " + entity.getId() );
					}
				}
				else
					handler.sendMessage( ConsoleColor.RED + "Only server operators can promote entities to server operator." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "aboutme" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				AccountInstance acct = handler.getPersistence().getSession().account();
				
				for ( String s : acct.metadata().getKeys() )
					handler.sendMessage( s + " => " + acct.metadata().getString( s ) );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "exit" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( ConsoleColor.AQUA + "Thank you for visiting, please come back again." );
				handler.getPersistence().finish();
				return true;
			}
		}.setAliases( Arrays.asList( new String[] {"quit", "end", "leave", "logout"} ) ) );
		
		CommandDispatch.registerCommand( new LoginCommand() );
	}
}
