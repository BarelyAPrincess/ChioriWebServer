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

public class DeopCommand extends VanillaCommand
{
	public DeopCommand()
	{
		super( "deop" );
		this.description = "Takes the specified User's operator status";
		this.usageMessage = "/deop <User>";
		this.setPermission( "chiori.command.op.take" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length != 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		Account acct = Loader.getAccountsManager().deop( args[0] );
		
		acct.sendMessage( ChatColor.YELLOW + "You are no longer op!" );
		
		Command.broadcastCommandMessage( sender, "De-opped " + args[0] );
		return true;
	}
}
