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
