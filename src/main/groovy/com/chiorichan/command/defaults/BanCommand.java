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

public class BanCommand extends VanillaCommand
{
	public BanCommand()
	{
		super( "ban" );
		this.description = "Prevents the specified User from using this server";
		this.usageMessage = "/ban <User> [reason ...]";
		this.setPermission( "chiori.command.ban.User" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		// TODO: Ban Reason support
		Account acct = Loader.getAccountsManager().getAccount( args[0] );
		
		if ( acct != null )
		{
			Loader.getAccountsManager().banId( acct.getAccountId() );
			acct.kick( "Banned by admin." );
		}
		
		Command.broadcastCommandMessage( sender, "Banned User " + args[0] );
		return true;
	}
}
