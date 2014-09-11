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

public class ConnectCommand extends ChioriCommand
{
	public ConnectCommand()
	{
		super( "send" );
		this.description = "Attempts to connect to the remote client. Clients Only!";
		this.usageMessage = "/connect";
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{/*
	 * if ( !NetworkManager.isClientMode() )
	 * {
	 * sender.sendMessage( ChatColor.RED + "Severe: You can only use this command on a client connection." );
	 * return true;
	 * }
	 */
		
		NetworkManager.initTcpClient();
		
		return true;
	}
}
