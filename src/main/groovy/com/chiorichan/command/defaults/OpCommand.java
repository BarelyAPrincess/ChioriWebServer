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

public class OpCommand extends VanillaCommand
{
	public OpCommand()
	{
		super( "op" );
		this.description = "Gives the specified user operator status";
		this.usageMessage = "/op <user>";
		this.setPermission( "chiori.command.op.give" );
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
		
		Account acct = Loader.getAccountsManager().op( args[0] );
		
		if ( acct != null )
		{
			Command.broadcastCommandMessage( sender, "Opped " + args[0] );
		}
		else
		{
			Command.broadcastCommandMessage( sender, "There was a problem oping " + args[0] );
		}
		
		return true;
	}
}
