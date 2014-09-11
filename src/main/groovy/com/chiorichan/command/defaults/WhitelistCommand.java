/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class WhitelistCommand extends VanillaCommand
{
	// private static final List<String> WHITELIST_SUBCOMMANDS = ImmutableList.of( "add", "remove", "on", "off", "list", "reload" );
	
	public WhitelistCommand()
	{
		super( "whitelist" );
		this.description = "Manages the list of Users allowed to use this server";
		this.usageMessage = "/whitelist (add|remove) <User>\n/whitelist (on|off|list|reload)";
		this.setPermission( "chiori.command.whitelist.reload;chiori.command.whitelist.enable;chiori.command.whitelist.disable;chiori.command.whitelist.list;chiori.command.whitelist.add;chiori.command.whitelist.remove" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length == 1 )
		{
			if ( args[0].equalsIgnoreCase( "reload" ) )
			{
				if ( badPerm( sender, "reload" ) )
					return true;
				
				Loader.getAccountsManager().reloadWhitelist();
				Command.broadcastCommandMessage( sender, "Reloaded white-list from file" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "on" ) )
			{
				if ( badPerm( sender, "enable" ) )
					return true;
				
				Loader.getAccountsManager().setWhitelist( true );
				Command.broadcastCommandMessage( sender, "Turned on white-listing" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "off" ) )
			{
				if ( badPerm( sender, "disable" ) )
					return true;
				
				Loader.getAccountsManager().setWhitelist( false );
				Command.broadcastCommandMessage( sender, "Turned off white-listing" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "list" ) )
			{
				if ( badPerm( sender, "list" ) )
					return true;
				
				StringBuilder result = new StringBuilder();
				
				for ( Account User : Loader.getAccountsManager().getWhitelisted() )
				{
					if ( result.length() > 0 )
					{
						result.append( ", " );
					}
					
					result.append( User.getName() );
				}
				
				sender.sendMessage( "White-listed Users: " + result.toString() );
				return true;
			}
		}
		else if ( args.length == 2 )
		{
			if ( args[0].equalsIgnoreCase( "add" ) )
			{
				if ( badPerm( sender, "add" ) )
					return true;
				
				Loader.getAccountsManager().addWhitelist( args[1] );
				
				Command.broadcastCommandMessage( sender, "Added " + args[1] + " to white-list" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "remove" ) )
			{
				if ( badPerm( sender, "remove" ) )
					return true;
				
				Loader.getAccountsManager().removeWhitelist( args[1] );
				
				Command.broadcastCommandMessage( sender, "Removed " + args[1] + " from white-list" );
				return true;
			}
		}
		
		sender.sendMessage( ChatColor.RED + "Correct command usage:\n" + usageMessage );
		return false;
	}
	
	private boolean badPerm( SentientHandler sender, String perm )
	{
		if ( !sender.getSentient().hasPermission( "chiori.command.whitelist." + perm ) )
		{
			sender.sendMessage( ChatColor.RED + "You do not have permission to perform this action." );
			return true;
		}
		
		return false;
	}
}
