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

import java.util.List;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;

public class ListCommand extends VanillaCommand
{
	public ListCommand()
	{
		super( "list" );
		this.description = "Lists all online users";
		this.usageMessage = "/list";
		this.setPermission( "chiori.command.list" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		StringBuilder online = new StringBuilder();
		
		List<Account> users = Loader.getAccountsManager().getOnlineAccounts();
		
		for ( Account user : users )
		{
			// If a user is hidden from the sender don't show them in the list
			if ( sender instanceof Account && !( (Account) sender ).canSee( user ) )
				continue;
			
			if ( online.length() > 0 )
			{
				online.append( ", " );
			}
			
			online.append( user.getDisplayName() );
		}
		
		sender.sendMessage( "There are " + users.size() + "/" + Loader.getAccountsManager().getMaxAccounts() + " users online:\n" + online.toString() );
		
		return true;
	}
}
