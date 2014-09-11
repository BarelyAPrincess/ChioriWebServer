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
import com.chiorichan.account.bases.SentientHandler;

public class HelpCommand extends VanillaCommand
{
	public HelpCommand()
	{
		super( "help" );
		this.description = "Prints out the help map";
		this.usageMessage = "help";
		this.setPermission( "chiori.command.help" );
		this.setAliases( Arrays.asList( "?" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		sender.sendMessage( ChatColor.RED + "Not Implemented!" );
		
		return true;
	}
}
