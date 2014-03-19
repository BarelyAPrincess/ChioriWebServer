package com.chiorichan.command.defaults;

import com.chiorichan.Console;
import com.chiorichan.command.CommandSender;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.net.packet.PingPacket;
import com.chiorichan.util.StringUtil;

public class PingCommand extends VanillaCommand
{
	public PingCommand()
	{
		super( "ping" );
		this.description = "Responds to sender with PONG tick";
		this.usageMessage = "/ping";
		this.setPermission( "chiori.command.ping" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( NetworkManager.isClientMode() )
			
			NetworkManager.sendTCP( new PingPacket( StringUtil.md5( "PingCommand/" + System.currentTimeMillis() ) ) );
		else
			sender.sendMessage( "Pong " + Console.currentTick );
		
		return true;
	}
}
