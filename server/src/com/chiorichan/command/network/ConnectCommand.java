package com.chiorichan.command.network;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.defaults.ChioriCommand;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.packet.CommandPacket;

public class ConnectCommand extends ChioriCommand
{
	public ConnectCommand()
	{
		super( "send" );
		this.description = "Attempts to connect to the remote client. Clients Only!";
		this.usageMessage = "/connect";
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !NetworkManager.isClientMode() )
		{
			sender.sendMessage( ChatColor.RED + "Severe: You can only use this command on a client connection." );
			return true;
		}
		
		NetworkManager.initTcpClient();
		
		return true;
	}
}
