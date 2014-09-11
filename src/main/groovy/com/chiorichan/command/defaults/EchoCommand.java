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
import com.chiorichan.account.bases.SentientHandler;

public class EchoCommand extends VanillaCommand
{
	public EchoCommand()
	{
		super( "echo" );
		this.description = "Repeats the given message to the user";
		this.usageMessage = "/echo <message>";
		this.setPermission( "chiori.command.echo" );
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
		
		StringBuilder message = new StringBuilder();
		if ( args.length > 0 )
		{
			message.append( args[0] );
			for ( int i = 1; i < args.length; i++ )
			{
				message.append( " " );
				message.append( args[i] );
			}
		}
		
		sender.sendMessage( message.toString() );
		
		return true;
	}
}
