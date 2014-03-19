package com.chiorichan.command.network;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.defaults.ChioriCommand;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.packet.CommandPacket;

public class SendCommand extends ChioriCommand
{
	public SendCommand()
	{
		super( "send" );
		this.description = "Sends a command to remote virtual console. Clients Only!";
		this.usageMessage = "/send (command) [params]";
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !NetworkManager.isClientMode() )
		{
			sender.sendMessage( ChatColor.RED + "Severe: You can only use this command on a client connection." );
			return true;
		}
		
		if ( args.length < 1 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		StringBuilder params = new StringBuilder();
		
		for ( int i = 1; i < args.length; i++ )
		{
			params.append( " " );
			params.append( args[i] );
		}
		
		NetworkManager.sendTCP( new CommandPacket( args[0], params.toString() ) );
		
		return true;
	}
}
