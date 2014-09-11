/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command.network;

import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.defaults.ChioriCommand;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.packet.DataPacket;

public class LoginCommand extends ChioriCommand
{
	public LoginCommand()
	{
		super( "login" );
		this.description = "Attempts to start an interactive console experience with the remote server. Clients Only!";
		this.usageMessage = "/login";
	}
	
	@Override
	public boolean execute( SentientHandler sender, String commandLabel, String[] args )
	{/*
		if ( !NetworkManager.isClientMode() )
		{
			sender.sendMessage( ChatColor.RED + "Severe: You can only use this command on a client connection." );
			return true;
		}*/
		
		NetworkManager.sendTCP( new DataPacket( "BeginConsole", null ) );
		
		return true;
	}
}
