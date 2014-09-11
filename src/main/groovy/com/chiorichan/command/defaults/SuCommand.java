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

public class SuCommand extends VanillaCommand
{
	public SuCommand()
	{
		super( "su" );
		this.description = "Changes the current login to specified one. OP's don't need a password.";
		this.usageMessage = "/su <user>";
		this.setPermission( "chiori.command.su" );
	}
	
	/**
	 * TODO: Make it so the user can exit the sued session. ATM they are stuck with the new login.
	 */
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		/*
		if ( !Loader.getAccountsManager().isOp( sender.getSentient().getId() ) )
		{
			sender.sendMessage( ChatColor.RED + "ATM only OP's can use su since we have not implemented a password prompt for non-ops. SAWRY :(" );
			return true;
		}
		*/
		
		if ( args.length != 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		Account acct = Loader.getAccountsManager().getAccount( args[0] );
		
		if ( acct != null )
		{
			sender.getSentient().removeHandler( sender );
			sender.sendMessage( ChatColor.AQUA + "You have been successfully switched to Account: " + acct.getId() );
			acct.putHandler( sender );
			sender.attachSentient( acct );
		}
		else
		{
			sender.sendMessage( ChatColor.RED + "There was a problem switched to Account: " + args[0] );
		}
		
		return true;
	}
}
