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

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class ReloadCommand extends ChioriCommand
{
	public ReloadCommand(String name)
	{
		super( name );
		this.description = "Reloads the server configuration and plugins";
		this.usageMessage = "/reload";
		this.setPermission( "chiori.command.reload" );
		this.setAliases( Arrays.asList( "rl" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Loader.getInstance().reload();
		Command.broadcastCommandMessage( sender, ChatColor.GREEN + "Reload complete." );
		
		return true;
	}
}
