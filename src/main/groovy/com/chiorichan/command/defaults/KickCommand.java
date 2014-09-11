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

public class KickCommand extends VanillaCommand
{
	public KickCommand()
	{
		super( "kick" );
		this.description = "Removes the specified user from the server";
		this.usageMessage = "/kick <user> [reason ...]";
		this.setPermission( "chiori.command.kick" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length < 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		Account user = Loader.getAccountsManager().getAccount( args[0] );
		
		if ( user != null )
		{
			String reason = "Kicked by an operator.";
			
			if ( args.length > 1 )
			{
				reason = createString( args, 1 );
			}
			
			user.kick( reason );
			Command.broadcastCommandMessage( sender, "Kicked user " + user.getName() + ". With reason:\n" + reason );
		}
		else
		{
			sender.sendMessage( args[0] + " not found." );
		}
		
		return true;
	}
}
