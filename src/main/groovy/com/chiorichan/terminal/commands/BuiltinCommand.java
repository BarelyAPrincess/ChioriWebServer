/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal.commands;

import java.util.Arrays;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountAttachment;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.Kickable;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.event.server.KickEvent;
import com.chiorichan.messaging.MessageBuilder;
import com.chiorichan.messaging.MessageDispatch;
import com.chiorichan.messaging.MessageException;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissionDefault;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.References;
import com.chiorichan.terminal.Command;
import com.chiorichan.terminal.CommandDispatch;
import com.chiorichan.terminal.TerminalEntity;
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
	
	BuiltinCommand( String name, String permission, String description, String usage )
	{
		super( name, permission );
		setDescription( description );
		setUsage( usage );
	}
	
	public static void registerBuiltinCommands()
	{
		CommandDispatch.registerCommand( new BuiltinCommand( "version" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( ConsoleColor.AQUA + Versioning.getProduct() + " is running version " + Versioning.getVersion() + ( ( Versioning.getBuildNumber().equals( "0" ) ) ? " (dev)" : " (build #" + Versioning.getBuildNumber() + ")" ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "uptime" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( "Server Uptime: " + Loader.getUptime() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "echo" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( Joiner.on( " " ).join( args ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "help" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( ConsoleColor.YELLOW + "We're sorry, help has not been implemented as of yet, try again in a later version." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "whoami" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( sender.getId() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "color" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				String color = "" + ( ( args.length < 1 ) ? !StringFunc.isTrue( sender.getVariable( "color", "true" ) ) : StringFunc.isTrue( args[0] ) );
				sender.setVariable( "color", color );
				sender.sendMessage( ConsoleColor.AQUA + "Console color has been " + ( ( StringFunc.isTrue( color ) ) ? "enabled" : "disabled" ) + "." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "kick" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				try
				{
					if ( !testPermission( sender ) )
						return true;
					if ( args.length < 1 || args[0].length() == 0 )
					{
						sender.sendMessage( ConsoleColor.RED + "Usage: " + usageMessage );
						return false;
					}
					
					Account user = AccountManager.INSTANCE.getAccountPartial( args[0] );
					
					if ( user != null )
					{
						String reason = "Kicked by an operator.";
						
						if ( args.length > 1 )
							reason = StringFunc.join( args, 1 );
						
						if ( user instanceof Kickable )
						{
							KickEvent.kick( sender.meta(), ( Kickable ) user ).setReason( reason ).fire();
							
							try
							{
								MessageDispatch.sendMessage( MessageBuilder.msg( String.format( "Kicked account %s with reason:%s", user.getDisplayName(), reason ) ).from( sender ) );
							}
							catch ( MessageException e )
							{
								e.printStackTrace();
							}
						}
						else
							sender.sendMessage( String.format( "We found %s but it was not an instance of Kickable", args[0] ) );
					}
					else
						sender.sendMessage( String.format( "%s not found.", args[0] ) );
				}
				catch ( AccountException e )
				{
					e.printStackTrace();
					sender.sendMessage( "We had a problem executing the kick command" );
				}
				
				return true;
			}
		}.setDescription( "Kicks the specified user from the server." ) );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "colors" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( "&l&d&oTo use any of these just type & (amperstamp) folowed by the color/format code." );
				sender.sendMessage( "" );
				sender.sendMessage( "&00 - Black &11 - Dark Blue &22 - Dark Green &33 - Dark Aqua &44 - Dark Red &55 - Dark Purple &66 - Gold &77 - Gray &88 - Dark Gray &99 - Indigo" );
				sender.sendMessage( "&aa - Green &bb - Aqua &cc - Red &dd - Pink &ee - Yellow &ff - White &r&mm - Strike Through&r &nn - Underlined&r &ll - Bold&r &kk - Random&r &oo - Italic" );
				sender.sendMessage( "" );
				sender.sendMessage( "&l&4&oJust keep in mind that some of these color/format codes are not supported by all terminals. If your have any problems check your terminal type." );
				return true;
			}
		}.setDescription( "Prints a list of colors that can be used in this console/chat." ) );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "restart" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				if ( sender.getEntity().isOp() )
					Loader.restart( "The server is restarting by request of acct " + sender.getId() );
				else
					sender.sendMessage( ConsoleColor.RED + "Only server operators can request the server to restart." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "stop" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				if ( sender.getEntity().isOp() )
					Loader.stop( "The server is shutting down by request of acct " + sender.getId() );
				else
					sender.sendMessage( ConsoleColor.RED + "Only server operators can request the server to stop." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "deop" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				if ( sender.getEntity().isOp() )
				{
					if ( args.length < 1 )
						sender.sendMessage( ConsoleColor.RED + "You must specify which account you wish to deop." );
					else
					{
						PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( args[0], false );
						if ( entity == null )
							sender.sendMessage( ConsoleColor.RED + "We could not find an entity by that id." );
						entity.removePermission( PermissionDefault.OP.getNode(), References.format() );
						sender.sendMessage( ConsoleColor.AQUA + "We successfully deop'ed entity " + entity.getId() );
					}
				}
				else
					sender.sendMessage( ConsoleColor.RED + "Only server operators can demote entities from server operator." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "op" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				if ( sender.getEntity().isOp() )
				{
					if ( args.length < 1 )
						sender.sendMessage( ConsoleColor.RED + "You must specify which account you wish to op." );
					else
					{
						PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( args[0], false );
						if ( entity == null )
							sender.sendMessage( ConsoleColor.RED + "We could not find an entity by that id." );
						entity.addPermission( PermissionDefault.OP.getNode(), true, null );
						sender.sendMessage( ConsoleColor.AQUA + "We successfully op'ed entity " + entity.getId() );
					}
				}
				else
					sender.sendMessage( ConsoleColor.RED + "Only server operators can promote entities to server operator." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "aboutme" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				for ( String s : sender.meta().getKeys() )
					sender.sendMessage( s + " => " + sender.meta().getString( s ) );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "save" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( ConsoleColor.AQUA + "Forcing Save..." );
				AccountManager.INSTANCE.save();
				PermissionManager.INSTANCE.saveData();
				Loader.saveConfig();
				sender.sendMessage( ConsoleColor.AQUA + "Complete." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "logout" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				sender.sendMessage( ConsoleColor.AQUA + "Thank you for visiting, please come back again." );
				sender.getPermissible().logout();
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "exit" )
		{
			@Override
			public boolean execute( AccountAttachment sender, String command, String[] args )
			{
				if ( sender.getPermissible() instanceof TerminalEntity )
				{
					( ( TerminalEntity ) sender.getPermissible() ).getHandler().disconnect();
					sender.sendMessage( ConsoleColor.AQUA + "Good bye!" );
				}
				else
					sender.sendMessage( ConsoleColor.RED + "We're sorry, this connection can not be disconnected." );
				
				return true;
			}
		}.setAliases( Arrays.asList( new String[] {"quit", "end", "leave"} ) ) );
		
		CommandDispatch.registerCommand( new LoginCommand() );
	}
}
